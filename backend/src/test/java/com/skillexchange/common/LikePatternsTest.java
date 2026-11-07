package com.skillexchange.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * D37: ký tự \, %, _ của người dùng phải thành ký tự thường trong mẫu ILIKE ... ESCAPE '\'.
 * Chuỗi Java viết "\\" cho MỘT dấu \ thật.
 */
class LikePatternsTest {

    @Test
    @DisplayName("D37 chữ thường giữ nguyên, bọc % hai đầu")
    void d37_plainTextIsWrapped() {
        assertThat(LikePatterns.contains("An")).isEqualTo("%An%");
    }

    @Test
    @DisplayName("D37 escape % và _ bằng dấu gạch chéo ngược")
    void d37_wildcardsAreEscaped() {
        // %  →  %\%%
        assertThat(LikePatterns.contains("%")).isEqualTo("%\\%%");
        // a_b  →  %a\_b%
        assertThat(LikePatterns.contains("a_b")).isEqualTo("%a\\_b%");
    }

    @Test
    @DisplayName("D37 escape dấu gạch chéo ngược trước, mỗi ký tự gốc chỉ được escape một lần")
    void d37_backslashEscapedFirst() {
        // \  →  %\\%
        assertThat(LikePatterns.contains("\\")).isEqualTo("%\\\\%");
        // \%  →  %\\\%%  (\ thành \\, % thành \%)
        assertThat(LikePatterns.contains("\\%")).isEqualTo("%\\\\\\%%");
    }
}
