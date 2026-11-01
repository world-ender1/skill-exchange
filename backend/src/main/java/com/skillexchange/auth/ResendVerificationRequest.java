package com.skillexchange.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body của POST /api/auth/resend-verification: {email}.
 * Sai định dạng thì 400 (D42); đúng định dạng thì luôn 204, dù email có trong DB hay không (D7).
 */
public record ResendVerificationRequest(

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 255, message = "Email dài tối đa 255 ký tự")
        String email) {

    public ResendVerificationRequest {
        email = Emails.normalize(email); // D9
    }
}
