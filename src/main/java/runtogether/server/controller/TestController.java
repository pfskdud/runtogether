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
        // 오목교역 8번출구 -> 투썸플레이스 좌표
        double startLat = 37.524300;
        double startLng = 126.873800;
        double endLat = 37.520000;
        double endLng = 126.875100;

        return tmapService.getPedestrianPath(startLat, startLng, endLat, endLng);
    }
}