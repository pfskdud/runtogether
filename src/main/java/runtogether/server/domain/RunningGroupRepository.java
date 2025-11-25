package runtogether.server.domain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RunningGroupRepository extends JpaRepository<RunningGroup, Long> {

    // ★ [추가] 검색 기능 (이름 또는 태그에 키워드가 포함되면 찾음)
    // SQL: SELECT * FROM running_groups WHERE name LIKE %keyword% OR tags LIKE %keyword%
    List<RunningGroup> findAllByNameContainingOrTagsContaining(String nameKeyword, String tagKeyword);
}