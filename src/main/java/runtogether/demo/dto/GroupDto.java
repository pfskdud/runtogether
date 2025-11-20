package runtogether.demo.dto;

import lombok.AllArgsConstructor; // ★ 추가
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

public class GroupDto {

    // 1. 그룹 생성 요청
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

    // ★ [추가됨] 3. 그룹 목록 조회 응답용 (이게 없어서 에러남!)
    @Getter
    @AllArgsConstructor
    public static class Response {
        private Long groupId;
        private String groupName;
        private String startDate;
        private String endDate;
        private String ownerName; // 방장 이름
    }
}