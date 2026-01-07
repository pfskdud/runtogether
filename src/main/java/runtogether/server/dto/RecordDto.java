package runtogether.server.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import runtogether.server.domain.RunStatus; // ★ RunStatus Enum이 없다면 이 줄과 아래 status 필드를 지우세요!

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class RecordDto {

    // 1. 기록 저장 요청 (프론트 -> 백엔드)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private Long courseId;       // 코스 ID
        private String runTime;      // "00:30:00"
        private Double distance;     // 8.2
        private String averagePace;  // "6'52\""
        private Integer calories;    // 612
        private Integer heartRate;   // 148
        private String sectionJson;  // 구간 기록 (JSON String)
        private String routeData;    // 이동 경로 (지도 그리기용)

        // ★ 만약 RunStatus 관련 에러가 나면 이 줄을 지우세요.
        private RunStatus status;
        // ★ [추가] 프론트가 보내주는 종료 시간
        // (ISO 8601 형식: "2025-10-26T10:30:00")
        private LocalDateTime endTime;
    }

    // 2. 내 기록 목록 조회 응답 (마이페이지용)
    @Getter
    @AllArgsConstructor
    public static class Response {
        private Long recordId;      // id -> recordId로 명확하게 변경
        private String title;       // courseTitle -> title
        private String date;        // 뛴 날짜
        private Double distance;    // 거리
        private String runTime;     // 시간
    }

    // 3. 기록 상세 화면 응답 (여기가 에러의 원인이었습니다! 수정 완료)
    @Getter
    @AllArgsConstructor
    public static class DetailResponse {
        // [헤더 정보]
        private String groupName;    // 그룹 이름
        private String date;         // 날짜
        private String startTime;    // 시작 시간
        // ★ [추가] 종료 시간 (화면에 보여줄 포맷팅된 문자열)
        private String endTime; // 예: "10:16 am"

        // [핵심 요약] - 순서 중요 (Service랑 맞춰야 함)
        private String runTime;      // "56:42"
        private Double distance;     // 8.2
        private String avgPace;      // "6'52\""
        private Integer calories;    // 612 (Service 순서에 맞춤)
        private Integer heartRate;   // 148

        // [복잡한 데이터]
        private List<LapDto> laps; // 구간 기록
        private List<Map<String, Object>> routeData;    // 지도 경로

        // [그룹 비교 - 순위]
        private int myRank;          // 내 순위
        private int totalRunner;     // 전체 참가자

        // [그룹 비교 - 페이스] (새로 추가됨)
        private String groupAvgPace;   // 그룹 평균 페이스
        private String paceDifference; // "18초 더 빠름"

        // [러닝 분석] (새로 추가됨)
        private String analysisResult; // "후반 페이스가 좋아요..."

        // [뱃지] (String -> List로 변경)
        private List<String> badges;   // ["8km 완주", "첫 기록"]
    }
}