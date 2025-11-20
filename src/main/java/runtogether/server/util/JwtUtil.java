package runtogether.server.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // 32글자 이상 비밀키
    private static final String SECRET_KEY = "runtogether_secret_key_runtogether_secret_key";
    private static final long EXPIRATION_TIME = 3600000; // 1시간
    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    // 1. 토큰 생성 (기존과 동일)
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ★ 2. 토큰에서 이메일 꺼내기 (추가됨)
    public String getEmailFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    // ★ 3. 토큰 유효성 검사 (추가됨)
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true; // 문제 없으면 true
        } catch (Exception e) {
            return false; // 위조되거나 만료됐으면 false
        }
    }
}