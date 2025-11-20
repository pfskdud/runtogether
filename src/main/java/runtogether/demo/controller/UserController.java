package runtogether.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import runtogether.demo.dto.UserRequestDto;
import runtogether.demo.service.UserService;
import runtogether.demo.dto.LoginRequestDto;
import runtogether.demo.dto.TokenResponseDto;

@RestController // "이건 JSON 데이터를 주고받는 API 컨트롤러입니다"
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth") // 이 컨트롤러의 기본 주소는 "localhost:8080/api/v1/auth" 입니다.
public class UserController {

    private final UserService userService;

    // 회원가입 API
    // 주소: POST http://localhost:8080/api/v1/auth/register
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody UserRequestDto requestDto) {
        // 1. 서비스에게 회원가입 처리를 맡김
        String message = userService.registerUser(requestDto);

        // 2. 결과 메시지를 200 OK 상태코드와 함께 반환
        return ResponseEntity.ok(message);
    }

    // ★ 로그인 API 추가
    // POST http://localhost:8080/api/v1/auth/login
    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@RequestBody LoginRequestDto requestDto) {
        String token = userService.login(requestDto);
        return ResponseEntity.ok(new TokenResponseDto(token));
    }
}