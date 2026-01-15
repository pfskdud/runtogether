package runtogether.server.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import runtogether.server.dto.*;
import runtogether.server.service.UserService;

import java.util.Collections;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    // ★ [수정됨] 이메일 중복 확인 API (POST 방식)
    // POST http://localhost:8080/api/v1/auth/check-email
    // Body: { "email": "test@example.com" }
    @PostMapping("/check-email")
    public ResponseEntity<?> checkEmailDuplicate(@RequestBody @Valid CheckEmailDto requestDto) {
        // DTO에서 이메일 꺼내서 서비스로 전달
        userService.checkEmailDuplicate(requestDto.getEmail());
        return ResponseEntity.ok(Collections.singletonMap("message", "사용 가능한 이메일입니다."));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody SignUpDto requestDto) {
        // try-catch 삭제! 에러 나면 알아서 GlobalExceptionHandler로 넘어감
        String message = userService.registerUser(requestDto);
        return ResponseEntity.ok(Collections.singletonMap("message", message));
    }

    // 2. 프로필 설정
    @PostMapping("/profile")
    public ResponseEntity<?> setupProfile(
            @AuthenticationPrincipal String email, // ★ 수정: UserDetails -> String
            @Valid @RequestBody ProfileDto requestDto) {

        // ★ 수정: userDetails.getUsername() -> email 로 변경
        userService.setupProfile(email, requestDto);
        return ResponseEntity.ok(Collections.singletonMap("message", "프로필 설정 완료!"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto requestDto) {
        String token = userService.login(requestDto);
        return ResponseEntity.ok(new TokenResponseDto(token));
    }

    // ★ [추가] 마이페이지 정보 조회
    // 요청 주소: GET /api/v1/auth/mypage
    // 헤더: Authorization: Bearer {토큰}
    @GetMapping("/mypage")
    public ResponseEntity<MyPageDto> getMyPage(@AuthenticationPrincipal String email) {
        // 토큰에 들어있는 email로 유저 정보를 찾아서 DTO로 만듦
        MyPageDto myPageData = userService.getMyPageData(email);
        return ResponseEntity.ok(myPageData);
    }

    // ★ [추가] 회원 탈퇴 API
    // 요청 주소: DELETE /api/v1/auth/withdraw
    // 헤더: Authorization: Bearer {토큰}
    @DeleteMapping("/withdraw")
    public ResponseEntity<?> withdraw(@AuthenticationPrincipal String email) {
        // 서비스에 이메일 넘겨서 삭제 요청
        userService.withdrawUser(email);
        return ResponseEntity.ok(Collections.singletonMap("message", "회원 탈퇴가 완료되었습니다."));
    }
}