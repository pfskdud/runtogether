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
    private Double distance;

    @Column(length = 1000)
    private String description;

    // ★ 중요: 코스가 그룹을 참조함 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private RunningGroup runningGroup;

    public Course(String title, Double distance, String description, RunningGroup runningGroup) {
        this.title = title;
        this.distance = distance;
        this.description = description;
        this.runningGroup = runningGroup;
    }
}