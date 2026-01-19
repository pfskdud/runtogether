package runtogether.server.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import runtogether.server.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RunRecordRepository extends JpaRepository<RunRecord, Long> {

    Optional<RunRecord> findTopByUserOrderByEndTimeDesc(User user);

    // 특정 유저의 모든 기록을 가져오는 기능 (마이페이지용)
    List<RunRecord> findAllByUser(User user);

    // ★ [추가] 이 코스를 뛴 전체 사람 수 (12명 중...)
    int countByCourse(Course course);

    // ★ [추가] 나보다 기록(runTime)이 더 빠른 사람 수 (4위...)
    // (String 비교라 정확하진 않지만, "00:56:42" 처럼 자릿수 맞추면 비교 가능)
    int countByCourseAndRunTimeLessThan(Course course, String runTime);

    // ★ [추가] 유저의 가장 최근 기록 1개 찾기
    // SQL: SELECT * FROM run_records WHERE user_id = ? ORDER BY created_at DESC LIMIT 1
    Optional<RunRecord> findTopByUserOrderByCreatedAtDesc(User user);

    // 1. [시간순 랭킹] 특정 코스의 기록을 시간(runTime) 빠른 순으로 가져오기
    // (문자열 비교지만 "00:56:42" 포맷이 일정하다면 정렬 잘 됨)
    @Query("SELECT r FROM RunRecord r JOIN FETCH r.user WHERE r.course.id = :courseId ORDER BY r.runTime ASC")
    List<RunRecord> findRankingByTotalTime(@Param("courseId") Long courseId);

    // 2. [구간별 랭킹] 특정 코스 + 특정 구간(km)의 기록을 시간(lapTime) 빠른 순으로 가져오기
    // (Lap 테이블과 RunRecord 테이블을 조인해서 가져옴)
    @Query("SELECT l FROM Lap l JOIN FETCH l.runRecord r JOIN FETCH r.user u " +
            "WHERE r.course.id = :courseId AND l.lapKm = :km " +
            "ORDER BY l.lapTime ASC")
    List<Lap> findRankingBySection(@Param("courseId") Long courseId, @Param("km") int km);

    List<RunRecord> findByRunningGroupId(Long runningGroupId);

    // ★ [추가] 특정 그룹의 모든 기록 삭제
    void deleteByRunningGroup(RunningGroup runningGroup);
    void deleteByCourse(Course course);
}