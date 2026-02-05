package runtogether.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import runtogether.server.service.TmapService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController // 클래스 상단에 이게 반드시 있어야 합니다!
@RequiredArgsConstructor
@RequestMapping("/api/v1/courses")
public class TestController {

    private final TmapService tmapService;

    @GetMapping("/search")
    public Map<String, Object> searchPath(
            @RequestParam String startName,
            @RequestParam String endName
    ) {
        // 1. TmapService를 통해 장소 이름 -> 경로 데이터 획득
        // (참고: TmapService에 getPedestrianPathByName 메서드를 만들어야 합니다.)
        List<Map<String, Object>> pathData = tmapService.getPedestrianPathByName(startName, endName);

        // 2. 피그마 디자인(디스턴스, 시간 등)에 필요한 정보들을 응답
        Map<String, Object> result = new HashMap<>();
        result.put("title", startName + " ➡️ " + endName);
        result.put("distance", "약 1.2km"); // 실제 서비스에서 계산된 값을 넣으면 더 좋습니다.
        result.put("expectedTime", "약 15분");
        result.put("pathData", pathData);
        result.put("description", startName + "에서 출발하여 " + endName + "까지 가는 쾌적한 코스입니다.");

        return result;
    }

    @GetMapping("/poi-search")
    public List<Map<String, Object>> searchPoi(@RequestParam String keyword) {
        // TmapService에 POI 리스트만 가져오는 메서드를 호출합니다.
        return tmapService.getPoiList(keyword);
    }
}