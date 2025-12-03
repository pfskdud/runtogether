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
import runtogether.server.dto.CourseDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String OPENAI_URL = "https://api.openai.com/v1/chatgit add ./completions";

    private final String API_KEY = "여기에 키 입력";

    // 지도 경로를 AI가 못 그리므로, 우리가 준비한 '안전한 경로'를 랜덤으로 붙여줍니다.
    private final String MOCK_PATH_1 = "[{\"lat\":37.55,\"lng\":126.99},{\"lat\":37.56,\"lng\":127.00}]";
    private final String MOCK_PATH_2 = "[{\"lat\":37.53,\"lng\":126.92},{\"lat\":37.51,\"lng\":126.99}]";

    public List<CourseDto.Response> getAiRecommendedCourses(String keyword) {
        if (keyword == null || keyword.isEmpty()) keyword = "서울";

        // 1. 프롬프트(질문) 구성
        String prompt = String.format(
                "%s 지역의 러닝 코스 3곳을 추천해줘. " +
                        "응답은 반드시 아래 JSON 배열 형식으로만 줘. 다른 말은 하지 마. " +
                        "[{\"title\": \"코스명\", \"description\": \"설명\", \"distance\": 거리(숫자만), \"expectedTime\": 시간(분단위 숫자)}]",
                keyword
        );

        // 2. 요청 바디
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo"); // 가성비 모델
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", 0.7);

        // 3. 헤더
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(API_KEY); // 여기서 자동으로 "Bearer sk-..."로 만들어줌

        // 4. 전송
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(OPENAI_URL, entity, String.class);
            return parseAiResponse(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
            // 에러 나면 빈 리스트 반환
            return new ArrayList<>();
        }
    }

    // AI 응답(JSON String) -> DTO 변환
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
            for (JsonNode node : coursesNode) {
                String randomPath = (Math.random() > 0.5) ? MOCK_PATH_1 : MOCK_PATH_2;

                list.add(new CourseDto.Response(
                        -1L,                                // 1. id
                        node.path("title").asText(),        // 2. title
                        node.path("description").asText(),  // 3. description
                        node.path("distance").asDouble(),   // 4. distance
                        node.path("expectedTime").asInt(),  // 5. expectedTime
                        randomPath                          // 6. pathData
                ));
            }
        }
        return list;
    }
}