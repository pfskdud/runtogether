package runtogether.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TmapService {

    private final String TMAP_URL = "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1";
    private final String APP_KEY = "api key 입력";
    public List<Map<String, Object>> getPedestrianPath(double startLat, double startLng, double endLat, double endLng) {
        RestTemplate restTemplate = new RestTemplate();

        // 1. 헤더 설정 (AppKey)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("appKey", APP_KEY);

        // 2. 요청 바디 설정 (주의: Tmap은 X가 경도(Lng), Y가 위도(Lat)입니다)
        Map<String, Object> body = new HashMap<>();
        body.put("startX", startLng); // 경도
        body.put("startY", startLat); // 위도
        body.put("endX", endLng);
        body.put("endY", endLat);
        body.put("reqCoordType", "WGS84GEO"); // 입력 좌표계
        body.put("resCoordType", "WGS84GEO"); // 출력 좌표계
        body.put("startName", "Start"); // 필수값이라 아무거나 넣음
        body.put("endName", "End");     // 필수값이라 아무거나 넣음

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // 3. API 호출
        try {
            String response = restTemplate.postForObject(TMAP_URL, entity, String.class);
            return parseTmapResponse(response);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>(); // 에러나면 빈 리스트
        }
    }

    // Tmap 응답 JSON에서 좌표만 쏙쏙 뽑아내는 메서드
    private List<Map<String, Object>> parseTmapResponse(String jsonResponse) {
        List<Map<String, Object>> pathList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode root = mapper.readTree(jsonResponse);
            JsonNode features = root.path("features");

            for (JsonNode feature : features) {
                JsonNode geometry = feature.path("geometry");
                String type = geometry.path("type").asText();

                // "LineString" 타입인 경우만 좌표가 여러 개 들어있음 (이게 경로임)
                if ("LineString".equals(type)) {
                    JsonNode coordinates = geometry.path("coordinates");
                    for (JsonNode coord : coordinates) {
                        // Tmap은 [경도, 위도] 순서로 줌
                        double lng = coord.get(0).asDouble();
                        double lat = coord.get(1).asDouble();

                        Map<String, Object> point = new HashMap<>();
                        point.put("lat", lat);
                        point.put("lng", lng);
                        pathList.add(point);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pathList;
    }
}