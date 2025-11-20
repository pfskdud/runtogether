package runtogether.demo.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecordRepository extends JpaRepository<RunRecord, Long> {
    // 특정 유저의 모든 기록을 가져오는 기능 (마이페이지용)
    List<RunRecord> findAllByUser(User user);
}