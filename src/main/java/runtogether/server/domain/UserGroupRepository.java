package runtogether.server.domain;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
    // 이미 가입했는지 확인하는 기능
    boolean existsByUserAndRunningGroup(User user, RunningGroup runningGroup);

    // ★ [추가] 특정 그룹의 가입자 수 세기
    // SQL: SELECT count(*) FROM user_groups WHERE group_id = ?
    int countByRunningGroup(RunningGroup runningGroup);
}