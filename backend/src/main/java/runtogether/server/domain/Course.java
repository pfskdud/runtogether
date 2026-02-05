package runtogether.server.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

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

    private Integer expectedTime;  // 예상 소요 시간 (분 단위)

    // ★ 추가됨: 추천 코스 여부 (true: AI/시스템 추천, false: 일반 사용자 생성)
    @Column(nullable = false)
    private boolean isRecommended = false;

    // 경로 데이터 (GPS 좌표들의 배열 JSON)
    // 내용이 길기 때문에 TEXT 타입으로 설정
    @Column(columnDefinition = "TEXT")
    private String pathData;

    @Column(length = 1000)
    private String description;

    private LocalDate startDate;
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private RunningGroup runningGroup;

    // 생성자 업데이트 (isRecommended 필드 포함)
    public Course(String title, Double distance, Integer expectedTime, String pathData, String description, LocalDate startDate, LocalDate endDate, RunningGroup runningGroup, boolean isRecommended) {
        this.title = title;
        this.distance = distance;
        this.expectedTime = expectedTime;
        this.pathData = pathData;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.runningGroup = runningGroup;
        this.isRecommended = isRecommended;
    }

    // 그룹 생성 시 코스 정보를 업데이트하는 메소드
    public void updateGroupAndSchedule(RunningGroup group, LocalDate startDate, LocalDate endDate) {
        this.runningGroup = group;  // 이 코스의 주인을 그룹으로 설정
        this.startDate = startDate; // 시작일 변경
        this.endDate = endDate;     // 종료일 변경
    }

    // 그룹 삭제 시, 코스는 지우지 않고 연결만 끊기 위한 메소드
    public void disconnectGroup() {
        this.runningGroup = null; // 그룹과의 연결고리(FK)만 삭제
        this.startDate = null;    // (선택사항) 일정 정보도 초기화하고 싶다면
        this.endDate = null;
    }
}