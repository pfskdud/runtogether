package runtogether.server.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class RecordDto {

    // 요청 (프론트 -> 백엔드): "나 1번 코스, 30분 만에 뛰었어!"
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private Long courseId;    // 어떤 코스인지
        private String runTime;   // 얼마나 걸렸는지 (예: "00:30:00")
    }

    // 응답 (백엔드 -> 프론트): "너 1번 코스(한강), 30분에 뛰었고 랭킹은..."
    @Getter
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String courseTitle; // 코스 이름
        private String runTime;
        private String date;        // 뛴 날짜
    }
}