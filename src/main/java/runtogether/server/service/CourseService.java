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
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final RunningGroupRepository groupRepository;

    // ★ 테스트용 고정 경로 데이터 (여의도 좌표)
    // 프론트엔드 테스트를 위해 무조건 이 경로만 반환합니다.
    private final String FIXED_PATH = "[{\"lat\":37.528,\"lng\":126.933},{\"lat\":37.529,\"lng\":126.935},{\"lat\":37.530,\"lng\":126.938},{\"lat\":37.531,\"lng\":126.940},{\"lat\":37.532,\"lng\":126.942}]";

    // 1. 추천 코스 조회 (무조건 고정된 1개만 반환)
    @Transactional(readOnly = true)
    public List<CourseDto.Response> getRecommendedCourses() {
        List<CourseDto.Response> list = new ArrayList<>();

        // 딱 하나만 추가
        list.add(new CourseDto.Response(
                -1L, // 임시 ID
                "여의도 벚꽃 러닝 코스", // 제목
                "봄에는 벚꽃, 가을에는 단풍! 한강 바람을 맞으며 뛰는 최고의 힐링 코스입니다. (테스트용 고정 데이터)", // 설명
                5.2, // 거리 (km)
                35,  // 소요 시간 (분)
                FIXED_PATH // 경로 데이터
        ));

        return list;
    }

    // 2. 경로 검색 (무얼 검색해도 여의도 코스 반환)
    public CourseDto.Response searchRoute(CourseDto.RouteRequest request) {
        // 사용자가 입력한 출발지/도착지는 제목에만 보여주고, 내용은 여의도로 고정
        return new CourseDto.Response(
                -999L,
                request.getStartLocation() + " ~ " + request.getEndLocation() + " (추천 코스)",
                "검색하신 지역의 러닝하기 가장 좋은 코스로 '여의도 벚꽃 코스'를 추천합니다!",
                5.2,
                35,
                FIXED_PATH
        );
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