package runtogether.server.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class CourseDto {

    // ★ [수정됨] AI 추천 코스 응답용 DTO
    // 화면에 뿌려줄 모든 정보를 담아야 합니다.
    @Getter
    @AllArgsConstructor
    public static class Response {
        private Long id;             // 추천 코스는 -1, -2...
        private String title;        // 코스 이름
        private String description;  // 설명

        private Double distance;     // 거리 (km)
        private Integer expectedTime; // 예상 소요 시간 (분)
        private String pathData;     // 지도 경로 데이터 (JSON String)
    }
}