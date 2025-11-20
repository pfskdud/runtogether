package runtogether.demo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "user_groups")
public class UserGroup {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private RunningGroup runningGroup;

    private LocalDateTime joinedAt;

    public UserGroup(User user, RunningGroup runningGroup) {
        this.user = user;
        this.runningGroup = runningGroup;
        this.joinedAt = LocalDateTime.now();
    }
}