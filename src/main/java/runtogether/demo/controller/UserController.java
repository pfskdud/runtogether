package runtogether.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import runtogether.demo.dto.*;
import runtogether.demo.service.UserService;

import java.util.Collections;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody SignUpDto requestDto) {
        // try-catch 삭제! 에러 나면 알아서 GlobalExceptionHandler로 넘어감
        String message = userService.registerUser(requestDto);
        return ResponseEntity.ok(Collections.singletonMap("message", message));
    }

    @PostMapping("/profile")
    public ResponseEntity<?> setupProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ProfileDto requestDto) {

        userService.setupProfile(userDetails.getUsername(), requestDto);
        return ResponseEntity.ok(Collections.singletonMap("message", "프로필 설정 완료!"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto requestDto) {
        String token = userService.login(requestDto);
        return ResponseEntity.ok(new TokenResponseDto(token));
    }
}