package runtogether.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import runtogether.server.dto.CourseDto;
import runtogether.server.service.CourseService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    // ★ AI 추천 코스 API
    // GET http://localhost:8080/api/v1/courses/recommendations
    @GetMapping("/recommendations")
    public ResponseEntity<List<CourseDto.Response>> getRecommendedCourses() {
        return ResponseEntity.ok(courseService.getRecommendedCourses());
    }
}