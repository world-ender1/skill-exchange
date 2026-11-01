package com.skillexchange.common;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Một đồng hồ dùng chung cho mọi mốc thời gian của token và xác thực (D18).
 * Service hỏi giờ qua clock.instant() thay vì Instant.now(), nên test thay được bằng đồng hồ giả.
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
