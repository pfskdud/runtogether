package runtogether.server.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import runtogether.server.domain.RunningGroup;
import runtogether.server.domain.User;
import runtogether.server.domain.UserGroup;

import java.util.List;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
    // 이미 가입했는지 확인하는 기능
    boolean existsByUserAndRunningGroup(User user, RunningGroup runningGroup);

    // ★ [추가] 특정 그룹의 가입자 수 세기
    // SQL: SELECT count(*) FROM user_groups WHERE group_id = ?
    int countByRunningGroup(RunningGroup runningGroup);

    // ★ [추가] 내가 가입한 모든 내역 조회
    // select * from user_groups where user_id = ?
    List<UserGroup> findAllByUser(User user);
}