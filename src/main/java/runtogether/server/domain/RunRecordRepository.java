package runtogether.server.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RunRecordRepository extends JpaRepository<RunRecord, Long> {
    // 특정 유저의 모든 기록을 가져오는 기능 (마이페이지용)
    List<RunRecord> findAllByUser(User user);

    // ★ [추가] 이 코스를 뛴 전체 사람 수 (12명 중...)
    int countByCourse(Course course);

    // ★ [추가] 나보다 기록(runTime)이 더 빠른 사람 수 (4위...)
    // (String 비교라 정확하진 않지만, "00:56:42" 처럼 자릿수 맞추면 비교 가능)
    int countByCourseAndRunTimeLessThan(Course course, String runTime);
}