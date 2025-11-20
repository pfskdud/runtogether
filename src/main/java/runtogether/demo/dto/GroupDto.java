package runtogether.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

public class GroupDto {

    // 1. 그룹 생성 요청 (이 클래스가 꼭 있어야 함!)
    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        private String groupName;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    // 2. 코스 추가 요청
    @Getter
    @NoArgsConstructor
    public static class AddCourseRequest {
        private String title;
        private Double distance;
        private String description;
    }
}