package runtogether.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import runtogether.server.domain.*;
import runtogether.server.dto.LoginRequestDto;
import runtogether.server.dto.MyPageDto;
import runtogether.server.dto.ProfileDto;
import runtogether.server.dto.SignUpDto;
import runtogether.server.repository.RunRecordRepository;
import runtogether.server.repository.UserRepository;
import runtogether.server.util.JwtUtil;

import java.util.UUID;
import java.io.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RunRecordRepository runRecordRepository;

    // ★ 파일 저장 경로 (프로젝트 루트의 uploads 폴더)
    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";

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

    // 2. 프로필 설정 (★ MultipartFile image 매개변수 추가)
    @Transactional
    public void setupProfile(String email, ProfileDto requestDto, MultipartFile image) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        // [1] 닉네임 처리
        String nicknameToSave = user.getNickname();
        if (requestDto.getNickname() != null && !requestDto.getNickname().trim().isEmpty()) {
            if (!nicknameToSave.equals(requestDto.getNickname())) {
                if (userRepository.findByNickname(requestDto.getNickname()).isPresent()) {
                    throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
                }
                nicknameToSave = requestDto.getNickname();
            }
        }

        // [2] 이미지 파일 처리 (★ 파일 업로드 로직 적용)
        String finalImageUrl = user.getProfileImageUrl(); // 기본값은 기존 이미지

        if (image != null && !image.isEmpty()) {
            // 새 파일이 업로드된 경우 로컬에 저장하고 경로 반환
            finalImageUrl = uploadFile(image);
        } else if (requestDto.getProfileImageUrl() != null && !requestDto.getProfileImageUrl().isEmpty()) {
            // 파일은 없지만 텍스트로 URL이 온 경우 (기존 방식 유지용)
            finalImageUrl = requestDto.getProfileImageUrl();
        }

        if (finalImageUrl == null || finalImageUrl.trim().isEmpty()) {
            finalImageUrl = "default.png";
        }

        // [3] 최종 업데이트
        user.updateProfile(
                nicknameToSave,
                requestDto.getGender(),
                requestDto.getBirthDate(),
                finalImageUrl
        );
    }

    // ★ [추가] 실제 파일을 서버 로컬에 저장하는 프라이빗 메서드
    private String uploadFile(MultipartFile file) {
        try {
            File folder = new File(UPLOAD_DIR);
            if (!folder.exists()) folder.mkdirs(); // 폴더 없으면 생성

            String originalName = file.getOriginalFilename();
            String extension = originalName != null ? originalName.substring(originalName.lastIndexOf(".")) : ".jpg";
            String savedName = UUID.randomUUID().toString() + extension;

            // 파일 저장
            file.transferTo(new File(UPLOAD_DIR + savedName));

            // 프론트에서 접근할 경로 리턴 (예: /uploads/uuid.jpg)
            return "/uploads/" + savedName;

        } catch (IOException e) {
            throw new RuntimeException("프로필 사진 저장 실패", e);
        }
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