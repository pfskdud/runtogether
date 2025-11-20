package runtogether.demo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import runtogether.demo.util.JwtUtil;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 1. 헤더에서 "Authorization" 키의 값을 가져옴
        String authorizationHeader = request.getHeader("Authorization");

        // 2. 토큰이 없거나, "Bearer "로 시작하지 않으면 그냥 통과 (검사 안 함 -> SecurityConfig에서 막힘)
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. "Bearer " 뒤의 순수 토큰 문자열만 잘라냄
        String token = authorizationHeader.substring(7);

        // 4. 토큰이 유효한지 검사
        if (jwtUtil.validateToken(token)) {
            String email = jwtUtil.getEmailFromToken(token);

            // 5. (중요) "이 사용자는 인증됐습니다!"라고 스프링에게 도장을 찍어줌 (SecurityContext에 저장)
            // (지금은 권한이 없어서 빈 리스트 Collections.emptyList()를 넣음)
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 6. 다음 단계로 진행
        filterChain.doFilter(request, response);
    }
}