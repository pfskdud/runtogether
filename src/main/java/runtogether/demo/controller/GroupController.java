package runtogether.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import runtogether.demo.dto.GroupDto;
import runtogether.demo.service.GroupService;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    // 1. 그룹 생성
    @PostMapping
    public ResponseEntity<?> createGroup(
            @AuthenticationPrincipal String email, // ★ 수정됨
            @RequestBody GroupDto.CreateRequest request) {
        try {
            Long groupId = groupService.createGroup(email, request);
            return ResponseEntity.ok(Collections.singletonMap("message", "그룹 생성 완료! ID: " + groupId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    // 2. 코스 추가
    @PostMapping("/{groupId}/courses")
    public ResponseEntity<?> addCourse(
            @PathVariable Long groupId,
            @RequestBody GroupDto.AddCourseRequest request) {
        try {
            groupService.addCourse(groupId, request);
            return ResponseEntity.ok(Collections.singletonMap("message", "코스 추가 완료!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    // 3. 그룹 참여
    @PostMapping("/{groupId}/join")
    public ResponseEntity<?> joinGroup(
            @AuthenticationPrincipal String email, // ★ 수정됨
            @PathVariable Long groupId) {
        try {
            String message = groupService.joinGroup(email, groupId);
            return ResponseEntity.ok(Collections.singletonMap("message", message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    // 4. 전체 그룹 목록 조회
    @GetMapping
    public ResponseEntity<List<GroupDto.Response>> getAllGroups() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }
}