package runtogether.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runtogether.server.domain.CourseRepository;
import runtogether.server.dto.CourseDto;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    // [삭제됨] createCourse 기능은 이제 GroupService가 담당하므로 삭제했습니다.

    // 2. 코스 목록 조회 기능 (이건 남겨둡니다)
    @Transactional(readOnly = true)
    public List<CourseDto.Response> getCourseList() {
        return courseRepository.findAll().stream()
                .map(course -> new CourseDto.Response(
                        course.getId(),
                        course.getTitle(),
                        course.getDistance(),
                        course.getDescription()))
                .collect(Collectors.toList());
    }
}