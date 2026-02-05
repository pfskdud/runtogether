package runtogether.server.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; // Setter 추가 (필요시 사용)

import java.security.SecureRandom;
import java.time.LocalDate; // ★ 날짜 타입을 위해 필수 import
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "running_groups")
public class RunningGroup {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    @Column(nullable = false)
    private String groupName; // ★ name -> groupName으로 변경 (Service와 통일)

    @Column(length = 500)
    private String description;

    private boolean isSecret;

    private Integer maxPeople;
    private boolean isSearchable;
    private String tags;
    private LocalDate startDate;
    private LocalDate endDate;

    @Column(unique = true)
    private String accessCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    // 1. 코스 (Course) - 주의: 그룹 삭제 시 코스는 삭제 안 되게 하려면 CascadeType.REMOVE 제외해야 함
    // 일단 기존 코드 유지하되, 서비스에서 disconnectGroup()을 호출하면 안전함
    @OneToMany(mappedBy = "runningGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Course> courses = new ArrayList<>();

    // 2. 참가자 명단 (UserGroup)
    @OneToMany(mappedBy = "runningGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserGroup> members = new ArrayList<>();

    // 3. 러닝 기록 (RunRecord)
    @OneToMany(mappedBy = "runningGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RunRecord> records = new ArrayList<>();

    // ★ 생성자 수정 (날짜 포함)
    public RunningGroup(String groupName, String description,
                        boolean isSecret, boolean isSearchable, Integer maxPeople, String tags,
                        User owner, LocalDate startDate, LocalDate endDate) { // 날짜 파라미터 추가
        this.groupName = groupName;
        this.description = description;
        this.isSecret = isSecret;
        this.isSearchable = isSearchable;
        this.maxPeople = maxPeople;
        this.tags = tags;
        this.owner = owner;
        this.startDate = startDate;
        this.endDate = endDate;

        if (isSecret) {
            this.accessCode = generateRandomCode();
        }
    }

    private String generateRandomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 10; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    public void updateInfo(String groupName, String description) {
        this.groupName = groupName;
        this.description = description;
    }
}