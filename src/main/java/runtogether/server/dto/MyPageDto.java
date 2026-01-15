package runtogether.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MyPageDto {
    // 1. 유저 프로필 영역
    private String nickname;      // 예: 열정열정
    private String userCode;      // 예: 2251 0295 1291 (화면에 보이는 ID)
    private String profileImage;  // 프로필 이미지 URL

    // 2. 최근 대회 영역 (없으면 null일 수도 있음)
    private String competitionTitle; // 예: 2025 숙명여대 마라톤
    private String courseName;       // 예: 여의도 고구마 코스
    private String period;           // 예: 2025.10.25 - 2025.11.1

    // 3. 기록 통계 영역
    private String totalDistance;    // 예: 5.2 km
    private String totalTime;        // 예: 00:45:12
    private Integer totalCalories;   // 예: 320
}