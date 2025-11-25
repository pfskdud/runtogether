package runtogether.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty; // ★ 추가됨
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

public class GroupDto {

    // 1. 그룹 생성 요청 (UI에 맞춰 필드 추가)
    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        private String groupName;
        private LocalDate startDate; // 앱에서는 안 보이지만 필요하다면 유지, 아니면 삭제
        private LocalDate endDate;   // 상동
        private String description;  // 그룹 소개

        @JsonProperty("isSecret")
        private boolean isSecret;     // 공개 여부

        @JsonProperty("isSearchable")
        private boolean isSearchable; // ★ 추가: 검색 허용

        private Integer maxPeople;    // ★ 추가: 그룹 인원

        private String tags;          // ★ 추가: 태그 (예: "오운완,한강")
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

        private Integer maxPeople;
        private String tags;
    }
}