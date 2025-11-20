package runtogether.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class CourseDto {

    // 1. 코스 등록 요청용 (프론트 -> 백엔드)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String title;
        private Double distance;
        private String description;
    }

    // 2. 코스 조회 응답용 (백엔드 -> 프론트)
    @Getter
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String title;
        private Double distance;
        private String description;
    }
}