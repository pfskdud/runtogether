package runtogether.demo.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // JpaRepository<User, Long>를 상속받기만 하면
    // save(), findById() 같은 기본 DB 기능들을 자동으로 다 만들어줍니다.

    // 우리는 닉네임 중복 검사용으로 이것 하나만 추가합니다.
    Optional<User> findByNickname(String nickname);
}