package com.skillexchange.common;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cổng bảo mật của cả ứng dụng (D14, D24).
 * Mỗi request đi qua một chuỗi bộ lọc (filter chain) TRƯỚC khi tới controller;
 * class này quyết định chuỗi đó gồm những bộ lọc nào và luật cho từng đường dẫn.
 */
@Configuration
public class SecurityConfig {

    private static final int MIN_SECRET_BYTES = 32; // HS256 cần khóa ít nhất 256 bit

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            ProblemAuthenticationEntryPoint authenticationEntryPoint,
                                            ProblemAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                // API REST dùng vé JWT trong header, không dùng cookie phiên → không cần CSRF,
                // không tạo session, không có form đăng nhập hay HTTP Basic.
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Luật theo đường dẫn, xét từ trên xuống, gặp luật khớp đầu tiên thì dừng.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())

                // Đọc "Authorization: Bearer <JWT>", kiểm chữ ký và hạn bằng JwtDecoder bên dưới.
                // Vé hỏng/hết hạn cũng trả 401 Problem Details qua cùng entry point.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(authenticationEntryPoint))

                // Chưa đăng nhập → 401; đã đăng nhập nhưng thiếu quyền → 403.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret) {
        return NimbusJwtDecoder.withSecretKey(jwtSigningKey(secret))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /** Dùng chung cho JwtDecoder (bây giờ) và JwtEncoder (bước 6). */
    static SecretKey jwtSigningKey(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET phải dài ít nhất " + MIN_SECRET_BYTES + " byte (hiện có " + keyBytes.length + ")");
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
