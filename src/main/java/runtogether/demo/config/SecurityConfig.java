package runtogether.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration // "이건 설정 파일입니다"
@EnableWebSecurity // "Spring Security를 활성화합니다"
public class SecurityConfig {

    // 1. 비밀번호 암호화 기계(PasswordEncoder)를 Bean으로 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. 보안 규칙 설정
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // (1) CSRF 보호 비활성화 (API 서버는 보통 비활성화합니다)
                .csrf(AbstractHttpConfigurer::disable)

                // (2) HTTP 요청에 대한 접근 권한 설정
                .authorizeHttpRequests(authorize -> authorize
                        // "/api/v1/auth/**" 경로의 모든 요청은...
                        .requestMatchers("/api/v1/auth/**").permitAll() // ...누구나 접근(permit) 허용!

                        // 그 외의 모든 요청은...
                        .anyRequest().authenticated() // ...인증된(로그인한) 사용자만 접근 허용!
                );

        return http.build();
    }
}