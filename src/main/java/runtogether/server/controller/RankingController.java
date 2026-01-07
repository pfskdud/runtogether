package runtogether.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import runtogether.server.dto.RankingDto;
import runtogether.server.service.RankingService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    // 랭킹 조회 API
    // GET /api/v1/courses/{courseId}/rankings?type=TOTAL
    // GET /api/v1/courses/{courseId}/rankings?type=SECTION&km=1
    @GetMapping("/{courseId}/rankings")
    public ResponseEntity<List<RankingDto>> getRankings(
            @RequestParam String email, // 테스트용 (나중에 토큰에서 추출)
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "TOTAL") String type, // "TOTAL" or "SECTION"
            @RequestParam(required = false) Integer km         // 구간 랭킹일 때만 필요
    ) {
        return ResponseEntity.ok(rankingService.getRanking(email, courseId, type, km));
    }
}