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

    // 생성자
    public RunRecord(User user, Course course, String runTime) {
        this.user = user;
        this.course = course;
        this.runTime = runTime;
        this.createdAt = LocalDateTime.now(); // 생성되는 순간 현재 시간 저장
    }
}