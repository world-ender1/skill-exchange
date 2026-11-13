package com.skillexchange.admin;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillexchange.common.ConflictException;
import com.skillexchange.common.InvalidRequestException;
import com.skillexchange.common.NotFoundException;
import com.skillexchange.skill.Skill;
import com.skillexchange.skill.SkillCategory;
import com.skillexchange.skill.SkillCategoryRepository;
import com.skillexchange.skill.SkillCategoryResponse;
import com.skillexchange.skill.SkillRepository;

/**
 * Quản trị danh mục kỹ năng A01 (TK 4.9, D73–D75). Quyền ADMIN đã kiểm ở SecurityConfig.
 *
 * Thứ tự kiểm (D74): 400 dữ liệu sai → 404 không tìm thấy → 409 xung đột.
 * Lỗi 400 theo trường (tên rỗng, thiếu mã...) đã do @Valid chặn trước khi vào đây.
 * Trùng tên vẫn có thể lọt khi hai request chạy cùng lúc: UNIQUE của DB chặn,
 * GlobalExceptionHandler đổi DataIntegrityViolationException thành 409.
 */
@Service
public class AdminSkillService {

    private final SkillRepository skillRepository;
    private final SkillCategoryRepository categoryRepository;
    private final SkillRelationRepository relationRepository;

    public AdminSkillService(SkillRepository skillRepository,
                             SkillCategoryRepository categoryRepository,
                             SkillRelationRepository relationRepository) {
        this.skillRepository = skillRepository;
        this.categoryRepository = categoryRepository;
        this.relationRepository = relationRepository;
    }

    // ---------- Nhóm kỹ năng ----------

    @Transactional
    public SkillCategoryResponse createCategory(CategoryRequest request) {
        checkCategoryNameFree(request.name(), null);
        SkillCategory category = categoryRepository.save(SkillCategory.create(request.name()));
        return SkillCategoryResponse.from(category);
    }

    @Transactional
    public SkillCategoryResponse renameCategory(long id, CategoryRequest request) {
        SkillCategory category = findCategory(id);
        checkCategoryNameFree(request.name(), id);
        category.rename(request.name());
        return SkillCategoryResponse.from(category);
    }

    // ---------- Kỹ năng ----------

    @Transactional(readOnly = true)
    public List<AdminSkillResponse> listSkills() {
        return skillRepository.findAllWithCategory().stream()
                .map(AdminSkillResponse::from)
                .toList();
    }

    @Transactional
    public AdminSkillResponse createSkill(CreateSkillRequest request) {
        SkillCategory category = findCategory(request.categoryId());
        checkSkillNameFree(request.name(), null);
        Skill skill = skillRepository.save(Skill.create(category, request.name()));
        return AdminSkillResponse.from(skill);
    }

    @Transactional
    public AdminSkillResponse updateSkill(long id, UpdateSkillRequest request) {
        Skill skill = findSkill(id);
        SkillCategory category = findCategory(request.categoryId());
        checkSkillNameFree(request.name(), id);

        // R42: đổi nhóm làm cặp liên quan thành "khác nhóm", trái luật của quan hệ → bắt gỡ quan hệ trước.
        // Đổi tên, ẩn, hiện thì không ảnh hưởng quan hệ nên không chặn.
        boolean categoryChanged = !category.getId().equals(skill.getCategory().getId());
        if (categoryChanged && relationRepository.existsForSkill(id)) {
            throw new ConflictException("Kỹ năng đang có quan hệ liên quan, hãy gỡ quan hệ trước khi đổi nhóm");
        }

        skill.update(request.name(), category, request.isActive());
        return AdminSkillResponse.from(skill);
    }

    // ---------- Quan hệ liên quan ----------

    @Transactional(readOnly = true)
    public List<SkillRelationResponse> listRelations() {
        return relationRepository.findAll();
    }

    @Transactional
    public SkillRelationResponse createRelation(SkillRelationRequest request) {
        long firstId = request.skillAId();
        long secondId = request.skillBId();
        if (firstId == secondId) {
            throw new InvalidRequestException("Hai kỹ năng phải khác nhau");
        }
        Skill first = findSkill(firstId);
        Skill second = findSkill(secondId);
        // Là lỗi 400 nhưng phải xét sau 404: cần đọc được hai kỹ năng mới biết nhóm của chúng.
        if (!first.getCategory().getId().equals(second.getCategory().getId())) {
            throw new InvalidRequestException("Hai kỹ năng phải cùng nhóm");
        }
        if (relationRepository.exists(firstId, secondId)) {
            throw new ConflictException("Hai kỹ năng đã được đánh dấu liên quan");
        }
        // D75: kỹ năng bị ẩn vẫn được tạo quan hệ, nên không xét isActive.

        relationRepository.insert(firstId, secondId);
        Skill low = firstId < secondId ? first : second;
        Skill high = firstId < secondId ? second : first;
        return new SkillRelationResponse(low.getId(), low.getName(), high.getId(), high.getName());
    }

    @Transactional
    public void deleteRelation(long firstId, long secondId) {
        if (relationRepository.delete(firstId, secondId) == 0) {
            throw new NotFoundException("Không tìm thấy quan hệ liên quan");
        }
    }

    // ---------- Dùng chung ----------

    private SkillCategory findCategory(long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhóm kỹ năng"));
    }

    private Skill findSkill(long id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy kỹ năng"));
    }

    /** exceptId: id của chính nhóm đang đổi tên (null khi thêm mới), để đổi "Java" → "JAVA" vẫn được. */
    private void checkCategoryNameFree(String name, Long exceptId) {
        if (categoryRepository.countSameName(name, exceptId) > 0) {
            throw new ConflictException("Tên nhóm kỹ năng đã tồn tại");
        }
    }

    private void checkSkillNameFree(String name, Long exceptId) {
        if (skillRepository.countSameName(name, exceptId) > 0) {
            throw new ConflictException("Tên kỹ năng đã tồn tại");
        }
    }
}
