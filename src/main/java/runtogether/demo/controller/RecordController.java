package runtogether.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    // POST http://localhost:8080/api/v1/records
    @PostMapping
    public ResponseEntity<String> createRecord(
            @AuthenticationPrincipal UserDetails userDetails, // ★ 토큰에서 유저 정보(이메일) 자동 추출!
            @RequestBody RecordDto.Request requestDto) {

        recordService.createRecord(userDetails.getUsername(), requestDto);
        return ResponseEntity.ok("기록 저장이 완료되었습니다.");
    }

    // 2. 내 기록 조회 API
    // GET http://localhost:8080/api/v1/records/my
    @GetMapping("/my")
    public ResponseEntity<List<RecordDto.Response>> getMyRecords(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(recordService.getMyRecords(userDetails.getUsername()));
    }
}