package runtogether.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import runtogether.server.domain.*;

import java.util.List;
import java.util.Optional;

public interface RunRecordRepository extends JpaRepository<RunRecord, Long> {

    // 1. [핵심] 특정 그룹(대회) 안에서 내 기록 찾기 (저장/갱신용)
    Optional<RunRecord> findByUserAndCourseAndRunningGroup(User user, Course course, RunningGroup runningGroup);

    // 2. [그룹 통계] 순위, 평균 계산용
    int countByRunningGroup(RunningGroup runningGroup);
    int countByRunningGroupAndRunTimeLessThan(RunningGroup runningGroup, String runTime);
    List<RunRecord> findAllByRunningGroup(RunningGroup runningGroup);

    // 3. [조회] 특정 그룹 내에서 내 최신 기록 조회
    Optional<RunRecord> findTopByUserAndRunningGroupOrderByCreatedAtDesc(User user, RunningGroup runningGroup);

    Optional<RunRecord> findTopByUserAndRunningGroupOrderByRunTimeAsc(User user, RunningGroup runningGroup);

    // ★★★ [에러 해결] 아까 에러 났던 메소드 추가! (전체 최신 기록 조회용) ★★★
    Optional<RunRecord> findTopByUserOrderByCreatedAtDesc(User user);


    // ------------------- 랭킹 쿼리 (그룹 기준) -------------------

    // 4. [시간순 랭킹]
    @Query("SELECT r FROM RunRecord r JOIN FETCH r.user " +
            "WHERE r.runningGroup.id = :groupId " +
            "ORDER BY r.runTime ASC")
    List<RunRecord> findRankingByTotalTime(@Param("groupId") Long groupId);

    // 5. [구간별 랭킹]
    @Query("SELECT l FROM Lap l JOIN FETCH l.runRecord r JOIN FETCH r.user u " +
            "WHERE r.runningGroup.id = :groupId AND l.lapKm = :km " +
            "ORDER BY l.lapTime ASC")
    List<Lap> findRankingBySection(@Param("groupId") Long groupId, @Param("km") int km);


    // ------------------- 기존 메소드 & 호환성 유지 -------------------

    List<RunRecord> findAllByCourse(Course course);
    Optional<RunRecord> findTopByUserOrderByEndTimeDesc(User user);

    int countByCourse(Course course);
    int countByCourseAndRunTimeLessThan(Course course, String runTime);

    List<RunRecord> findByRunningGroupId(Long runningGroupId);
    void deleteByRunningGroup(RunningGroup runningGroup);
    void deleteByCourse(Course course);
}