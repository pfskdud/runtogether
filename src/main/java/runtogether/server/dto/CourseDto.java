package runtogether.server.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class CourseDto {

    @Getter
    @NoArgsConstructor
    public static class RouteRequest {
        private String startLocation; // 출발지 (예: "숙명여자대학교")
        private String endLocation;   // 도착지 (예: "여의도공원")
    }


    // ★ [수정됨] AI 추천 코스 응답용 DTO (7개 필드)
    @Getter
    @AllArgsConstructor
    public static class Response {
        private Long id;             // 추천 코스는 -1
        private String title;        // 코스명
        private String description;  // 설명
        private Double distance;     // 거리
        private Integer expectedTime; // 소요 시간
        private String pathData;     // 경로 데이터
    }


}