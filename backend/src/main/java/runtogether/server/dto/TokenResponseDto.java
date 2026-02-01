package runtogether.server.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor // 모든 필드를 넣는 생성자 자동 생성
public class TokenResponseDto {
    private String token;
}