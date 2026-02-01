package runtogether.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runtogether.server.domain.Course;
import runtogether.server.repository.CourseRepository;
import runtogether.server.domain.RunningGroup;
import runtogether.server.repository.RunningGroupRepository;
import runtogether.server.dto.CourseDto;
import runtogether.server.dto.GroupDto;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final RunningGroupRepository groupRepository;

    // 1. 전체 코스 목록 조회 (DB에서 진짜 데이터를 가져옵니다!)
    @Transactional(readOnly = true)
    public List<CourseDto.Response> getRecommendedCourses() {
        // DB에 저장된 모든 코스를 가져와서 DTO로 변환합니다.
        return courseRepository.findAll().stream()
                .map(course -> new CourseDto.Response(
                        course.getId(),
                        course.getTitle(),
                        course.getDescription(),
                        course.getDistance(),
                        course.getExpectedTime(),
                        course.getPathData()
                ))
                .collect(Collectors.toList());
    }

    // 2. 경로 검색 (검색 로직이 아직 없다면 일단 전체 목록 중 첫 번째를 반환하거나 유지)
    @Transactional(readOnly = true)
    public CourseDto.Response searchRoute(CourseDto.RouteRequest request) {
        // 실제 운영 시에는 검색 로직이 필요하지만,
        // 지금은 DB의 첫 번째 코스를 반환하거나 상세 조회 로직을 활용하세요.
        return getRecommendedCourses().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("등록된 코스가 없습니다."));
    }

    // 3. 코스 추가 (그룹 생성 시 코스 저장용 - DB 연동 유지)
    @Transactional
    public void addCourse(Long groupId, GroupDto.AddCourseRequest request) {
        RunningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 없음"));

        // ★ [추가됨] 글자("2025-05-01")를 날짜로 변환!
        LocalDate start = LocalDate.parse(request.getStartDate());
        LocalDate end = LocalDate.parse(request.getEndDate());

        Course course = new Course(
                request.getTitle(),
                request.getDistance(),
                request.getExpectedTime(),
                request.getPathData(),
                request.getDescription(),
                start,  // ★ 변환된 날짜 객체를 넣습니다
                end,    // ★ 변환된 날짜 객체를 넣습니다
                group
        );
        courseRepository.save(course);
    }

    // 4. ★ [추가] 코스 상세 조회 (ID로 찾기)
    @Transactional(readOnly = true)
    public CourseDto.Response getCourseDetail(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("해당 코스가 존재하지 않습니다. id=" + courseId));

        return new CourseDto.Response(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getDistance(),
                course.getExpectedTime(),
                course.getPathData() // 상세 조회니까 경로 데이터 필수!
        );
    }
}