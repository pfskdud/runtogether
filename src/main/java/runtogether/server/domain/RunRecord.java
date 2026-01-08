package runtogether.server.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class RunRecord extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long id;

    // 누가, 어디서 뛰었는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "running_group_id")
    private RunningGroup runningGroup;

    // [핵심 데이터]
    private String runTime;      // "56:42"
    private Double distance;     // 8.2 (km)
    private String averagePace;  // "6'52""
    private Integer calories;    // 612 (kcal)
    private Integer heartRate;   // 148 (bpm)
    private LocalDateTime endTime;

    // [상세 데이터 - 텍스트나 JSON으로 긴 내용 저장]
    @Column(columnDefinition = "TEXT")
    private String sectionJson;  // 구간별 기록 (표/그래프용)

    @Column(columnDefinition = "TEXT")
    private String routeData;    // 이동 경로 (지도 그리기용)

    // ▼ [추가] 분석 결과 멘트
    private String analysisResult;

    // ★ [추가] Laps 테이블과 1:N 관계 연결
    // mappedBy = "runRecord"는 Lap.java 안에 있는 변수명과 같아야 함
    @OneToMany(mappedBy = "runRecord", cascade = CascadeType.ALL)
    private List<Lap> laps = new ArrayList<>();

    @OneToMany(mappedBy = "runRecord")
    private List<RoutePoint> routePoints = new ArrayList<>();

    // 생성자 (분석 결과 추가)
    public RunRecord(User user, Course course, RunningGroup runningGroup, String runTime, Double distance,
                     String averagePace, Integer calories, Integer heartRate,
                     String sectionJson, String routeData, String analysisResult, LocalDateTime endTime) {
        this.user = user;
        this.course = course;
        this.runningGroup = runningGroup;
        this.runTime = runTime;
        this.distance = distance;
        this.averagePace = averagePace;
        this.calories = calories;
        this.heartRate = heartRate;
        this.sectionJson = sectionJson;
        this.routeData = routeData;
        this.analysisResult = analysisResult;
        this.endTime = endTime;
    }
}