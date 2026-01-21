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
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;


    // 1. 기록 저장 (프론트가 달리기 끝나면 호출)
    // POST /api/v1/records
    @PostMapping
    public ResponseEntity<Long> createRecord(
            @AuthenticationPrincipal String email,
            @RequestBody RecordDto.Request request) {
        return ResponseEntity.ok(recordService.createRecord(email, request));
    }

    // ★★★ [추가] 가장 최근 기록 조회 ("latest" 문자열 처리) ★★★
    // 이 메서드가 /{recordId} 보다 먼저 있거나 명시되어야 에러가 안 납니다.
    @GetMapping("/latest")
    public ResponseEntity<RecordDto.DetailResponse> getLatestRecord(
            @AuthenticationPrincipal String email) {
        // 서비스에서 최근 기록 가져오기
        RecordDto.DetailResponse response = recordService.getLatestRecord(email);

        if (response == null) {
            return ResponseEntity.noContent().build(); // 204 No Content (기록 없음)
        }
        return ResponseEntity.ok(response);
    }

    // 2. 기록 상세 조회 (결과 페이지 볼 때 호출)
    // GET /api/v1/records/{recordId}
    @GetMapping("/{recordId}")
    public ResponseEntity<RecordDto.DetailResponse> getRecordDetail(
            @AuthenticationPrincipal String email,
            @PathVariable Long recordId) {
        return ResponseEntity.ok(recordService.getRecordDetail(email, recordId));
    }

    // ▼▼▼ [여기 추가] 리플레이 API ▼▼▼
    // 요청 주소: GET /api/record/replay/{groupId}
    @GetMapping("/replay/{groupId}")
    public ResponseEntity<List<ReplayDto>> getReplay(
            @PathVariable Long groupId,
            @AuthenticationPrincipal User user // 현재 로그인한 유저 정보 (JWT 토큰에서 가져옴)
    ) {
        // 서비스에게 "이 방(groupId)의 리플레이 데이터 줘! (내 아이디는 user.getId()야)" 라고 시킴
        List<ReplayDto> response = recordService.getReplayData(groupId, user.getId());

        return ResponseEntity.ok(response);
    }
}