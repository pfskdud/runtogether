package runtogether.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import runtogether.demo.domain.Gender;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class ProfileDto {
    private String nickname;
    private Gender gender;       // "MALE" or "FEMALE"
    private LocalDate birthDate; // "1999-01-01"
    private String profileImageUrl; // "default.png" 또는 이미지 URL
}