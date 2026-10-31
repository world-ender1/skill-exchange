package com.skillexchange.skill;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Danh mục kỹ năng (TK 4.4, D40). Quyền: User – cần đăng nhập.
 * Không có permitAll ở đâu cả: Spring Security mặc định chặn mọi request chưa đăng nhập.
 */
@RestController
@RequestMapping("/api")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping("/skill-categories")
    public List<SkillCategoryResponse> listCategories() {
        return skillService.listCategories();
    }

    // required = false: thiếu tham số thì Spring truyền null thay vì báo lỗi 400.
    @GetMapping("/skills")
    public List<SkillResponse> listSkills(@RequestParam(required = false) Long categoryId,
                                          @RequestParam(required = false) String q) {
        return skillService.listActiveSkills(categoryId, q);
    }
}
