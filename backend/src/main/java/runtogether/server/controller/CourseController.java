package runtogether.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import runtogether.server.dto.CourseDto;
import runtogether.server.dto.GroupDto;
import runtogether.server.service.CourseService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    // 0. 전체 코스 목록 조회 (플러터 드롭다운 목록용!)
    // GET http://localhost:8080/api/v1/courses
    @GetMapping
    public ResponseEntity<List<CourseDto.Response>> getAllCourses() {
        // Service에 전체 목록을 가져오는 로직이 있다면 그것을 연결하고,
        // 없다면 일단 추천 코스 목록과 동일하게 반환하도록 설정합니다.
        return ResponseEntity.ok(courseService.getRecommendedCourses());
    }

    // 1. 추천 코스 조회
    // GET http://localhost:8080/api/v1/courses/recommendations
    @GetMapping("/recommendations")
    public ResponseEntity<List<CourseDto.Response>> getRecommendedCourses(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(courseService.getRecommendedCourses());
    }

    // 2. ★ [추가됨] 코스 상세 조회 (프론트 친구가 요청한 부분!)
    // GET http://localhost:8080/api/v1/courses/{courseId}
    // 예: /api/v1/courses/1
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseDto.Response> getCourseDetail(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseDetail(courseId));
    }

    // 3. 경로 검색
    @PostMapping("/search")
    public ResponseEntity<CourseDto.Response> searchRoute(@RequestBody CourseDto.RouteRequest request) {
        return ResponseEntity.ok(courseService.searchRoute(request));
    }

    // 4. 그룹장이 코스 추가
    @PostMapping("/groups/{groupId}")
    public ResponseEntity<String> addCourse(
            @PathVariable Long groupId,
            @RequestBody GroupDto.AddCourseRequest request) {
        courseService.addCourse(groupId, request);
        return ResponseEntity.ok("코스 추가 완료");
    }
}