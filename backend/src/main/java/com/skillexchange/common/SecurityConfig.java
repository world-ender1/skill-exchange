package com.skillexchange.common;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.skillexchange.auth.AccountStatusFilter;
import com.skillexchange.user.UserRepository;

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
                                            ProblemAccessDeniedHandler accessDeniedHandler,
                                            UserRepository userRepository) throws Exception {
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

                // Đọc "Authorization: Bearer <JWT>", kiểm chữ ký và hạn bằng JwtDecoder bên dưới,
                // rồi biến claim "role" thành quyền ROLE_STUDENT / ROLE_ADMIN.
                // Vé hỏng/hết hạn cũng trả 401 Problem Details qua cùng entry point.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(roleClaimConverter()))
                        .authenticationEntryPoint(authenticationEntryPoint))

                // Vé đã qua kiểm chữ ký → đọc lại tài khoản từ DB: bị xóa 401, bị khóa 403 (D13).
                .addFilterAfter(
                        new AccountStatusFilter(userRepository, authenticationEntryPoint, accessDeniedHandler),
                        BearerTokenAuthenticationFilter.class)

                // Chưa đăng nhập → 401; đã đăng nhập nhưng thiếu quyền → 403.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    /**
     * /api/auth/** là API công khai: bỏ qua header Authorization ở đó.
     * Nếu không, vé cũ đã hết hạn còn nằm trong trình duyệt sẽ làm chính request đăng nhập bị 401.
     */
    private static BearerTokenResolver bearerTokenResolver() {
        RequestMatcher authPaths = PathPatternRequestMatcher.pathPattern("/api/auth/**");
        DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();
        return request -> authPaths.matches(request) ? null : defaultResolver.resolve(request);
    }

    /** Claim "role" = "STUDENT" → quyền "ROLE_STUDENT", để hasRole("ADMIN") ở trên dùng được (D14). */
    private static JwtAuthenticationConverter roleClaimConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /**
     * Kiểm chữ ký HS256 và hạn của vé. Hạn kiểm theo bean Clock dùng chung (D18), cùng đồng hồ
     * với lúc phát vé (JwtTokenService), nên test vặn đồng hồ giả là vé hết hạn theo.
     * Lệch giờ cho phép = 0: một máy chủ vừa phát vừa kiểm vé, không có hai đồng hồ lệch nhau.
     */
    @Bean
    JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey(secret))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        JwtTimestampValidator expiryValidator = new JwtTimestampValidator(Duration.ZERO);
        expiryValidator.setClock(clock);
        expiryValidator.setAllowEmptyExpiryClaim(false); // vé không có exp thì không nhận
        decoder.setJwtValidator(expiryValidator);
        return decoder;
    }

    /** Ký vé khi đăng nhập (JwtTokenService), cùng khóa với JwtDecoder. */
    @Bean
    JwtEncoder jwtEncoder(@Value("${app.jwt.secret}") String secret) {
        return NimbusJwtEncoder.withSecretKey(jwtSigningKey(secret))
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    /** Dùng chung cho JwtDecoder và JwtEncoder. */
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
