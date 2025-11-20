package runtogether.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class CourseDto {

    // 조회 응답용
    @Getter
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String title;
        private Double distance;
        private String description;
    }
}