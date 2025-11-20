package runtogether.demo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // ★ 수정: 1단계에서는 닉네임이 없으므로 nullable = false 제거!
    @Column(unique = true)
    private String nickname;

    // ★ 추가: 성별 (DB에는 문자열 "MALE", "FEMALE"로 저장됨)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    // ★ 추가: 생년월일
    private LocalDate birthDate;

    // ★ 추가: 프로필 이미지 URL
    private String profileImageUrl;

    // [1단계용 생성자] 이메일과 비밀번호만으로 계정 생성
    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // [2단계용 메서드] 나중에 프로필 정보를 업데이트할 때 사용
    public void updateProfile(String nickname, Gender gender, LocalDate birthDate, String profileImageUrl) {
        this.nickname = nickname;
        this.gender = gender;
        this.birthDate = birthDate;
        this.profileImageUrl = profileImageUrl;
    }
}