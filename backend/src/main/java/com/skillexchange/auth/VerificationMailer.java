package com.skillexchange.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Gửi thư có link xác thực (D5, D11, D27). */
@Component
public class VerificationMailer {

    private static final Logger log = LoggerFactory.getLogger(VerificationMailer.class);

    private final JavaMailSender mailSender;
    private final String frontendUrl;
    private final String from;

    public VerificationMailer(JavaMailSender mailSender,
                              @Value("${app.frontend-url}") String frontendUrl,
                              @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
        this.from = from;
    }

    /**
     * AFTER_COMMIT: Spring chỉ gọi method này khi transaction của AuthService đã commit.
     * Vì vậy không bao giờ gửi link cho một tài khoản/token chưa thật sự được lưu (D27).
     * Gửi lỗi thì chỉ ghi log: transaction đã xong, API vẫn trả thành công; người dùng bấm "gửi lại link".
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationMailRequested(VerificationMailRequested event) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(event.email());
        mail.setSubject("Xác thực email Skill Exchange");
        mail.setText("""
                Chào bạn,

                Bấm vào link dưới đây để xác thực email và dùng đầy đủ Skill Exchange:
                %s/verify-email?token=%s

                Link có hiệu lực trong 24 giờ và chỉ dùng được một lần.
                Nếu bạn không đăng ký, hãy bỏ qua thư này.
                """.formatted(frontendUrl, event.rawToken()));
        try {
            mailSender.send(mail);
        } catch (MailException e) {
            // Không ghi token vào log (log có thể bị người khác đọc).
            log.error("Không gửi được thư xác thực tới {}", event.email(), e);
        }
    }
}
