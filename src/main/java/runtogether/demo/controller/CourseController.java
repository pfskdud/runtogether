package runtogether.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import runtogether.demo.dto.CourseDto;
import runtogether.demo.service.CourseService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    // [삭제됨] createCourse API 삭제

    // 2. 코스 목록 조회 API (GET)
    @GetMapping
    public ResponseEntity<List<CourseDto.Response>> getCourseList() {
        return ResponseEntity.ok(courseService.getCourseList());
    }
}