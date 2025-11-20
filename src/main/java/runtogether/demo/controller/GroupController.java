package runtogether.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import runtogether.demo.dto.GroupDto;
import runtogether.demo.service.GroupService;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<String> createGroup(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody GroupDto.CreateRequest request) {

        Long groupId = groupService.createGroup(userDetails.getUsername(), request);
        return ResponseEntity.ok("그룹 생성 완료! ID: " + groupId);
    }

    @PostMapping("/{groupId}/courses")
    public ResponseEntity<String> addCourse(
            @PathVariable Long groupId,
            @RequestBody GroupDto.AddCourseRequest request) {

        groupService.addCourse(groupId, request);
        return ResponseEntity.ok("코스 추가 완료!");
    }

    @PostMapping("/{groupId}/join")
    public ResponseEntity<String> joinGroup(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long groupId) {

        String message = groupService.joinGroup(userDetails.getUsername(), groupId);
        return ResponseEntity.ok(message);
    }
}