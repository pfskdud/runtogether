package runtogether.demo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "running_groups")
public class RunningGroup {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    @Column(nullable = false)
    private String name; // 대회 이름

    private LocalDate startDate; // 시작일
    private LocalDate endDate;   // 종료일

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner; // 주최자(방장)

    // 생성자
    public RunningGroup(String name, LocalDate startDate, LocalDate endDate, User owner) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.owner = owner;
    }
}