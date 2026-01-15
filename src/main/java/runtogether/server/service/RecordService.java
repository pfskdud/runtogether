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
    private final RoutePointRepository routePointRepository; // ★ [추가] 좌표 저장을 위해 필요

    // 1. 기록 저장하기
    @Transactional
    public Long createRecord(String email, RecordDto.Request request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("코스 없음"));

        // ★ [수정 1] 그룹 찾기 로직 추가 (request에 groupId가 있다고 가정)
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

        // ★ [수정 3] 리플레이를 위해 'RoutePoint' 엔티티 별도 저장 (필수!)
        // JSON 문자열인 routeData를 파싱해서 RoutePoint 테이블에 저장해야 함
        saveRoutePoints(savedRecord, request.getRouteData());

        return savedRecord.getId();
    }

    // 2. 상세 조회
    @Transactional(readOnly = true)
    public RecordDto.DetailResponse getRecordDetail(String email, Long recordId) {
        RunRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("기록 없음"));

        // (1) 순위 계산
        int totalRunners = recordRepository.countByCourse(record.getCourse());
        // null safe 처리 (혹시 모를 에러 방지)
        String myRunTime = record.getRunTime() != null ? record.getRunTime() : "00:00";
        int myRank = recordRepository.countByCourseAndRunTimeLessThan(record.getCourse(), myRunTime) + 1;

        // (2) Laps 변환
        List<LapDto> lapList = record.getLaps().stream()
                .map(LapDto::new)
                .collect(Collectors.toList());

        // (3) RouteData 변환 (JSON String -> List)
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

        // 기타 더미 데이터
        String groupAvgPace = "7'10\"";
        String timeDiffText = "18초 더 빠름";
        List<String> badges = Arrays.asList("8km 완주", "첫 기록 달성");

        String formattedEndTime = (record.getEndTime() != null)
                ? record.getEndTime().format(DateTimeFormatter.ofPattern("h:mm a"))
                : "-";

        // 그룹 이름 null 처리
        String groupName = (record.getRunningGroup() != null) ? record.getRunningGroup().getName() : "개인 러닝";

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

    // ▼ [수정 2] 리플레이 데이터 조회
    @Transactional(readOnly = true)
    public List<ReplayDto> getReplayData(Long groupId, Long currentUserId) {

        // ★ [주의] Repository에 이 이름(findByRunningGroupId)으로 메소드가 있어야 함!
        // 만약 AndIsFinishedTrue를 쓰고 싶으면 Repository에도 똑같이 만들어야 합니다.
        List<RunRecord> records = recordRepository.findByRunningGroupId(groupId);

        return records.stream().map(record -> {
            // 좌표 변환
            List<ReplayDto.PointDto> path = record.getRoutePoints().stream()
                    .map(p -> ReplayDto.PointDto.builder()
                            .lat(p.getLatitude())
                            .lng(p.getLongitude())
                            .time(p.getElapsedSeconds())
                            .build())
                    .collect(Collectors.toList());

            // 유저 정보 null 처리
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

    // ★ [보조] JSON 문자열을 파싱해서 RoutePoint 엔티티로 저장하는 함수
    private void saveRoutePoints(RunRecord record, String routeDataJson) {
        if (routeDataJson == null || routeDataJson.isEmpty()) return;

        ObjectMapper mapper = new ObjectMapper();
        try {
            // JSON 형태: [{"lat":37.5, "lng":127.0, "time":0}, {"lat":..., "time":1}, ...]
            List<Map<String, Object>> points = mapper.readValue(routeDataJson, new TypeReference<List<Map<String, Object>>>() {});

            List<RoutePoint> entities = new ArrayList<>();
            for (Map<String, Object> p : points) {
                double lat = Double.parseDouble(String.valueOf(p.get("lat"))); // 안전한 형변환
                double lng = Double.parseDouble(String.valueOf(p.get("lng")));

                // 값을 먼저 꺼내보고
                Object timeObj = p.get("time");

                // 값이 있으면 숫자로 바꾸고, 없으면(null이면) 0으로 퉁친다!
                int time = (timeObj != null) ? Integer.parseInt(String.valueOf(timeObj)) : 0;

                entities.add(new RoutePoint(record, lat, lng, time));
            }
            routePointRepository.saveAll(entities); // 한 번에 저장

        } catch (Exception e) {
            e.printStackTrace(); // 파싱 실패 시 로그 출력
        }
    }
}