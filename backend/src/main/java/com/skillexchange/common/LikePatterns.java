package com.skillexchange.common;

/**
 * Dựng mẫu cho {@code ILIKE ... ESCAPE '\'} từ chuỗi người dùng gõ (D37).
 *
 * Trong LIKE, {@code %} là "chuỗi bất kỳ" và {@code _} là "một ký tự bất kỳ". Nếu đưa thẳng q = "%" vào
 * mẫu thì câu tìm khớp mọi người. Ở đây mỗi ký tự đặc biệt được đặt sau một dấu {@code \} để thành
 * ký tự thường. Câu SQL dùng mẫu này phải có {@code ESCAPE '\'}.
 */
public final class LikePatterns {

    private static final String ESCAPE = "\\";

    private LikePatterns() {
    }

    /** Mẫu "chứa chuỗi raw ở bất kỳ đâu": %raw đã escape%. */
    public static String contains(String raw) {
        return "%" + escape(raw) + "%";
    }

    private static String escape(String raw) {
        // Escape chính dấu \ TRƯỚC. Làm sau thì các dấu \ vừa thêm cho % và _ sẽ bị nhân đôi lần nữa.
        return raw.replace(ESCAPE, ESCAPE + ESCAPE)
                .replace("%", ESCAPE + "%")
                .replace("_", ESCAPE + "_");
    }
}
