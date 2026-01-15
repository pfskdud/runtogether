package runtogether.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import runtogether.server.domain.RunRecord;
import runtogether.server.repository.RunRecordRepository;
import runtogether.server.domain.User;
import runtogether.server.repository.UserRepository;
import runtogether.server.dto.CourseDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    // ★ API 키 (본인 키 유지)
    private final String API_KEY = "api key 입력";

    // ★ [수정됨] 서로 다른 3개의 가짜 경로 준비 (서울의 대표 러닝 코스 좌표들)

    // 1. 남산 둘레길 느낌
    private final String MOCK_PATH_1 = "[{\"lat\":37.551,\"lng\":126.988},{\"lat\":37.552,\"lng\":126.990},{\"lat\":37.553,\"lng\":126.991},{\"lat\":37.554,\"lng\":126.993}]";

    // 2. 여의도 한강공원 느낌
    private final String MOCK_PATH_2 = "[{\"lat\":37.528,\"lng\":126.933},{\"lat\":37.529,\"lng\":126.935},{\"lat\":37.530,\"lng\":126.938},{\"lat\":37.531,\"lng\":126.940}]";

    // 3. 잠실 석촌호수 느낌
    private final String MOCK_PATH_3 = "[{\"lat\":37.509,\"lng\":127.100},{\"lat\":37.510,\"lng\":127.102},{\"lat\":37.511,\"lng\":127.104},{\"lat\":37.509,\"lng\":127.105}]";

    private final UserRepository userRepository;     // ★ 추가
    private final RunRecordRepository runrecordRepository; // ★ 추가

    // 1. (기존) 키워드 검색 추천
    public List<CourseDto.Response> getAiRecommendedCourses(String keyword) {
        if (keyword == null || keyword.isEmpty()) keyword = "서울";
        String prompt = createPrompt(keyword + " 지역의 러닝 코스 3곳을 추천해줘.");
        return callOpenAi(prompt);
    }

    // 2. ★ [신규] 사용자 맞춤형 추천 (기록 기반)
    public List<CourseDto.Response> getPersonalizedCourses(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        // 유저의 가장 최근 기록 조회
        Optional<RunRecord> lastRecord = runrecordRepository.findTopByUserOrderByCreatedAtDesc(user);

        String prompt;
        if (lastRecord.isPresent()) {
            // 기록이 있으면: 그 코스와 비슷한 곳 추천
            String lastCourseTitle = lastRecord.get().getCourse().getTitle();
            prompt = createPrompt("사용자가 최근에 '" + lastCourseTitle + "' 코스를 뛰었어. " +
                    "이와 비슷한 분위기나 난이도의, 같은 지역(한국) 내 다른 러닝 코스 3곳을 추천해줘.");
        } else {
            // 기록이 없으면: 인기 코스 추천
            prompt = createPrompt("러닝을 처음 시작하는 사람들을 위해 한국에서 가장 인기 많고 쉬운 러닝 코스 3곳을 추천해줘.");
        }

        return callOpenAi(prompt);
    }

    // (헬퍼) 프롬프트 생성기
    private String createPrompt(String userRequest) {
        return userRequest +
                " 응답은 반드시 아래 JSON 배열 형식으로만 줘. 다른 말은 하지 마. " +
                "[{\"title\": \"코스명\", \"description\": \"설명\", \"distance\": 거리(숫자만), \"expectedTime\": 시간(분단위 숫자)}]";
    }

    // (헬퍼) OpenAI 호출 공통 로직
    private List<CourseDto.Response> callOpenAi(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo");
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        requestBody.put("temperature", 0.7);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(API_KEY);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(OPENAI_URL, entity, String.class);
            return parseAiResponse(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // (헬퍼) 파싱 로직 (기존과 동일)
    private List<CourseDto.Response> parseAiResponse(String jsonResponse) throws JsonProcessingException {
        List<CourseDto.Response> list = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        String content = root.path("choices").get(0).path("message").path("content").asText();

        int start = content.indexOf("[");
        int end = content.lastIndexOf("]");
        if (start != -1 && end != -1) {
            content = content.substring(start, end + 1);
        }

        JsonNode coursesNode = mapper.readTree(content);
        if (coursesNode.isArray()) {
            int index = 0; // ★ 순서 체크용 변수
            for (JsonNode node : coursesNode) {

                // ★ [핵심 수정] 0번, 1번, 2번 순서대로 다른 경로를 넣어줍니다!
                String pathData;
                if (index == 0) pathData = MOCK_PATH_1;
                else if (index == 1) pathData = MOCK_PATH_2;
                else pathData = MOCK_PATH_3;

                list.add(new CourseDto.Response(
                        -1L,
                        node.path("title").asText(),
                        node.path("description").asText(),
                        node.path("distance").asDouble(),
                        node.path("expectedTime").asInt(),
                        pathData
                ));

                index++;
            }
        }
        return list;
    }

    // ★ [신규/수정] 출발지-도착지 맞춤형 경로 추천
    public CourseDto.Response getRouteRecommendation(CourseDto.RouteRequest request) {
        String start = request.getStartLocation();
        String end = request.getEndLocation();

        // 1. 프롬프트 작성 (AI에게 시킬 내용)
        String prompt = String.format(
                "'%s'에서 출발하여 '%s'에 도착하는 러닝 코스를 하나 추천해줘. " +
                        "자동차가 다니는 큰 길보다는 공원, 강변, 산책로 등 러닝하기 좋은 길 위주로 구성해줘. " +
                        "응답은 반드시 아래 JSON 형식으로만 줘. 다른 말은 하지 마. " +
                        "{" +
                        "  \"title\": \"코스 이름 (예: 숙대~여의도 힐링 러닝)\", " +
                        "  \"description\": \"주요 경유지(체크포인트)를 화살표(->)로 연결해서 설명해줘. 각 지점마다 간단한 특징도 괄호 안에 적어줘. (예: 효창공원(녹지 구간) -> 마포대교(한강 뷰))\", " +
                        "  \"distance\": 예상거리(km단위 숫자만), " +
                        "  \"expectedTime\": 예상소요시간(분단위 숫자만)" +
                        "}",
                start, end
        );

        // 2. 요청 바디 구성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo");
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", 0.7);

        // 3. 헤더
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(API_KEY);

        // 4. 전송 및 파싱
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(OPENAI_URL, entity, String.class);
            return parseSingleAiResponse(response.getBody()); // ★ 단일 응답 파싱 메서드 호출
        } catch (Exception e) {
            e.printStackTrace();
            // 에러 시 비상용 하드코딩 데이터 반환
            return new CourseDto.Response(-999L, start + "~" + end, "AI 호출 실패", 5.0, 30, MOCK_PATH_1);
        }
    }

    // ★ [추가] 단일 JSON 응답 파싱용 (목록 아님)
    private CourseDto.Response parseSingleAiResponse(String jsonResponse) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        String content = root.path("choices").get(0).path("message").path("content").asText();

        // 앞뒤 군더더기 제거 ({ 로 시작해서 } 로 끝나게)
        int start = content.indexOf("{");
        int end = content.lastIndexOf("}");
        if (start != -1 && end != -1) {
            content = content.substring(start, end + 1);
        }

        JsonNode node = mapper.readTree(content);
        return new CourseDto.Response(
                -1L, // 임시 ID
                node.path("title").asText(),
                node.path("description").asText(),
                node.path("distance").asDouble(),
                node.path("expectedTime").asInt(),
                MOCK_PATH_1 // 가짜 경로
        );
    }
}