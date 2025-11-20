package runtogether.server.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import runtogether.server.domain.Gender;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDto {
    private String nickname;
    private Gender gender;

    // ★ 여기가 핵심입니다. 이 줄이 없거나 오타가 있으면 날짜 에러가 납니다.
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private String profileImageUrl;
}