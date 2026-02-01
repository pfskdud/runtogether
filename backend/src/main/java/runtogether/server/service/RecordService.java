package runtogether.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runtogether.server.domain.*;
import runtogether.server.dto.LapDto;
import runtogether.server.dto.RecordDto;
import runtogether.server.dto.ReplayDto;
import runtogether.server.repository.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final RunRecordRepository recordRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final RunningGroupRepository runningGroupRepository;
    private final RoutePointRepository routePointRepository;
    private final LapRepository lapRepository;

    // 1. 기록 저장하기
    @Transactional
    public Long createRecord(String email, RecordDto.Request request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("코스 없음"));

        if (request.getGroupId() == null) {
            throw new IllegalArgumentException("대회(그룹) ID가 없습니다. 혼자 뛰기는 불가능합니다.");
        }

        RunningGroup runningGroup = runningGroupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 그룹입니다."));

        Optional<RunRecord> existingRecordOpt = recordRepository.findByUserAndCourseAndRunningGroup(user, course, runningGroup);

        RunRecord savedRecord;

        if (existingRecordOpt.isPresent()) {
            RunRecord existingRecord = existingRecordOpt.get();
            int oldSeconds = parseTimeToSeconds(existingRecord.getRunTime());
            int newSeconds = parseTimeToSeconds(request.getRunTime());

            if (newSeconds < oldSeconds) {
                existingRecord.updateRecord(
                        request.getRunTime(),
                        request.getDistance(),
                        request.getAveragePace(),
                        request.getCalories(),
                        request.getHeartRate(),
                        request.getSectionJson(),
                        request.getRouteData()
                );
                lapRepository.deleteByRunRecord(existingRecord);
                saveLaps(existingRecord, request.getSectionJson());
                savedRecord = existingRecord;
            } else {
                return existingRecord.getId();
            }

        } else {
            RunRecord newRecord = new RunRecord(
                    user,
                    course,
                    runningGroup,
                    request.getRunTime(),
                    request.getDistance(),
                    request.getAveragePace(),
                    request.getCalories(),
                    request.getHeartRate(),
                    request.getSectionJson(),
                    request.getRouteData(),
                    "페이스가 안정적이었어요!",
                    request.getEndTime()
            );
            savedRecord = recordRepository.save(newRecord);
            saveLaps(savedRecord, request.getSectionJson());
        }

        saveRoutePoints(savedRecord, request.getRouteData());
        return savedRecord.getId();
    }

    private void saveLaps(RunRecord record, String sectionJson) {
        if (sectionJson == null || sectionJson.isEmpty()) return;

        ObjectMapper mapper = new ObjectMapper();
        try {
            List<Map<String, Object>> lapMaps = mapper.readValue(sectionJson, new TypeReference<List<Map<String, Object>>>() {});
            List<Lap> laps = new ArrayList<>();

            for (Map<String, Object> map : lapMaps) {
                int km = Integer.parseInt(String.valueOf(map.get("km")));
                String paceStr = String.valueOf(map.get("pace"));

                double lapPace = 0.0;
                int lapTime = 0;

                try {
                    if (paceStr.contains("'")) {
                        String cleanPace = paceStr.replace("''", "").replace("\"", "");
                        String[] parts = cleanPace.split("'");
                        double min = Double.parseDouble(parts[0]);
                        double sec = Double.parseDouble(parts[1]);
                        lapPace = min + (sec / 100.0);
                        lapTime = (int) (min * 60 + sec);
                    }
                } catch (Exception e) {
                    lapPace = 0.0;
                    lapTime = 0;
                }
                laps.add(new Lap(record, km, lapPace, lapTime));
            }
            lapRepository.saveAll(laps);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 2. (기존) 그룹 내 최신 기록 조회 (CreatedAt 기준)
    @Transactional(readOnly = true)
    public RecordDto.DetailResponse getMyRecordInGroup(String email, Long groupId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        RunningGroup group = runningGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹이 없습니다."));

        RunRecord record = recordRepository.findTopByUserAndRunningGroupOrderByCreatedAtDesc(user, group)
                .orElse(null);

        if (record == null) return null;
        return convertToDetailResponse(record);
    }

    // ★★★ [추가됨] 그룹 내 "최고 기록(RunTime 짧은 순)" 조회 ★★★
    // 컨트롤러의 /best API가 이 메소드를 호출해야 합니다.
    @Transactional(readOnly = true)
    public RecordDto.DetailResponse getBestRecordInGroup(String email, Long groupId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        RunningGroup group = runningGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹이 없습니다."));

        // 여기서 시간순(RunTime Asc) 1등 기록을 가져옵니다!
        RunRecord record = recordRepository.findTopByUserAndRunningGroupOrderByRunTimeAsc(user, group)
                .orElse(null);

        if (record == null) return null;
        return convertToDetailResponse(record);
    }

    // 헬퍼 함수들
    private int parseTimeToSeconds(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return 999999;
        try {
            String[] parts = timeStr.split(":");
            int h = 0, m = 0, s = 0;
            if (parts.length == 3) {
                h = Integer.parseInt(parts[0]);
                m = Integer.parseInt(parts[1]);
                s = Integer.parseInt(parts[2]);
            } else if (parts.length == 2) {
                m = Integer.parseInt(parts[0]);
                s = Integer.parseInt(parts[1]);
            }
            return h * 3600 + m * 60 + s;
        } catch (Exception e) {
            return 999999;
        }
    }

    private int parsePaceToSeconds(String pace) {
        if (pace == null || !pace.contains("'")) return 0;
        try {
            String[] parts = pace.replace("''", "").replace("\"", "").split("'");
            int m = Integer.parseInt(parts[0]);
            int s = Integer.parseInt(parts[1]);
            return m * 60 + s;
        } catch (Exception e) {
            return 0;
        }
    }

    @Transactional(readOnly = true)
    public RecordDto.DetailResponse getRecordDetail(String email, Long recordId) {
        RunRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("기록 없음"));
        return convertToDetailResponse(record);
    }

    @Transactional(readOnly = true)
    public RecordDto.DetailResponse getLatestRecord(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        RunRecord record = recordRepository.findTopByUserOrderByCreatedAtDesc(user).orElse(null);
        if (record == null) return null;
        return convertToDetailResponse(record);
    }

    @Transactional(readOnly = true)
    public List<ReplayDto> getReplayData(Long groupId, Long currentUserId) {
        List<RunRecord> records = recordRepository.findByRunningGroupId(groupId);
        return records.stream().map(record -> {
            List<ReplayDto.PointDto> path = record.getRoutePoints().stream()
                    .map(p -> ReplayDto.PointDto.builder()
                            .lat(p.getLatitude()).lng(p.getLongitude()).time(p.getElapsedSeconds()).build())
                    .collect(Collectors.toList());
            String nickname = (record.getUser() != null) ? record.getUser().getNickname() : "알 수 없음";
            boolean isMe = (record.getUser() != null) && record.getUser().getId().equals(currentUserId);
            return ReplayDto.builder().runRecordId(record.getId()).nickname(nickname).isMe(isMe).path(path).build();
        }).collect(Collectors.toList());
    }

    private void saveRoutePoints(RunRecord record, String routeDataJson) {
        if (routeDataJson == null || routeDataJson.isEmpty()) return;
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<Map<String, Object>> points = mapper.readValue(routeDataJson, new TypeReference<List<Map<String, Object>>>() {});
            List<RoutePoint> entities = new ArrayList<>();
            for (Map<String, Object> p : points) {
                double lat = Double.parseDouble(String.valueOf(p.get("lat")));
                double lng = Double.parseDouble(String.valueOf(p.get("lng")));
                Object timeObj = p.get("time");
                int time = (timeObj != null) ? Integer.parseInt(String.valueOf(timeObj)) : 0;
                entities.add(new RoutePoint(record, lat, lng, time));
            }
            routePointRepository.saveAll(entities);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private RecordDto.DetailResponse convertToDetailResponse(RunRecord record) {
        RunningGroup currentGroup = record.getRunningGroup();

        int totalRunners = 1;
        int myRank = 1;
        String groupAvgPace = "-'--''";
        String timeDiffText = "-";
        String groupName = "개인 러닝";

        if (currentGroup != null) {
            groupName = currentGroup.getGroupName();

            // 순위 계산
            totalRunners = recordRepository.countByRunningGroup(currentGroup);
            String myRunTime = record.getRunTime() != null ? record.getRunTime() : "00:00";
            myRank = recordRepository.countByRunningGroupAndRunTimeLessThan(currentGroup, myRunTime) + 1;

            // 평균 페이스 계산
            List<RunRecord> groupRecords = recordRepository.findAllByRunningGroup(currentGroup);
            int myPaceSeconds = parsePaceToSeconds(record.getAveragePace());
            double totalPaceSeconds = 0;
            int validCount = 0;
            for (RunRecord r : groupRecords) {
                int p = parsePaceToSeconds(r.getAveragePace());
                if (p > 0) { totalPaceSeconds += p; validCount++; }
            }
            int groupAvgSeconds = (validCount > 0) ? (int) (totalPaceSeconds / validCount) : 0;
            groupAvgPace = (groupAvgSeconds > 0) ? formatSecondsToPace(groupAvgSeconds) : "-'--''";

            if (groupAvgSeconds > 0 && myPaceSeconds > 0) {
                int diff = groupAvgSeconds - myPaceSeconds;
                if (diff > 0) timeDiffText = diff + "초 더 빠름";
                else if (diff < 0) timeDiffText = Math.abs(diff) + "초 더 느림";
                else timeDiffText = "평균과 같음";
            }
        }

        List<LapDto> lapList = record.getLaps().stream().map(LapDto::new).collect(Collectors.toList());
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> routeList;
        try {
            if (record.getRouteData() != null) {
                routeList = mapper.readValue(record.getRouteData(), new TypeReference<List<Map<String, Object>>>() {});
            } else { routeList = List.of(); }
        } catch (Exception e) { routeList = List.of(); }

        String formattedEndTime = (record.getEndTime() != null)
                ? record.getEndTime().format(DateTimeFormatter.ofPattern("h:mm a")) : "-";
        List<String> badges = Arrays.asList("8km 완주", "첫 기록 달성");

        return new RecordDto.DetailResponse(
                groupName,
                record.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                record.getCreatedAt().format(DateTimeFormatter.ofPattern("h:mm a")),
                formattedEndTime,
                record.getRunTime(),
                record.getDistance(),
                record.getAveragePace(),
                record.getCalories(),
                record.getHeartRate(),
                lapList,
                routeList,
                myRank,
                totalRunners,
                groupAvgPace,
                timeDiffText,
                record.getAnalysisResult(),
                badges
        );
    }

    private String formatSecondsToPace(int totalSeconds) {
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return String.format("%d'%02d''", m, s);
    }
}