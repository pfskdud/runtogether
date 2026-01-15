package runtogether.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runtogether.server.domain.*;
import runtogether.server.dto.LoginRequestDto;
import runtogether.server.dto.MyPageDto;
import runtogether.server.dto.ProfileDto;
import runtogether.server.dto.SignUpDto;
import runtogether.server.repository.RunRecordRepository;
import runtogether.server.repository.UserRepository;
import runtogether.server.util.JwtUtil;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RunRecordRepository runRecordRepository;

    // 1. 회원가입 (이메일, 비번만 저장)
    @Transactional
    public String registerUser(SignUpDto requestDto) {
        if (userRepository.findByEmail(requestDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());

        // ★ 랜덤 닉네임 생성 로직
        // 예: "Runner_" + 랜덤8글자 -> "Runner_a1b2c3d4"
        String randomNickname = "Runner_" + UUID.randomUUID().toString().substring(0, 8);

        // 혹시나 랜덤 닉네임이 겹칠 확률은 0에 가깝지만,
        // User 엔티티 생성자에 닉네임을 같이 넘겨줍니다.
        User newUser = new User(requestDto.getEmail(), encodedPassword, randomNickname);

        userRepository.save(newUser);

        return "회원가입 1단계 완료!";
    }

    @Transactional(readOnly = true)
    public void checkEmailDuplicate(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        // 아무 에러가 안 나면 사용 가능한 이메일임!
    }

    // 2. 프로필 설정
    @Transactional
    public void setupProfile(String email, ProfileDto requestDto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        // ★ 1. 닉네임 처리 로직 변경
        // 일단 기존 닉네임(랜덤 닉네임)을 기본값으로 잡음
        String nicknameToSave = user.getNickname();

        // 만약 사용자가 새 닉네임을 입력했다면? (null도 아니고 빈칸도 아님)
        if (requestDto.getNickname() != null && !requestDto.getNickname().trim().isEmpty()) {
            // 그리고 그게 기존 닉네임과 다르다면?
            if (!nicknameToSave.equals(requestDto.getNickname())) {
                // 중복 검사 실행
                if (userRepository.findByNickname(requestDto.getNickname()).isPresent()) {
                    throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
                }
                // 통과하면 저장할 닉네임을 교체
                nicknameToSave = requestDto.getNickname();
            }
        }

        // ★ 2. 이미지 처리 (기존과 동일)
        String finalImageUrl = requestDto.getProfileImageUrl();
        if (finalImageUrl == null || finalImageUrl.trim().isEmpty()) {
            finalImageUrl = "default.png";
        }

        // ★ 3. 최종 업데이트 (nicknameToSave를 넣음)
        user.updateProfile(
                nicknameToSave, // 입력했으면 새거, 안 했으면 랜덤 닉네임
                requestDto.getGender(),
                requestDto.getBirthDate(),
                finalImageUrl
        );
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

    // ★ [추가] 마이페이지 데이터 조회 로직
    public MyPageDto getMyPageData(String email) {
        // 1. 이메일로 유저 찾기 (없으면 에러)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        // 2. 유저 ID 포맷팅 (예: 그냥 숫자 문자열로 변환)
        String userCode = String.valueOf(user.getId());

        String title = "참여 기록 없음";
        String course = "-";
        String distance = "0.00 km";
        String time = "00:00:00";
        int calories = 0;

        // 1. 가장 최신 기록 하나 조회
        RunRecord lastRecord = runRecordRepository.findTopByUserOrderByEndTimeDesc(user)
                .orElse(null); // 기록이 없으면 null

        // 2. 기록이 있다면 데이터 덮어쓰기
        if (lastRecord != null) {
            // (주의: RunRecord -> Group -> Course 연결이 되어 있어야 함)
            // 만약 엔티티 연결이 아직 안 되어 있다면 이 부분은 나중에 주석 해제하세요.
            /* title = lastRecord.getGroup().getCourse().getTitle();
            course = lastRecord.getGroup().getCourse().getCourseName();
            */

            // 기록 정보 (DB에 저장된 값)
            distance = String.format("%.2f km", lastRecord.getDistance()); // 소수점 2자리 예쁘게
            time = lastRecord.getRunTime();
            calories = lastRecord.getCalories();
        }
        // 4. DTO에 담아서 리턴
        return MyPageDto.builder()
                .nickname(user.getNickname())
                .userCode(userCode)
                .profileImage(user.getProfileImageUrl())
                .competitionTitle(title)
                .courseName(course)
                .totalDistance(distance)
                .totalTime(time)
                .totalCalories(calories)
                .build();
    }
}