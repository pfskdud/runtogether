package runtogether.server.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "courses")
public class Course {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Double distance; // 거리 (km)

    // ★ [추가] 예상 소요 시간 (분 단위)
    private Integer expectedTime;

    // ★ [추가] 경로 데이터 (GPS 좌표들의 배열 JSON)
    // 내용이 길기 때문에 TEXT 타입으로 설정
    @Column(columnDefinition = "TEXT")
    private String pathData;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private RunningGroup runningGroup;

    // 생성자 업데이트 (새로운 필드 포함)
    public Course(String title, Double distance, Integer expectedTime, String pathData, String description, RunningGroup runningGroup) {
        this.title = title;
        this.distance = distance;
        this.expectedTime = expectedTime;
        this.pathData = pathData;
        this.description = description;
        this.runningGroup = runningGroup;
    }
}