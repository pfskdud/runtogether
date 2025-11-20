package runtogether.demo.domain; // 1번에서 만든 패키지 이름

import jakarta.persistence.*; // javax.persistence가 아닌 jakarta.persistence 입니다!
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity // "이건 테이블 설계도입니다!" 라고 JPA에게 알림
@Table(name = "users") // 테이블 이름을 'users'로 지정
@Getter // (Lombok) .getName() 같은 메소드 자동 생성
@NoArgsConstructor // (Lombok) 기본 생성자 자동 생성
public class User {

    @Id // 이건 Primary Key (식별자) 입니다.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB가 알아서 1, 2, 3... 번호 부여
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true) // null 안됨, 중복 안됨
    private String email;

    @Column(nullable = false)
    private String password; // 암호화해서 저장할 예정

    @Column(nullable = false, unique = true) // null 안됨, 중복 안됨
    private String nickname;

    // 서비스(Service)에서 DTO와 암호화된 비밀번호를 받아
    // User 객체를 쉽게 만들기 위한 생성자
    public User(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }
}