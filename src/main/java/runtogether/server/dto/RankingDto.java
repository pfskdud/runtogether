package runtogether.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import runtogether.server.domain.User;

@Data
@AllArgsConstructor
public class RankingDto {
    private int rank;           // 등수 (1, 2, 3...)
    private String nickname;    // 유저 닉네임 ("열정열정")
    private String profileImage;// 프로필 사진 URL
    private String recordValue; // 기록 값 ("56:42" 또는 "6'41\"")

    // 나중에 "나의 랭킹"인지 표시할 때 필요할 수 있음
    private boolean isMe;
}