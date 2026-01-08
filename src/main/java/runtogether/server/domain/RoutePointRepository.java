package runtogether.server.domain; // 패키지 위치에 맞게 수정

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoutePointRepository extends JpaRepository<RoutePoint, Long> {

    // 특정 기록(RunRecord)에 속한 좌표들을 '시간 순서대로' 가져오기
    // 리플레이는 순서가 생명이니까 OrderByElapsedSecondsAsc가 필수입니다.
    List<RoutePoint> findByRunRecordIdOrderByElapsedSecondsAsc(Long runRecordId);
}