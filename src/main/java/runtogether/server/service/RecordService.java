package runtogether.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runtogether.server.domain.*;
import runtogether.server.dto.LapDto;
import runtogether.server.dto.RecordDto;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter; // 날짜 포맷용

@Service
@RequiredArgsConstructor
public class RecordService {

    private final RunRecordRepository recordRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    // 1. 기록 저장하기
    @Transactional
    public Long createRecord(String email, RecordDto.Request request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("코스 없음"));

        // ★ [수정] 엔티티에 analysisResult가 추가되었으므로 생성자에도 넣어줘야 합니다.
        RunRecord record = new RunRecord(
                user,
                course,
                request.getRunTime(),
                request.getDistance(),
                request.getAveragePace(),
                request.getCalories(),
                request.getHeartRate(),
                request.getSectionJson(),
                request.getRouteData(),
                "페이스가 안정적이었어요!",// (임시) 분석 결과는 AI나 별도 로직으로 만들어야 하지만 일단 기본값 저장
                request.getEndTime()
        );

        return recordRepository.save(record).getId();
    }

    // 2. 상세 조회 (수정됨!)
    @Transactional(readOnly = true)
    public RecordDto.DetailResponse getRecordDetail(String email, Long recordId) {
        RunRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("기록 없음"));

        // (1) 순위 및 기타 정보 계산
        int totalRunners = recordRepository.countByCourse(record.getCourse());
        int myRank = recordRepository.countByCourseAndRunTimeLessThan(record.getCourse(), record.getRunTime()) + 1;

        // (2) ★ Laps 변환: DB 엔티티 리스트 -> DTO 리스트로 변환
        List<LapDto> lapList = record.getLaps().stream()
                .map(LapDto::new)
                .collect(Collectors.toList());

        // (3) ★ RouteData 변환: JSON 문자열 -> 진짜 리스트(Map)로 변환
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> routeList;
        try {
            // DB에 있는 String을 읽어서 자바 List로 바꿈
            routeList = mapper.readValue(record.getRouteData(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            routeList = List.of(); // 에러나면 빈 리스트 반환
        }

        // 나머지 값들 준비
        String groupAvgPace = "7'10\"";
        String timeDiffText = "18초 더 빠름";
        List<String> badges = Arrays.asList("8km 완주", "첫 기록 달성");

        String formattedEndTime = (record.getEndTime() != null)
                ? record.getEndTime().format(DateTimeFormatter.ofPattern("h:mm a"))
                : "-";

        // (4) 최종 응답 반환 (리스트로 바뀐 lapList, routeList를 넣어줌)
        return new RecordDto.DetailResponse(
                record.getCourse().getRunningGroup().getName(),
                record.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                record.getCreatedAt().format(DateTimeFormatter.ofPattern("h:mm a")),
                formattedEndTime, // ★ [추가] 여기에 종료 시간 들어감
                record.getRunTime(),
                record.getDistance(),
                record.getAveragePace(),
                record.getCalories(),
                record.getHeartRate(),
                lapList,    // ★ List<LapDto> 들어감
                routeList,  // ★ List<Map> 들어감
                myRank,
                totalRunners,
                groupAvgPace,
                timeDiffText,
                record.getAnalysisResult(),
                badges
        );
    }

    // [보조 메서드] 시간 차이 계산기 ("MM:SS" 포맷 가정)
    private String calculateTimeDifference(String myRunTime, int standardMin) {
        try {
            // 내 기록을 '초' 단위로 변환 (예: "56:42" -> 3402초)
            String[] parts = myRunTime.split(":");
            int mySeconds = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);

            // 기준 시간(평균)을 '초' 단위로 변환
            int standardSeconds = standardMin * 60;

            int diff = standardSeconds - mySeconds; // 양수면 내가 빠른 것

            if (diff > 0) {
                return diff + "초 더 빠름";
            } else if (diff < 0) {
                return Math.abs(diff) + "초 더 느림";
            } else {
                return "평균과 같음";
            }
        } catch (Exception e) {
            return "비교 불가"; // 포맷이 안 맞을 경우 안전장치
        }
    }

    // [보조 메서드] 거리와 시간을 받아 페이스(분/km) 문자열 만들기
    private String calculatePace(int totalMinutes, double distanceKm) {
        if (distanceKm == 0) return "0'00\"";
        double pace = totalMinutes / distanceKm; // 분/km
        int paceMin = (int) pace;
        int paceSec = (int) ((pace - paceMin) * 60);
        return String.format("%d'%02d\"", paceMin, paceSec);
    }
}