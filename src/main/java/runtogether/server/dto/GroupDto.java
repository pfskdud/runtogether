package runtogether.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty; // ★ 추가됨
import lombok.AllArgsConstructor;
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
        private String description;

        // ★ 수정됨: JSON에서 "isSecret"이라는 이름으로 들어오면 여기에 맵핑해라!
        @JsonProperty("isSecret")
        private boolean isSecret;
    }

    // 2. 코스 추가 요청
    @Getter
    @NoArgsConstructor
    public static class AddCourseRequest {
        private String title;
        private Double distance;
        private String description;

        private Integer expectedTime;
        private String pathData;
    }

    // 3. 그룹 가입 요청
    @Getter
    @NoArgsConstructor
    public static class JoinRequest {
        private String accessCode;
    }

    // 4. 그룹 목록 조회 응답
    @Getter
    @AllArgsConstructor
    public static class Response {
        private Long groupId;
        private String groupName;
        private String description;

        @JsonProperty("isSecret") // 나갈 때도 "isSecret"으로 나가게 설정
        private boolean isSecret;

        private String startDate;
        private String endDate;
        private String ownerName;
    }
}