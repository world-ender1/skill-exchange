package com.skillexchange.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import com.skillexchange.common.ForbiddenException;
import com.skillexchange.user.Role;
import com.skillexchange.user.User;
import com.skillexchange.user.UserRepository;
import com.skillexchange.user.UserStatus;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Đọc lại tài khoản từ DB ở MỖI request có vé (D13).
 * Chạy ngay sau bộ lọc của Spring đã kiểm chữ ký và hạn của vé:
 * - user không còn tồn tại → 401;
 * - user bị khóa → 403;
 * - sinh viên ACTIVE → gắn thêm quyền ACTIVE_STUDENT (D44) rồi cho đi tiếp;
 * - còn lại → cho đi tiếp.
 * Nhờ vậy khóa tài khoản có hiệu lực ngay, không phải chờ vé hết hạn sau 24 giờ.
 *
 * Cố ý KHÔNG đánh dấu @Component: Spring Boot sẽ tự gắn mọi Filter là bean vào servlet,
 * filter sẽ chạy hai lần. SecurityConfig tự tạo và gắn nó vào chuỗi bộ lọc bảo mật.
 */
public class AccountStatusFilter extends OncePerRequestFilter {

    /** Quyền Active (TK 4.1, D33): SecurityConfig yêu cầu quyền này cho matching, tìm kiếm, hồ sơ người khác. */
    public static final String ACTIVE_STUDENT = "ACTIVE_STUDENT";

    private final UserRepository userRepository;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;

    public AccountStatusFilter(UserRepository userRepository,
                               AuthenticationEntryPoint authenticationEntryPoint,
                               AccessDeniedHandler accessDeniedHandler) {
        this.userRepository = userRepository;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Request không có vé (hoặc test dùng @WithMockUser): không có gì để đọc lại.
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            chain.doFilter(request, response);
            return;
        }

        Optional<User> user = findUser(jwtAuthentication.getToken().getSubject());
        if (user.isEmpty()) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response,
                    new BadCredentialsException("Tài khoản của vé không còn tồn tại"));
            return;
        }
        if (user.get().getStatus() == UserStatus.LOCKED) {
            accessDeniedHandler.handle(request, response, new ForbiddenException("Tài khoản đã bị khóa"));
            return;
        }
        if (isActiveStudent(user.get(), jwtAuthentication)) {
            grantActiveStudent(jwtAuthentication);
        }
        chain.doFilter(request, response);
    }

    /** Trạng thái lấy từ DB (vừa đọc ở trên, D13); vai trò lấy từ claim role của vé (D14, D44). */
    private static boolean isActiveStudent(User user, JwtAuthenticationToken authentication) {
        return user.getStatus() == UserStatus.ACTIVE
                && Role.STUDENT.name().equals(authentication.getToken().getClaimAsString("role"));
    }

    /**
     * Authentication không sửa được danh sách quyền sau khi tạo, nên tạo vé xác thực mới
     * (cùng JWT, thêm một quyền) và đặt vào một SecurityContext mới.
     * Bộ lọc phân quyền chạy SAU filter này đọc context mới, nên luật hasAuthority thấy quyền vừa gắn.
     */
    private static void grantActiveStudent(JwtAuthenticationToken current) {
        List<GrantedAuthority> authorities = new ArrayList<>(current.getAuthorities());
        authorities.add(new SimpleGrantedAuthority(ACTIVE_STUDENT));
        JwtAuthenticationToken upgraded = new JwtAuthenticationToken(current.getToken(), authorities, current.getName());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(upgraded);
        SecurityContextHolder.setContext(context);
    }

    private Optional<User> findUser(String subject) {
        try {
            return userRepository.findById(Long.valueOf(subject));
        } catch (NumberFormatException e) {
            // Chỉ xảy ra nếu vé có chữ ký đúng mà sub không phải số: coi như không có user.
            return Optional.empty();
        }
    }
}
