package runtogether.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // http://.../uploads/파일명.png 요청이 들어오면
        // 실제 프로젝트 폴더의 uploads/ 폴더 안에서 파일을 찾습니다.
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/"); // ./ 대신 그냥 uploads/ 로 시도해 보세요.
    }
}