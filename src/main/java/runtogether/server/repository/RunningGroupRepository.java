package runtogether.server.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import runtogether.server.domain.RunningGroup;

import java.util.List;
import java.util.Optional;

public interface RunningGroupRepository extends JpaRepository<RunningGroup, Long> {

    // ★ [추가] 검색 기능 (이름 또는 태그에 키워드가 포함되면 찾음)
    // SQL: SELECT * FROM running_groups WHERE name LIKE %keyword% OR tags LIKE %keyword%
    List<RunningGroup> findAllByGroupNameContainingOrTagsContaining(String nameKeyword, String tagKeyword);

    // 2. ★ 초대 코드로 그룹 찾기
    Optional<RunningGroup> findByAccessCode(String accessCode);
}