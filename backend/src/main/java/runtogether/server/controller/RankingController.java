package runtogether.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import runtogether.server.dto.RankingDto;
import runtogether.server.service.RankingService;

import java.util.List;

@RestController
@RequestMapping("/api/v1") // ★ 수정: 공통 경로로 변경 (기존: /api/v1/courses)
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    // 랭킹 조회 API
    // URL 변경: /api/v1/courses/{id}/rankings -> /api/v1/groups/{groupId}/rankings
    @GetMapping("/groups/{groupId}/rankings")
    public ResponseEntity<List<RankingDto>> getRankings(
            @AuthenticationPrincipal String email, // ★ 수정: 토큰에서 이메일 자동 추출 (앱 연동 필수)
            @PathVariable Long groupId,            // ★ 수정: courseId -> groupId 로 변경
            @RequestParam(defaultValue = "TOTAL") String type, // "TOTAL" or "SECTION"
            @RequestParam(required = false) Integer km         // 구간 랭킹일 때만 필요
    ) {

        System.out.println("📢 [RankingController] 요청받은 그룹 ID: " + groupId + ", 타입: " + type);
        // 서비스에도 groupId를 넘겨줍니다.
        return ResponseEntity.ok(rankingService.getRanking(email, groupId, type, km));
    }
}