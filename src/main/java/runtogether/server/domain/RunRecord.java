package runtogether.server.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "records")
public class RunRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long id;

    // ★ 핵심: N:1 관계 설정 (기록 N개 : 유저 1명)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // DB에 user_id 라는 컬럼으로 저장됨
    private User user;

    // ★ 핵심: N:1 관계 설정 (기록 N개 : 코스 1개)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id") // DB에 course_id 라는 컬럼으로 저장됨
    private Course course;

    @Column(nullable = false)
    private String runTime; // 완주 시간 (예: "00:45:20")

    private LocalDateTime createdAt; // 기록 생성 날짜 (언제 뛰었는지)

    private Double distance;    // 총 거리 (8.2km)
    private String averagePace; // 평균 페이스 (6'52"/km)
    private Integer heartRate;  // 평균 심박수 (148)
    private Integer calories;   // 칼로리 (612)

    // ★ [추가] 구간별 기록 & 그래프 데이터 (복잡한 JSON은 문자열로 저장)
    // 예: [{"section":"1km", "time":"06:41"}, ...]
    @Column(columnDefinition = "TEXT")
    private String sectionJson;

    // 생성자
    public RunRecord(User user, Course course, String runTime, Double distance,
                     String averagePace, Integer heartRate, Integer calories, String sectionJson) {
        this.user = user;
        this.course = course;
        this.runTime = runTime;
        this.distance = distance;
        this.averagePace = averagePace;
        this.heartRate = heartRate;
        this.calories = calories;
        this.sectionJson = sectionJson;
        this.createdAt = LocalDateTime.now();
    }
}