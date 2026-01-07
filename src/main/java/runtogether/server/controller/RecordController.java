package runtogether.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import runtogether.server.dto.RecordDto;
import runtogether.server.service.RecordService;

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

    // 2. 기록 상세 조회 (결과 페이지 볼 때 호출)
    // GET /api/v1/records/{recordId}
    @GetMapping("/{recordId}")
    public ResponseEntity<RecordDto.DetailResponse> getRecordDetail(
            @AuthenticationPrincipal String email,
            @PathVariable Long recordId) {
        return ResponseEntity.ok(recordService.getRecordDetail(email, recordId));
    }
}