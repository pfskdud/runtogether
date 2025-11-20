package runtogether.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runtogether.demo.domain.Course;
import runtogether.demo.domain.CourseRepository;
import runtogether.demo.dto.CourseDto;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    // 1. 코스 등록 기능
    @Transactional
    public Long createCourse(CourseDto.Request requestDto) {
        // DTO(그릇)에 담긴 데이터를 Entity(설계도)로 옮겨 담기
        Course course = new Course(requestDto.getTitle(), requestDto.getDistance(), requestDto.getDescription());

        // DB에 저장하고, 저장된 코스의 ID 반환
        return courseRepository.save(course).getId();
    }

    // 2. 코스 목록 조회 기능
    @Transactional(readOnly = true) // 조회만 할 때는 readOnly=true가 성능에 좋습니다
    public List<CourseDto.Response> getCourseList() {
        // DB에 있는 모든 코스를 꺼내서 -> DTO 리스트로 변환해서 반환
        return courseRepository.findAll().stream()
                .map(course -> new CourseDto.Response(
                        course.getId(),
                        course.getTitle(),
                        course.getDistance(),
                        course.getDescription()))
                .collect(Collectors.toList());
    }
}