package com.salon.auth.config;

import com.salon.auth.security.JwtAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtAuthInterceptor jwtAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Only routes under /api/customer/** require a valid JWT cookie
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/customer/**");
    }
}
