package runtogether.demo.domain;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
    // 이미 가입했는지 확인하는 기능
    boolean existsByUserAndRunningGroup(User user, RunningGroup runningGroup);
}