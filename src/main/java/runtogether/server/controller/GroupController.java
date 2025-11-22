package runtogether.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import runtogether.server.domain.RunningGroup;
import runtogether.server.dto.GroupDto;
import runtogether.server.service.GroupService;

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
            @AuthenticationPrincipal String email,
            @RequestBody GroupDto.CreateRequest request) {

        Long groupId = groupService.createGroup(email, request);

        // 생성된 그룹 정보를 가져와서, 비공개면 '입장 코드'도 같이 알려줌
        RunningGroup group = groupService.getGroup(groupId);
        String message = "그룹 생성 완료!";

        if (group.isSecret()) {
            message += " [입장코드: " + group.getAccessCode() + "]";
        }

        return ResponseEntity.ok(Collections.singletonMap("message", message));
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
    // 코드(accessCode)는 Body에 담아서 보냄. 공개 그룹이면 빈칸으로 보냄.
    @PostMapping("/{groupId}/join")
    public ResponseEntity<?> joinGroup(
            @AuthenticationPrincipal String email,
            @PathVariable Long groupId,
            @RequestBody(required = false) GroupDto.JoinRequest request) {
        // required = false: 공개 그룹일 땐 body 아예 안 보내도 되게 함

        String code = (request != null) ? request.getAccessCode() : null;

        String message = groupService.joinGroup(email, groupId, code);
        return ResponseEntity.ok(Collections.singletonMap("message", message));
    }

    // 4. 전체 그룹 목록 조회
    @GetMapping
    public ResponseEntity<List<GroupDto.Response>> getAllGroups() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }
}