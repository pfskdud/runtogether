package runtogether.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import runtogether.server.domain.Lap;
import runtogether.server.domain.RunRecord;
import java.util.List;

public interface LapRepository extends JpaRepository<Lap, Long> {
    // 특정 기록에 딸린 랩 정보 다 지우기 (업데이트할 때 필요)
    void deleteByRunRecord(RunRecord runRecord);
}