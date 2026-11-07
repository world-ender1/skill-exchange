package com.skillexchange.user;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hồ sơ người khác và tìm kiếm (TK 4.3). Quyền Active: SecurityConfig đã chặn người không phải
 * sinh viên ACTIVE (403) trước khi tới đây, kể cả khi tham số sai như /users/abc (D44, TC174).
 */
@RestController
@RequestMapping("/api/users")
public class PublicUserController {

    private final PublicUserService publicUserService;

    public PublicUserController(PublicUserService publicUserService) {
        this.publicUserService = publicUserService;
    }

    /**
     * Spring ưu tiên đường dẫn cố định hơn đường dẫn có biến, nên /search không bị hiểu là /{id} với id = "search".
     * Hai tham số đều không bắt buộc riêng lẻ; luật "ít nhất một" nằm ở service (D45a cần cắt q trước).
     */
    @GetMapping("/search")
    public List<UserSearchResultResponse> search(@AuthenticationPrincipal Jwt jwt,
                                                 @RequestParam(required = false) String q,
                                                 @RequestParam(required = false) Long skillId) {
        return publicUserService.search(Long.parseLong(jwt.getSubject()), q, skillId);
    }

    /** @PathVariable: lấy {id} trong đường dẫn. "abc" hay số vượt long → 400 qua handleTypeMismatch (D38). */
    @GetMapping("/{id}")
    public PublicProfileResponse getProfile(@PathVariable long id) {
        return publicUserService.getProfile(id);
    }
}
