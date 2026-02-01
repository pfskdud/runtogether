package runtogether.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import runtogether.server.service.TmapService;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final TmapService tmapService;

    // 접속 주소: http://localhost:8080/test/path
    @GetMapping("/test/path")
    public List<Map<String, Object>> testPath() {

        // 출발: 오목교역 8번출구 투썸플레이스
        double startLat = 37.524234;
        double startLng = 126.874463;

        // 도착: 오목교역 다이소 (대학학원 지하)
        double endLat = 37.524117;
        double endLng = 126.875562;

        return tmapService.getPedestrianPath(startLat, startLng, endLat, endLng);
    }
}