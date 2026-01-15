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

        // 출발: 마곡엠밸리 15단지
        double startLat = 37.557266;
        double startLng = 126.828154;

        // 도착: 서울식물원
        double endLat = 37.569413;
        double endLng = 126.835025;

        return tmapService.getPedestrianPath(startLat, startLng, endLat, endLng);
    }
}