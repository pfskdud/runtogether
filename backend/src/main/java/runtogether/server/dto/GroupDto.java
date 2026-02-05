package runtogether.server.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

public class GroupDto {

    // 1. 그룹 생성 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String groupName;
        private String description;
        @JsonProperty("isSecret")
        private boolean isSecret;
        @JsonProperty("isSearchable")
        private boolean isSearchable;
        private Integer maxPeople;
        private String tags;
        private Long courseId; // 기존 코스 ID (-1 또는 null 가능)
        private String startDate;
        private String endDate;

        // ★ 직접 검색 시 프론트에서 보내는 추가 데이터들
        private String courseTitle;
        private String pathData;
        private String distance;
        private String expectedTime;
    }

    // 2. 그룹 목록 응답 (Response)
    @Getter
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String groupName;
        private String description;
        private boolean isSecret;
        private String ownerName;
        private Integer maxPeople;
        private String tags;
        private Integer currentPeople;
        private Long courseId;
        private boolean isOwner;
    }

    // 3. 그룹 상세 조회 응답
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailResponse {
        private Long id;
        private String groupName;
        private String description;
        private boolean isSecret;
        private String accessCode;
        private boolean isOwner;
        private Long courseId;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
        private LocalDate startDate;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
        private LocalDate endDate;

        private long dDay;
    }

    // 4. 메인 화면 응답
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MainResponse {
        private String groupName;
        private String courseName;
        private String datePeriod;
        private String dDay;
        private String nickname;
        private Double myTotalDistance;
        private Double goalDistance;
        private String profileImageUrl;
        private Long courseId;
    }

    // 5. 코스 추가 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddCourseRequest {
        private Long groupId;
        private Long courseId;
        private String title;
        private Double distance;
        private Integer expectedTime;
        private String pathData;
        private String description;
        private String startDate;
        private String endDate;
    }

    // 6. 그룹 가입 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinRequest {
        private String accessCode;
    }

    // 7. 그룹 수정 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String groupName;
        private String description;
        @JsonProperty("isSecret")
        private boolean isSecret;
        @JsonProperty("isSearchable")
        private boolean isSearchable;
        private Integer maxPeople;
        private String tags;
    }
}