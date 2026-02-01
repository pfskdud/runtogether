package runtogether.server.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ReplayDto {
    private Long runRecordId;
    private String nickname; // 유저 닉네임
    private boolean isMe;    // 나인지?
    private List<PointDto> path; // 이동 경로

    @Data
    @Builder
    public static class PointDto {
        private double lat;
        private double lng;
        private int time; // 경과 시간
    }
}