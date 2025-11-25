package runtogether.server.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
        private String description;
        @JsonProperty("isSecret")
        private boolean isSecret;
        @JsonProperty("isSearchable")
        private boolean isSearchable;
        private Integer maxPeople;
        private String tags;
    }

    // 2. 코스 추가 요청 (날짜, 경로 포함)
    @Getter
    @NoArgsConstructor
    public static class AddCourseRequest {
        private String title;
        private Double distance;
        private Integer expectedTime;
        private String pathData;
        private String description;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
        private LocalDate startDate;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
        private LocalDate endDate;
    }

    // 3. 그룹 가입 요청
    @Getter
    @NoArgsConstructor
    public static class JoinRequest {
        private String accessCode;
    }

    // 4. 그룹 수정 요청
    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {
        private String groupName;
        private String description;
    }

    // 5. 그룹 목록 조회 응답
    @Getter
    @AllArgsConstructor
    public static class Response {
        private Long groupId;
        private String groupName;
        private String description;
        @JsonProperty("isSecret")
        private boolean isSecret;
        private String ownerName;
        private Integer maxPeople;
        private String tags;
        private Integer currentPeople;

        // 목록에서는 날짜를 안 보여줘도 되면 빼도 되고,
        // 필요하다면 여기에 String startDate, endDate 추가 가능
    }

    // 6. 그룹 상세 조회 응답 (설정 페이지용)
    @Getter
    @AllArgsConstructor
    public static class DetailResponse {
        private Long groupId;
        private String groupName;
        private String description;
        private boolean isSecret;
        private String accessCode;
        private boolean isOwner;
    }

    // ★ [추가됨] 7. 그룹 메인 화면 응답 (이게 없어서 에러남!)
    @Getter
    @AllArgsConstructor
    public static class MainResponse {
        private String groupName;
        private String courseName;
        private String datePeriod;
        private long dDay;
        private String nickname;
        private Double myDistance;
        private Double totalDistance;
        private String userImage;
    }
}