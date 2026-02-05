package runtogether.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final TmapService tmapService;

    // 1. 추천 코스 목록 조회 (isRecommended가 true인 것만!)
    @Transactional(readOnly = true)
    public List<CourseDto.Response> getRecommendedCourses() {
        // ★ findAll() 대신 추천 코스 필터링 메서드 사용
        return courseRepository.findByIsRecommendedTrue().stream()
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

        // ★ [수정됨] 엔티티 생성자에 마지막 인자로 false(사용자 생성 코스임)를 추가합니다.
        Course course = new Course(
                request.getTitle(),
                request.getDistance(),
                request.getExpectedTime(),
                request.getPathData(),
                request.getDescription(),
                start,
                end,
                group,
                false  // ★ isRecommended 필드 값 추가 (사용자가 만든 코스이므로 false)
        );
        courseRepository.save(course);
    }

    // ★ [추가] 자전거 도로 중심의 경로 검색 로직
    @Transactional(readOnly = true)
    public CourseDto.Response searchBicycleRoute(String startName, String endName) {
        // 1. 위경도 좌표 먼저 찾기
        Map<String, Double> start = tmapService.getCoordsByPlaceName(startName);
        Map<String, Double> end = tmapService.getCoordsByPlaceName(endName);

        if (start == null || end == null) {
            throw new IllegalArgumentException("출발지 또는 도착지 좌표를 찾을 수 없습니다.");
        }

        // 2. TmapService에서 전체 데이터(거리, 시간, 좌표 리스트) 한 번에 가져오기
        // (아까 tmapService에 추가한 getBicyclePathData 메서드를 사용합니다)
        Map<String, Object> routeData = tmapService.getBicyclePathData(
                start.get("lat"), start.get("lng"),
                end.get("lat"), end.get("lng")
        );

        if (routeData.isEmpty()) {
            throw new IllegalArgumentException("경로를 찾을 수 없습니다.");
        }

        // 3. DTO 구조에 맞춰 실제 데이터 매핑하여 반환
        try {
            ObjectMapper mapper = new ObjectMapper();
            // pathData 리스트를 JSON 문자열로 변환
            String jsonPath = mapper.writeValueAsString(routeData.get("pathData"));

            return new CourseDto.Response(
                    -1L, // 임시 ID
                    startName + " 🚩 " + endName,
                    "자전거 도로 중심의 러닝 최적 코스입니다.",
                    (Double) routeData.get("distance"),      // ★ 진짜 거리(km)
                    (Integer) routeData.get("expectedTime"), // ★ 진짜 시간(분)
                    jsonPath
            );
        } catch (Exception e) {
            throw new RuntimeException("경로 데이터 변환 중 오류 발생", e);
        }
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