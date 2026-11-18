package com.skillexchange.exchange;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Yêu cầu trao đổi (TK 4.6). Quyền Active: SecurityConfig chặn người không phải sinh viên ACTIVE (403) trước
 * khi tới đây, kể cả khi body sai (D87). Controller chỉ nhận request và trả response; luật nằm ở service.
 */
@RestController
@RequestMapping("/api/requests")
public class ExchangeRequestController {

    private final ExchangeRequestService requestService;

    public ExchangeRequestController(ExchangeRequestService requestService) {
        this.requestService = requestService;
    }

    /** 201 Created kèm RequestItem (D89). Người gửi là chủ của vé ("sub" = id người dùng). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequestItem create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateRequestRequest request) {
        return requestService.create(Long.parseLong(jwt.getSubject()), request);
    }
}
