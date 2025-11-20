package runtogether.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runtogether.demo.domain.User;
import runtogether.demo.domain.UserRepository;
import runtogether.demo.dto.UserRequestDto;
import runtogether.demo.util.JwtUtil;
import runtogether.demo.dto.LoginRequestDto;
import java.util.Optional;

@Service // "이건 비즈니스 로직을 담당하는 서비스 클래스입니다"
@RequiredArgsConstructor // (Lombok) final 필드를 위한 생성자를 자동으로 만들어줍니다.
public class UserService {

    // 1. final로 선언하여 반드시 주입받도록 함
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // SecurityConfig에 Bean으로 등록했던 암호화 기계
    private final JwtUtil jwtUtil; // JwtUtil 추가! (생성자 주입됨)

    @Transactional // "이 작업(회원가입)은 하나의 단위(트랜잭션)로 처리되어야 합니다"
    public String registerUser(UserRequestDto requestDto) {

        // 2. 닉네임 중복 검사
        // requestDto에서 닉네임을 가져와 DB에 이미 존재하는지 확인
        userRepository.findByNickname(requestDto.getNickname())
                .ifPresent(user -> { // 만약 존재한다면(ifPresent)
                    throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
                });

        // 3. (중요) 비밀번호 암호화
        // DTO에서 가져온 비밀번호를 암호화 기계(passwordEncoder)로 암호화
        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());

        // 4. User 객체 생성 및 저장
        // User.java에 만들어둔 생성자를 사용
        User newUser = new User(requestDto.getEmail(), encodedPassword, requestDto.getNickname());

        // 5. UserRepository를 통해 DB에 저장
        userRepository.save(newUser);

        return "회원가입이 완료되었습니다.";
    }

    public String login(LoginRequestDto requestDto) {
        // 1. 이메일로 유저 찾기
        User user = userRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        // 2. 비밀번호 확인 (입력받은 비번 vs DB의 암호화된 비번)
        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 3. 인증 성공 시 토큰 발급
        return jwtUtil.generateToken(user.getEmail());
    }
}