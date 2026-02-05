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
    private final String POI_URL = "https://apis.openapi.sk.com/tmap/pois?version=1&searchKeyword=";
    private final String BICYCLE_URL = "https://apis.openapi.sk.com/tmap/routes/bicycle";
    private final String APP_KEY = "키입력";

    // [추가] 장소 이름으로 경로 찾기
    public List<Map<String, Object>> getPedestrianPathByName(String startName, String endName) {
        Map<String, Double> startCoord = getCoordsByPlaceName(startName);
        Map<String, Double> endCoord = getCoordsByPlaceName(endName);

        if (startCoord == null || endCoord == null) return new ArrayList<>();

        return getPedestrianPath(
                startCoord.get("lat"), startCoord.get("lng"),
                endCoord.get("lat"), endCoord.get("lng")
        );
    }

    // [추가] 이름 -> 위경도 변환
    public Map<String, Double> getCoordsByPlaceName(String placeName) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            // ★ URL 파라미터에 appKey 직접 포함
            String url = POI_URL + placeName + "&count=1&appKey=" + APP_KEY;
            String response = restTemplate.getForObject(url, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            JsonNode firstPoi = root.path("searchPoiInfo").path("pois").path("poi").get(0);

            if (firstPoi != null) {
                Map<String, Double> coords = new HashMap<>();
                coords.put("lat", firstPoi.path("noorLat").asDouble());
                coords.put("lng", firstPoi.path("noorLon").asDouble());
                return coords;
            }
        } catch (Exception e) {
            System.err.println("❌ 좌표 변환 중 에러: " + e.getMessage());
        }
        return null;
    }

    public List<Map<String, Object>> getPoiList(String keyword) {
        // 1. 방어 코드: 검색어가 없으면 바로 빈 리스트 반환
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        RestTemplate restTemplate = new RestTemplate();
        List<Map<String, Object>> resultList = new ArrayList<>();

        try {
            // 2. URL을 더 안전하게 생성 (공백 등 특수문자 처리)
            String url = String.format("https://apis.openapi.sk.com/tmap/pois?version=1&searchKeyword=%s&count=10&appKey=%s",
                    keyword, APP_KEY);

            String response = restTemplate.getForObject(url, String.class);

            if (response == null) return resultList;

            ObjectMapper mapper = new ObjectMapper();
            JsonNode pois = mapper.readTree(response).path("searchPoiInfo").path("pois").path("poi");

            if (pois.isMissingNode()) return resultList;

            for (JsonNode poi : pois) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", poi.path("name").asText());
                map.put("address", poi.path("upperAddrName").asText() + " " + poi.path("middleAddrName").asText());
                map.put("lat", poi.path("noorLat").asDouble());
                map.put("lng", poi.path("noorLon").asDouble());
                resultList.add(map);
            }
        } catch (Exception e) {
            // 3. 에러 발생 시 상세 원인 출력
            System.err.println("❌ POI API 호출 중 진짜 에러: " + e.getMessage());
            e.printStackTrace();
        }
        return resultList;
    }

    public List<Map<String, Object>> getPedestrianPath(double startLat, double startLng, double endLat, double endLng) {
        RestTemplate restTemplate = new RestTemplate();

        // 1. 헤더 설정 (JSON 타입만 설정, appKey는 뺌)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. 바디 설정
        Map<String, Object> body = new HashMap<>();
        body.put("startX", startLng);
        body.put("startY", startLat);
        body.put("endX", endLng);
        body.put("endY", endLat);
        body.put("reqCoordType", "WGS84GEO");
        body.put("resCoordType", "WGS84GEO");
        body.put("startName", "Start");
        body.put("endName", "End");
        body.put("searchOption", "10");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            // ★ POST URL 뒤에 ?appKey=... 를 붙여서 호출합니다.
            String urlWithKey = TMAP_URL + "&appKey=" + APP_KEY;
            String response = restTemplate.postForObject(urlWithKey, entity, String.class);
            return parseTmapResponse(response);
        } catch (Exception e) {
            System.err.println("❌ 경로 탐색 API 호출 중 에러: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Map<String, Object> getBicyclePathData(double startLat, double startLng, double endLat, double endLng) {
        RestTemplate restTemplate = new RestTemplate();

        // 1. 헤더 설정 (기존과 동일)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("appKey", APP_KEY);

        // 2. 바디 설정 (보행자 API 규격에 맞게 수정)
        Map<String, Object> body = new HashMap<>();
        body.put("startX", startLng);
        body.put("startY", startLat);
        body.put("endX", endLng);
        body.put("endY", endLat);
        body.put("reqCoordType", "WGS84GEO");
        body.put("resCoordType", "WGS84GEO");
        body.put("startName", "Start");
        body.put("endName", "End");

        // ★ 핵심 포인트: searchOption "30"은 '계단 제외' 경로입니다.
        // 러닝 코스 탐색 시 가장 안전하고 쾌적한 평지를 찾아줍니다.
        body.put("searchOption", "30");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            // ★ 인증 성공이 보장된 보행자용 TMAP_URL을 사용합니다.
            String url = TMAP_URL + "&appKey=" + APP_KEY;

            System.out.println("🚀 보행자 API 기반 러닝 최적 코스 탐색 시작");

            String response = restTemplate.postForObject(url, entity, String.class);

            // 기존에 만들어둔 파싱 메서드를 그대로 사용합니다.
            return parseTmapResponseFull(response);

        } catch (Exception e) {
            System.err.println("❌ 경로 탐색 실패: " + e.getMessage());
            return new HashMap<>();
        }
    }

    // [추가] 상세 정보 파싱 메서드
    private Map<String, Object> parseTmapResponseFull(String jsonResponse) {
        Map<String, Object> resultMap = new HashMap<>();
        List<Map<String, Object>> pathList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode root = mapper.readTree(jsonResponse);
            JsonNode features = root.path("features");

            // 1. 전체 거리와 시간 정보 추출 (첫 번째 feature의 properties에 들어있음)
            JsonNode firstFeatureProps = features.get(0).path("properties");
            double totalDistance = firstFeatureProps.path("totalDistance").asDouble() / 1000.0; // m -> km
            int totalTime = firstFeatureProps.path("totalTime").asInt() / 60; // 초 -> 분

            resultMap.put("distance", Math.round(totalDistance * 100) / 100.0); // 소수점 둘째자리
            resultMap.put("expectedTime", totalTime);

            // 2. 좌표 리스트 추출 (기존 로직)
            for (JsonNode feature : features) {
                JsonNode geometry = feature.path("geometry");
                if ("LineString".equals(geometry.path("type").asText())) {
                    for (JsonNode coord : geometry.path("coordinates")) {
                        Map<String, Object> point = new HashMap<>();
                        point.put("lat", coord.get(1).asDouble());
                        point.put("lng", coord.get(0).asDouble());
                        pathList.add(point);
                    }
                }
            }
            resultMap.put("pathData", pathList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultMap;
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