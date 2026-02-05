package runtogether.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import runtogether.server.domain.User;
import runtogether.server.dto.RecordDto;
import runtogether.server.dto.ReplayDto;
import runtogether.server.service.RecordService;

import java.util.List;

@RestController
@RequestMapping("/api/v1") // ★ 수정: 범위를 /api/v1/records -> /api/v1 으로 넓혔습니다!
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;


    // 1. 기록 저장
    // URL: POST /api/v1/records
    @PostMapping("/records") // ★ 수정: 여기에 /records를 붙여줌
    public ResponseEntity<Long> createRecord(
            @AuthenticationPrincipal String email,
            @RequestBody RecordDto.Request request) {
        return ResponseEntity.ok(recordService.createRecord(email, request));
    }

    // 2. [전체] 가장 최근 기록 조회 (마이페이지용)
    // URL: GET /api/v1/records/latest
    @GetMapping("/records/latest") // ★ 수정: 여기에 /records 붙임
    public ResponseEntity<RecordDto.DetailResponse> getLatestRecord(
            @AuthenticationPrincipal String email) {
        RecordDto.DetailResponse response = recordService.getLatestRecord(email);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    // ★★★ [추가] 그룹 내 "최고 기록(Best)" 조회 (그룹 내 기록 탭용) ★★★
    // URL: GET /api/v1/groups/{groupId}/records/best
    @GetMapping("/groups/{groupId}/records/best")
    public ResponseEntity<RecordDto.DetailResponse> getBestRecordInGroup(
            @AuthenticationPrincipal String email,
            @PathVariable Long groupId
    ) {
        // 서비스의 getBestRecordInGroup 호출!
        RecordDto.DetailResponse response = recordService.getBestRecordInGroup(email, groupId);

        if (response == null) {
            return ResponseEntity.noContent().build(); // 기록 없으면 204 리턴
        }
        return ResponseEntity.ok(response);
    }

    // 3. 기록 상세 조회
    // URL: GET /api/v1/records/{recordId}
    @GetMapping("/records/{recordId}") // ★ 수정
    public ResponseEntity<RecordDto.DetailResponse> getRecordDetail(
            @AuthenticationPrincipal String email,
            @PathVariable Long recordId) {
        return ResponseEntity.ok(recordService.getRecordDetail(email, recordId));
    }

    // 4. 리플레이 API
    // URL: GET /api/v1/records/replay/{groupId}
    @GetMapping("/records/replay/{groupId}") // ★ 수정
    public ResponseEntity<List<ReplayDto>> getReplay(
            @PathVariable Long groupId,
            @AuthenticationPrincipal User user
    ) {
        List<ReplayDto> response = recordService.getReplayData(groupId, user.getId());
        return ResponseEntity.ok(response);
    }
}