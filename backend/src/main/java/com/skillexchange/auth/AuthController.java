package com.skillexchange.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** API xác thực (TK 4.2). Quyền: Public – SecurityConfig mở /api/auth/**. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // @Valid: kiểm các annotation trong RegisterRequest; sai thì GlobalExceptionHandler trả 400 kèm errors.
    // @ResponseStatus(CREATED): thành công trả 201 thay vì 200 mặc định (D29).
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    // 204 No Content: thành công, không có gì để trả về (D29).
    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.token());
    }

    // Luôn 204 khi email đúng định dạng, kể cả email chưa đăng ký (D7, D42).
    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request.email());
    }

    // 200 {accessToken}. Sai email/mật khẩu → 401, tài khoản bị khóa → 403 (D13).
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
