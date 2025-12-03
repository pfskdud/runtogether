package runtogether.server.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class RecordDto {

    // 1. 기록 저장 요청 (프론트가 보내줄 데이터)
    // 요청 (프론트 -> 백엔드): "나 1번 코스, 30분 만에 뛰었어!"
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private Long courseId;    // 어떤 코스인지
        private String runTime;   // 얼마나 걸렸는지 (예: "00:30:00")
        private Double distance;    // 8.2
        private String averagePace; // "6'52\""
        private Integer heartRate;  // 148
        private Integer calories;   // 612
        private String sectionJson; // 구간 기록 & 그래프 데이터 (JSON String)
    }

    // 2. 내 기록 목록 조회 응답
    // 응답 (백엔드 -> 프론트): "너 1번 코스(한강), 30분에 뛰었고 랭킹은..."
    @Getter
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String courseTitle; // 코스 이름
        private String runTime;
        private String date;        // 뛴 날짜
    }

    // ★ [신규] 3. 기록 상세 화면 응답 (UI 꽉 채우기용)
    @Getter
    @AllArgsConstructor
    public static class DetailResponse {
        // 헤더
        private String groupName;   // "2025 숙명여대 마라톤"

        // 요약 정보
        private String totalTime;   // "56:42"
        private String date;        // "2025.10.26"
        private String startTime;   // "9:20 am"
        private Double distance;    // 8.2
        private String avgPace;     // "6'52\""
        private Integer heartRate;  // 148
        private Integer calories;   // 612

        // 복잡한 데이터 (그대로 돌려줌)
        private String sectionJson; // 구간 기록 & 그래프

        // 랭킹 정보
        private int myRank;         // 4
        private int totalRunner;    // 12

        // 배지 (일단 하드코딩 or 간단 로직)
        private String badges;      // "8km 완주, 11월 3회 달리기"
    }
}