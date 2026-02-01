package runtogether.server.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "badges")
public class Badge {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String badgeName; // 뱃지 이름 (예: "첫 완주 달성", "10km 클리어")
    private String badgeIcon; // 뱃지 이미지 파일명 (예: "badge_10k.png")

    private LocalDateTime achievedAt; // 획득 날짜

    public Badge(User user, String badgeName, String badgeIcon) {
        this.user = user;
        this.badgeName = badgeName;
        this.badgeIcon = badgeIcon;
        this.achievedAt = LocalDateTime.now();
    }
}