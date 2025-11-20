package runtogether.demo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long id;

    @Column(nullable = false)
    private String title; // 코스 이름 (예: 한강 5km 코스)

    @Column(nullable = false)
    private Double distance; // 거리 (km 단위, 예: 5.0)

    @Column(length = 1000) // 설명을 길게 쓸 수 있게
    private String description; // 코스 설명

    // 나중에 '경로 좌표(GPS)' 데이터도 넣어야 하지만, 일단 기본 정보부터!

    // 생성자
    public Course(String title, Double distance, String description) {
        this.title = title;
        this.distance = distance;
        this.description = description;
    }
}