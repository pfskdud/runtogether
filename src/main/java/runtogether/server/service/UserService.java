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

    // ★ [수정] 마이페이지 데이터 조회 로직 (성별, 생년월일 추가)
    @Transactional(readOnly = true) // 조회 전용 트랜잭션 걸어주면 좋습니다
    public MyPageDto getMyPageData(String email) {
        // 1. 이메일로 유저 찾기
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        // 2. 유저 ID 포맷팅
        String userCode = String.valueOf(user.getId());

        // 3. 기록 초기값 설정
        String title = "참여 기록 없음";
        String course = "-";
        String distance = "0.00 km";
        String time = "00:00:00";
        int calories = 0;

        // 4. 가장 최신 기록 조회
        RunRecord lastRecord = runRecordRepository.findTopByUserOrderByEndTimeDesc(user)
                .orElse(null);

        // 5. 기록이 있다면 데이터 덮어쓰기
        if (lastRecord != null) {
            // (주의: RunRecord -> Group -> Course 엔티티 연결 필요 시 주석 해제)
            /* if (lastRecord.getGroup() != null && lastRecord.getGroup().getCourse() != null) {
                title = lastRecord.getGroup().getCourse().getTitle();
                course = lastRecord.getGroup().getCourse().getCourseName();
            }
            */

            distance = String.format("%.2f km", lastRecord.getDistance());
            time = lastRecord.getRunTime();
            calories = lastRecord.getCalories();
        }

        // 6. DTO에 담아서 리턴 (★ 여기 두 줄 추가됨!)
        return MyPageDto.builder()
                .nickname(user.getNickname())
                .userCode(userCode)
                .profileImage(user.getProfileImageUrl())

                // ★ [추가] DB에 있는 성별과 생년월일을 DTO로 변환
                // (Enum이면 .name(), LocalDate면 .toString() 사용)
                .gender(user.getGender() != null ? user.getGender().name() : "MALE")
                .birthDate(user.getBirthDate() != null ? user.getBirthDate().toString() : "2000-01-01")

                .competitionTitle(title)
                .courseName(course)
                .totalDistance(distance)
                .totalTime(time)
                .totalCalories(calories)
                .build();
    }

    // ★ [추가] 회원 탈퇴 로직
    @Transactional
    public void withdrawUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        // 유저 삭제 (연관된 기록이 있다면 User 엔티티의 Cascade 설정에 따라 같이 삭제됨)
        userRepository.delete(user);
    }
}