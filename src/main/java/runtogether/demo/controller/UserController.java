package runtogether.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import runtogether.demo.dto.LoginRequestDto;
import runtogether.demo.dto.TokenResponseDto;
import runtogether.demo.dto.UserRequestDto;
import runtogether.demo.service.UserService;

import java.util.Collections; // ★ 추가: Map을 쉽게 만들기 위해 필요

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 1. 회원가입 API
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRequestDto requestDto) {
        try {
            // 성공 시: 그냥 문자열 메시지 반환 (200 OK)
            String message = userService.registerUser(requestDto);
            return ResponseEntity.ok(message);

        } catch (IllegalArgumentException e) {
            // ★ 실패 시: JSON 형태로 반환 (400 Bad Request)
            // 결과 예시: { "message": "이미 가입된 이메일입니다." }
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    // 2. 로그인 API
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto requestDto) {
        try {
            // 성공 시: 토큰 객체 반환 (200 OK)
            String token = userService.login(requestDto);
            return ResponseEntity.ok(new TokenResponseDto(token));

        } catch (IllegalArgumentException e) {
            // ★ 실패 시: JSON 형태로 반환 (400 Bad Request)
            // 결과 예시: { "message": "이메일 또는 비밀번호가 잘못되었습니다." }
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("message", e.getMessage()));
        }
    }
}