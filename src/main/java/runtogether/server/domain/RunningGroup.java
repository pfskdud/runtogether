package runtogether.server.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "running_groups")
public class RunningGroup {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    private boolean isSecret;

    private Integer maxPeople;
    private boolean isSearchable;
    private String tags;

    @Column(unique = true)
    private String accessCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    // ★★★ [핵심] 그룹 삭제 시 연관된 데이터 자동 삭제 설정 (Cascade) ★★★

    // 1. 코스 (Course)
    @OneToMany(mappedBy = "runningGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Course> courses = new ArrayList<>();

    // 2. 참가자 명단 (UserGroup)
    @OneToMany(mappedBy = "runningGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserGroup> members = new ArrayList<>();

    // 3. 러닝 기록 (RunRecord)
    @OneToMany(mappedBy = "runningGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RunRecord> records = new ArrayList<>();


    public RunningGroup(String name, String description,
                        boolean isSecret, boolean isSearchable, Integer maxPeople, String tags, User owner) {
        this.name = name;
        this.description = description;
        this.isSecret = isSecret;
        this.isSearchable = isSearchable;
        this.maxPeople = maxPeople;
        this.tags = tags;
        this.owner = owner;

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

    public void updateInfo(String name, String description) {
        this.name = name;
        this.description = description;
    }
}