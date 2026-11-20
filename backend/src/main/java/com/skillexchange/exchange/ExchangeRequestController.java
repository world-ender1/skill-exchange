package com.skillexchange.exchange;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Yêu cầu trao đổi (TK 4.6). Quyền Active: SecurityConfig chặn người không phải sinh viên ACTIVE (403) trước
 * khi tới đây, kể cả khi body sai (D87). Controller chỉ nhận request và trả response; luật nằm ở service.
 *
 * {id} kiểu long: "abc" không đổi được sang số nên Spring trả 400 "Tham số id có giá trị không hợp lệ: abc"
 * (D38) trước khi gọi service, đúng thứ tự 400 trước 404 của D87.
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
        return requestService.create(userId(jwt), request);
    }

    /** Hộp yêu cầu (D91): box bắt buộc (thiếu → 400 "Thiếu tham số bắt buộc: box"), status không bắt buộc. */
    @GetMapping
    public List<RequestItem> list(@AuthenticationPrincipal Jwt jwt,
                                  @RequestParam String box,
                                  @RequestParam(required = false) RequestStatus status) {
        return requestService.list(userId(jwt), box, status);
    }

    @GetMapping("/{id}")
    public RequestItem get(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return requestService.get(userId(jwt), id);
    }

    /** 200 {requestId, sessionId} (D90). Không có body, như nút bấm của giao diện. */
    @PostMapping("/{id}/accept")
    public AcceptResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return requestService.accept(userId(jwt), id);
    }

    /**
     * Body {note} không bắt buộc (D90): required = false để gửi không có body cũng được (body = null).
     * Có body thì @Valid vẫn kiểm độ dài note.
     */
    @PostMapping("/{id}/reject")
    public RequestItem reject(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                              @Valid @RequestBody(required = false) RejectRequest body) {
        String note = body == null ? null : body.note();
        return requestService.reject(userId(jwt), id, note);
    }

    @PostMapping("/{id}/cancel")
    public RequestItem cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return requestService.cancel(userId(jwt), id);
    }

    /** "Tôi là ai" lấy từ claim sub của vé, không bao giờ từ tham số hay body. */
    private static long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
