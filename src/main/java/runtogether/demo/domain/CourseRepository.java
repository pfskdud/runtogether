package runtogether.demo.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
    // JpaRepository를 상속받으면 기본 기능(저장, 조회, 삭제) 자동 완성!
}