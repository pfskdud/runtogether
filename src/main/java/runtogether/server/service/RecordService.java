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
import runtogether.server.repository.RunRecordRepository;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecordService {

    // ★ [수정] 변수명 통일 (runRecordRepository -> recordRepository)
    // 기존 코드에서 이미 recordRepository로 선언되어 있었으므로 이를 사용합니다.
    private final RunRecordRepository recordRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final RunningGroupRepository runningGroupRepository;
    private final RoutePointRepository routePointRepository;

    // 1. 기록 저장하기
    @Transactional
    public Long createRecord(String email, RecordDto.Request request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("코스 없음"));

        RunningGroup runningGroup = null;
        if (request.getGroupId() != null) {
            runningGroup = runningGroupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new IllegalArgumentException("그룹 없음"));
        }

        RunRecord record = new RunRecord(
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

        RunRecord savedRecord = recordRepository.save(record);

        saveRoutePoints(savedRecord, request.getRouteData());

        return savedRecord.getId();
    }

    // 2. 상세 조회
    @Transactional(readOnly = true)
    public RecordDto.DetailResponse getRecordDetail(String email, Long recordId) {
        RunRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("기록 없음"));

        return convertToDetailResponse(record);
    }

    // ▼ 리플레이 데이터 조회
    @Transactional(readOnly = true)
    public List<ReplayDto> getReplayData(Long groupId, Long currentUserId) {
        List<RunRecord> records = recordRepository.findByRunningGroupId(groupId);

        return records.stream().map(record -> {
            List<ReplayDto.PointDto> path = record.getRoutePoints().stream()
                    .map(p -> ReplayDto.PointDto.builder()
                            .lat(p.getLatitude())
                            .lng(p.getLongitude())
                            .time(p.getElapsedSeconds())
                            .build())
                    .collect(Collectors.toList());

            String nickname = (record.getUser() != null) ? record.getUser().getNickname() : "알 수 없음";
            boolean isMe = (record.getUser() != null) && record.getUser().getId().equals(currentUserId);

            return ReplayDto.builder()
                    .runRecordId(record.getId())
                    .nickname(nickname)
                    .isMe(isMe)
                    .path(path)
                    .build();
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

    // ★ [수정] 유저의 가장 최근 기록 가져오기
    @Transactional(readOnly = true)
    public RecordDto.DetailResponse getLatestRecord(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // ★ [수정] recordRepository 사용 (runRecordRepository -> recordRepository)
        RunRecord record = recordRepository.findTopByUserOrderByCreatedAtDesc(user)
                .orElse(null);

        if (record == null) {
            return null;
        }

        // ★ [수정] DetailResponse 생성 로직을 공통 메소드로 분리하여 사용
        return convertToDetailResponse(record);
    }

    // ★ [추가] RunRecord 엔티티를 RecordDto.DetailResponse로 변환하는 공통 메소드
    private RecordDto.DetailResponse convertToDetailResponse(RunRecord record) {
        // (1) 순위 계산
        int totalRunners = recordRepository.countByCourse(record.getCourse());
        String myRunTime = record.getRunTime() != null ? record.getRunTime() : "00:00";
        int myRank = recordRepository.countByCourseAndRunTimeLessThan(record.getCourse(), myRunTime) + 1;

        // (2) Laps 변환
        List<LapDto> lapList = record.getLaps().stream()
                .map(LapDto::new)
                .collect(Collectors.toList());

        // (3) RouteData 변환
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> routeList;
        try {
            if (record.getRouteData() != null) {
                routeList = mapper.readValue(record.getRouteData(), new TypeReference<List<Map<String, Object>>>() {});
            } else {
                routeList = List.of();
            }
        } catch (Exception e) {
            routeList = List.of();
        }

        String groupAvgPace = "7'10\"";
        String timeDiffText = "18초 더 빠름";
        List<String> badges = Arrays.asList("8km 완주", "첫 기록 달성");

        String formattedEndTime = (record.getEndTime() != null)
                ? record.getEndTime().format(DateTimeFormatter.ofPattern("h:mm a"))
                : "-";

        String groupName = (record.getRunningGroup() != null) ? record.getRunningGroup().getGroupName() : "개인 러닝";

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
}