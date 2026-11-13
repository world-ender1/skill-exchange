package com.skillexchange.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.skillexchange.skill.SkillCategoryResponse;

import jakarta.validation.Valid;

/**
 * 8 API quản trị danh mục kỹ năng (TK 4.9, D73). Danh sách nhóm dùng GET /api/skill-categories có sẵn.
 *
 * Quyền: luật hasRole("ADMIN") cho /api/admin/** ở SecurityConfig chặn từ tầng filter, trước controller,
 * nên sinh viên luôn nhận 403 kể cả khi body sai. Không có API xóa kỹ năng hay nhóm: DELETE trả 405.
 * "abc" ở vị trí {id} → 400 qua handleTypeMismatch (D38).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminSkillController {

    private final AdminSkillService adminSkillService;

    public AdminSkillController(AdminSkillService adminSkillService) {
        this.adminSkillService = adminSkillService;
    }

    // ---------- Nhóm kỹ năng ----------

    @PostMapping("/skill-categories")
    @ResponseStatus(HttpStatus.CREATED)
    public SkillCategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        return adminSkillService.createCategory(request);
    }

    @PutMapping("/skill-categories/{id}")
    public SkillCategoryResponse renameCategory(@PathVariable long id, @Valid @RequestBody CategoryRequest request) {
        return adminSkillService.renameCategory(id, request);
    }

    // ---------- Kỹ năng ----------

    @GetMapping("/skills")
    public List<AdminSkillResponse> listSkills() {
        return adminSkillService.listSkills();
    }

    @PostMapping("/skills")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminSkillResponse createSkill(@Valid @RequestBody CreateSkillRequest request) {
        return adminSkillService.createSkill(request);
    }

    @PutMapping("/skills/{id}")
    public AdminSkillResponse updateSkill(@PathVariable long id, @Valid @RequestBody UpdateSkillRequest request) {
        return adminSkillService.updateSkill(id, request);
    }

    // ---------- Quan hệ liên quan ----------

    @GetMapping("/skill-relations")
    public List<SkillRelationResponse> listRelations() {
        return adminSkillService.listRelations();
    }

    @PostMapping("/skill-relations")
    @ResponseStatus(HttpStatus.CREATED)
    public SkillRelationResponse createRelation(@Valid @RequestBody SkillRelationRequest request) {
        return adminSkillService.createRelation(request);
    }

    /** Hai mã theo thứ tự bất kỳ (D73). */
    @DeleteMapping("/skill-relations/{aId}/{bId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRelation(@PathVariable long aId, @PathVariable long bId) {
        adminSkillService.deleteRelation(aId, bId);
    }
}
