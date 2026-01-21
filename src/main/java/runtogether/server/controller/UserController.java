package runtogether.server.controller;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import runtogether.server.domain.User;
import runtogether.server.dto.*;
import runtogether.server.repository.UserRepository;
import runtogether.server.service.UserService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1") // ★ 수정: /auth를 떼고 공통 경로로 설정
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository; // ★ [추가] 이게 없어서 에러 났었습니다!

    // ===========================
    //  1. 인증 (Auth) 관련 API
    // ===========================

    // 이메일 중복 확인
    // URL: /api/v1/auth/check-email
    @PostMapping("/auth/check-email")
    public ResponseEntity<?> checkEmailDuplicate(@RequestBody @Valid CheckEmailDto requestDto) {
        userService.checkEmailDuplicate(requestDto.getEmail());
        return ResponseEntity.ok(Collections.singletonMap("message", "사용 가능한 이메일입니다."));
    }

    // 회원가입
    // URL: /api/v1/auth/register
    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@Valid @RequestBody SignUpDto requestDto) {
        String message = userService.registerUser(requestDto);
        return ResponseEntity.ok(Collections.singletonMap("message", message));
    }

    // 로그인
    // URL: /api/v1/auth/login
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto requestDto) {
        String token = userService.login(requestDto);
        return ResponseEntity.ok(new TokenResponseDto(token));
    }

    // ==========================================================
    // ★ [수정됨] 프로필 설정 (JSON -> Multipart/form-data)
    // ==========================================================
    @PostMapping(value = "/auth/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> setupProfile(
            @AuthenticationPrincipal String email,

            // 1. 사진 파일 받기 (변수명 "image"는 프론트와 맞춰야 함)
            @RequestPart(value = "image", required = false) MultipartFile image,

            // 2. 나머지 정보(닉네임 등) 받기 (JSON 대신 Form 데이터로 매핑)
            @Valid @ModelAttribute ProfileDto requestDto
    ) {
        // 서비스로 이메일, DTO, 파일까지 다 넘김
        userService.setupProfile(email, requestDto, image);

        return ResponseEntity.ok(Collections.singletonMap("message", "프로필 설정 완료!"));
    }

    // 마이페이지 조회
    @GetMapping("/auth/mypage")
    public ResponseEntity<MyPageDto> getMyPage(@AuthenticationPrincipal String email) {
        MyPageDto myPageData = userService.getMyPageData(email);
        return ResponseEntity.ok(myPageData);
    }

    // 회원 탈퇴
    @DeleteMapping("/auth/withdraw")
    public ResponseEntity<?> withdraw(@AuthenticationPrincipal String email) {
        userService.withdrawUser(email);
        return ResponseEntity.ok(Collections.singletonMap("message", "회원 탈퇴가 완료되었습니다."));
    }

    // ===========================
    //  2. 유저 (Users) 관련 API
    // ===========================

    // ★ 내 정보(닉네임) 조회
    // URL: /api/v1/users/info (이제 앱이 찾는 주소와 일치합니다!)
    @GetMapping("/users/info")
    public ResponseEntity<Map<String, Object>> getMyInfo(@AuthenticationPrincipal String email) {
        // Service 안 거치고 바로 Repository 조회 (간단한 조회라 가능)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        Map<String, Object> response = new HashMap<>();
        response.put("nickname", user.getNickname());
        response.put("email", user.getEmail());

        return ResponseEntity.ok(response);
    }
}