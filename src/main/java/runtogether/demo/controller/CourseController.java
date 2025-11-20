package runtogether.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import runtogether.demo.dto.CourseDto;
import runtogether.demo.service.CourseService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses") // 기본 주소: localhost:8080/api/v1/courses
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    // 1. 코스 등록 API (POST)
    @PostMapping
    public ResponseEntity<String> createCourse(@RequestBody CourseDto.Request requestDto) {
        courseService.createCourse(requestDto);
        return ResponseEntity.ok("코스 등록이 완료되었습니다.");
    }

    // 2. 코스 목록 조회 API (GET)
    @GetMapping
    public ResponseEntity<List<CourseDto.Response>> getCourseList() {
        return ResponseEntity.ok(courseService.getCourseList());
    }
}