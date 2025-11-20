package runtogether.demo.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collections;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 우리가 의도적으로 던진 에러 (IllegalArgumentException) 잡기
    // (메시지 내용이 그때그때 바뀜)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest() // 400 Error
                .body(Collections.singletonMap("message", e.getMessage()));
    }

    // 2. ★ [추가됨] 우리가 예상하지 못한 나머지 모든 에러(Exception) 잡기
    // (NullPointer, DB 연결 실패 등등...)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAllException(Exception e) {
        // 500 Internal Server Error (서버 잘못)
        return ResponseEntity.internalServerError()
                .body(Collections.singletonMap("message", "서버에 알 수 없는 오류가 발생했습니다."));
    }
}