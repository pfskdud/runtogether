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
        try {
            Long groupId = groupService.createGroup(email, request);

            RunningGroup group = groupService.getGroup(groupId);
            String message = "그룹 생성 완료!";
            if (group.isSecret()) {
                message += " [입장코드: " + group.getAccessCode() + "]";
            }

            return ResponseEntity.ok(Collections.singletonMap("message", message));

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
            @AuthenticationPrincipal String email,
            @PathVariable Long groupId,
            @RequestBody(required = false) GroupDto.JoinRequest request) {
        try {
            String code = (request != null) ? request.getAccessCode() : null;
            String message = groupService.joinGroup(email, groupId, code);
            return ResponseEntity.ok(Collections.singletonMap("message", message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    // 4. ★ [수정됨] 그룹 목록 조회 (검색 + 필터링 통합)
    // 사용법: GET /api/v1/groups?keyword=한강&status=recruiting&type=public
    @GetMapping
    public ResponseEntity<List<GroupDto.Response>> getGroupList(
            @RequestParam(required = false) String keyword, // 검색어
            @RequestParam(required = false) String status,  // 모집 상태 (recruiting)
            @RequestParam(required = false) String type     // 공개 여부 (public)
    ) {
        // searchGroups 대신 getFilteredGroups를 호출합니다!
        return ResponseEntity.ok(groupService.getFilteredGroups(keyword, status, type));
    }

    // 5. 그룹 상세 조회 (설정 페이지 진입 시 호출)
    // GET http://localhost:8080/api/v1/groups/{groupId}
    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroupDetail(
            @AuthenticationPrincipal String email,
            @PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getGroupDetail(email, groupId));
    }

    // 6. 그룹 수정 (이름 변경 등)
    // PATCH http://localhost:8080/api/v1/groups/{groupId}
    @PatchMapping("/{groupId}")
    public ResponseEntity<?> updateGroup(
            @AuthenticationPrincipal String email,
            @PathVariable Long groupId,
            @RequestBody GroupDto.UpdateRequest request) {
        try {
            groupService.updateGroup(email, groupId, request);
            return ResponseEntity.ok(Collections.singletonMap("message", "그룹 정보가 수정되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    // 7. 그룹 삭제
    // DELETE http://localhost:8080/api/v1/groups/{groupId}
    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(
            @AuthenticationPrincipal String email,
            @PathVariable Long groupId) {
        try {
            groupService.deleteGroup(email, groupId);
            return ResponseEntity.ok(Collections.singletonMap("message", "그룹이 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }
}