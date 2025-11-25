package runtogether.server.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; // ★ 이 import가 꼭 필요합니다!

public interface CourseRepository extends JpaRepository<Course, Long> {

    // ★ [추가됨] 특정 그룹에 속한 코스들을 찾아오는 기능
    // SELECT * FROM courses WHERE group_id = ?
    List<Course> findByRunningGroup(RunningGroup runningGroup);
}