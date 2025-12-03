package runtogether.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import runtogether.server.dto.CourseDto;
import runtogether.server.service.AiService;
import runtogether.server.service.CourseService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final AiService aiService; // ★ 추가됨 (AI 서비스 주입)

    // 1. AI 추천 코스 API (이제 진짜 AI가 대답함!)
    // GET http://localhost:8080/api/v1/courses/recommendations?keyword=부산
    @GetMapping("/recommendations")
    public ResponseEntity<List<CourseDto.Response>> getRecommendedCourses(
            @RequestParam(required = false) String keyword) {

        // ★ 수정됨: courseService -> aiService로 변경
        return ResponseEntity.ok(aiService.getAiRecommendedCourses(keyword));
    }

    // ★ [추가] 경로 검색 API
    // POST http://localhost:8080/api/v1/courses/search
    @PostMapping("/search")
    public ResponseEntity<CourseDto.Response> searchRoute(@RequestBody CourseDto.RouteRequest request) {
        return ResponseEntity.ok(courseService.searchRoute(request));
    }
}