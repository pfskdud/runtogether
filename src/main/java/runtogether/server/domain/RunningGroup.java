package runtogether.server.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.security.SecureRandom; // ★ 추가
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
    private String name;

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(length = 500)
    private String description;

    private boolean isSecret;

    private Integer maxPeople;
    private boolean isSearchable;
    private String tags;

    // 코드가 길어질 수 있으니 넉넉하게 저장
    @Column(unique = true)
    private String accessCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    // 생성자
    public RunningGroup(String name, String description,
                        boolean isSecret, boolean isSearchable, Integer maxPeople, String tags, User owner) {
        this.name = name;
        this.description = description;
        this.isSecret = isSecret;

        this.isSearchable = isSearchable; // 추가
        this.maxPeople = maxPeople;       // 추가
        this.tags = tags;

        this.owner = owner;

        if (isSecret) {
            this.accessCode = generateRandomCode();
        }
    }

    // ★ [수정됨] 더 강력한 랜덤 코드 생성기 (10자리, A-Z + 0-9)
    private String generateRandomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 10; i++) { // 10글자 생성
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