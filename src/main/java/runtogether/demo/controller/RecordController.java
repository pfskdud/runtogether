package runtogether.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import runtogether.demo.dto.RecordDto;
import runtogether.demo.service.RecordService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    // 1. 기록 저장 API
    @PostMapping
    public ResponseEntity<String> createRecord(
            @AuthenticationPrincipal String email, // ★ 수정됨
            @RequestBody RecordDto.Request requestDto) {

        recordService.createRecord(email, requestDto);
        return ResponseEntity.ok("기록 저장이 완료되었습니다.");
    }

    // 2. 내 기록 조회 API
    @GetMapping("/my")
    public ResponseEntity<List<RecordDto.Response>> getMyRecords(
            @AuthenticationPrincipal String email) { // ★ 수정됨

        return ResponseEntity.ok(recordService.getMyRecords(email));
    }
}