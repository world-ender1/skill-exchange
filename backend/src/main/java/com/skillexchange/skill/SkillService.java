package com.skillexchange.skill;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkillService {

    private final SkillCategoryRepository categoryRepository;
    private final SkillRepository skillRepository;

    // Spring tự truyền các Repository vào constructor (constructor injection).
    public SkillService(SkillCategoryRepository categoryRepository, SkillRepository skillRepository) {
        this.categoryRepository = categoryRepository;
        this.skillRepository = skillRepository;
    }

    @Transactional(readOnly = true)
    public List<SkillCategoryResponse> listCategories() {
        return categoryRepository.findAllByOrderByNameAscIdAsc().stream()
                .map(SkillCategoryResponse::from)
                .toList();
    }

    /**
     * @param categoryId null = mọi nhóm; id không tồn tại thì trả danh sách rỗng (D40)
     * @param q          chuỗi con trong tên, không phân biệt hoa thường; null/rỗng = không lọc
     */
    @Transactional(readOnly = true)
    public List<SkillResponse> listActiveSkills(Long categoryId, String q) {
        return skillRepository.findActive(categoryId, toLikePattern(q)).stream()
                .map(SkillResponse::from)
                .toList();
    }

    /**
     * Biến chuỗi tìm kiếm thành mẫu LIKE "%...%".
     * Ký tự đặc biệt của LIKE (\, %, _) được thoát để người dùng gõ "_" không khớp mọi tên.
     */
    private static String toLikePattern(String q) {
        if (q == null || q.isBlank()) {
            return "%";
        }
        String escaped = q.strip()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
