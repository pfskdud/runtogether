package runtogether.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runtogether.demo.domain.*;
import runtogether.demo.dto.LoginRequestDto;
import runtogether.demo.dto.ProfileDto;
import runtogether.demo.dto.SignUpDto;
import runtogether.demo.util.JwtUtil;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 1. 회원가입 (이메일, 비번만 저장)
    @Transactional
    public String registerUser(SignUpDto requestDto) {
        // 이메일 중복 검사
        if (userRepository.findByEmail(requestDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());

        // 닉네임 없이 생성
        User newUser = new User(requestDto.getEmail(), encodedPassword);
        userRepository.save(newUser);

        return "회원가입 1단계 완료! 프로필을 설정해주세요.";
    }

    // 2. 프로필 설정 (닉네임, 성별 등 업데이트)
    @Transactional
    public void setupProfile(String email, ProfileDto requestDto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        // 닉네임 중복 검사 (설정하려는 닉네임이 이미 있는지)
        if (userRepository.findByNickname(requestDto.getNickname()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 정보 업데이트 (User 엔티티에 만든 메서드 사용)
        user.updateProfile(
                requestDto.getNickname(),
                requestDto.getGender(),
                requestDto.getBirthDate(),
                requestDto.getProfileImageUrl()
        );
        // @Transactional 덕분에 user.updateProfile만 해도 DB에 자동 저장됨 (Dirty Checking)
    }

    // 3. 로그인 (기존과 동일)
    public String login(LoginRequestDto requestDto) {
        User user = userRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 잘못되었습니다."));

        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 잘못되었습니다.");
        }

        return jwtUtil.generateToken(user.getEmail());
    }
}