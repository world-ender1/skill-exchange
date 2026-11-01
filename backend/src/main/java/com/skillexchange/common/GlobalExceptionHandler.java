package com.skillexchange.common;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Một chỗ duy nhất biến exception thành response lỗi dạng Problem Details (RFC 9457, D24).
 *
 * Kế thừa ResponseEntityExceptionHandler: Spring đã viết sẵn cách xử lý các lỗi MVC
 * (sai kiểu tham số, sai phương thức, không có đường dẫn...) thành ProblemDetail;
 * ở đây chỉ sửa lại vài loại và thêm các loại Spring không biết.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Một phần tử của thuộc tính "errors" (D24). */
    record FieldErrorItem(String field, String message) {
    }

    // ---------- 400: dữ liệu sai ----------

    /** @Valid thất bại: thêm "errors": [{field, message}] để frontend hiện lỗi cạnh từng ô. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldErrorItem> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorItem(error.getField(), error.getDefaultMessage()))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    /** Lỗi theo trường do Service phát hiện: cùng dạng với lỗi @Valid ở trên. */
    @ExceptionHandler(FieldValidationException.class)
    ResponseEntity<ProblemDetail> handleFieldValidation(FieldValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ");
        problem.setProperty("errors", List.of(new FieldErrorItem(ex.getField(), ex.getMessage())));
        return ResponseEntity.badRequest().body(problem);
    }

    /** Yêu cầu sai nhưng không thuộc trường nào (ví dụ link xác thực hỏng): chỉ có "detail". */
    @ExceptionHandler(InvalidRequestException.class)
    ResponseEntity<ProblemDetail> handleInvalidRequest(InvalidRequestException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Body không phải JSON hợp lệ. Không trả lại thông báo của Jackson (có thể lộ cấu trúc class). */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Nội dung JSON không đọc được");
        return ResponseEntity.badRequest().body(problem);
    }

    // ---------- 401 / 403: lỗi bảo mật ----------
    // Đến từ tầng filter (qua ProblemAuthenticationEntryPoint / ProblemAccessDeniedHandler)
    // hoặc ném từ trong service. Phải bắt riêng, nếu không bộ bắt Exception cuối file biến chúng thành 500.

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập hoặc phiên đăng nhập đã hết hạn");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này");
    }

    // ---------- 409: xung đột ----------

    /** Service đã kiểm và biết rõ lý do xung đột: dùng thông báo của nó. */
    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ProblemDetail> handleConflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Lưới an toàn cho ràng buộc DB:

    /**
     * Service nên kiểm tra trước để trả thông báo rõ ràng (D24). Nếu vẫn lọt tới đây
     * (ví dụ hai request đăng ký cùng email chạy song song), trả 409 với thông báo chung;
     * tên ràng buộc và câu SQL chỉ ghi vào log.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Vi phạm ràng buộc DB không được service kiểm trước: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Dữ liệu xung đột với dữ liệu đã có");
    }

    // ---------- 500: mọi lỗi còn lại ----------

    /** Lỗi không lường trước: stack trace đầy đủ vào log, client chỉ nhận thông báo chung (TC73). */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Lỗi không lường trước", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi ở máy chủ, vui lòng thử lại sau");
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
