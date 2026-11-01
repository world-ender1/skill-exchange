package com.skillexchange.user;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillexchange.common.FieldValidationException;
import com.skillexchange.skill.Skill;
import com.skillexchange.skill.SkillRepository;

/**
 * Kỹ năng dạy và muốn học của tôi (TK 4.3, D19, D20, D21, D28, D41).
 * Đây là dữ liệu đầu vào của gợi ý ghép cặp tuần 4, nên mọi luật được kiểm ở backend.
 */
@Service
public class SkillProfileService {

    private final UserTeachSkillRepository teachRepository;
    private final UserLearnSkillRepository learnRepository;
    private final SkillRepository skillRepository;

    public SkillProfileService(UserTeachSkillRepository teachRepository,
                               UserLearnSkillRepository learnRepository,
                               SkillRepository skillRepository) {
        this.teachRepository = teachRepository;
        this.learnRepository = learnRepository;
        this.skillRepository = skillRepository;
    }

    @Transactional(readOnly = true)
    public List<TeachSkillResponse> getTeachSkills(Long userId) {
        return teachRepository.findResponsesByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<LearnSkillResponse> getLearnSkills(Long userId) {
        return learnRepository.findResponsesByUserId(userId);
    }

    /** Thay danh sách dạy: kiểm hết, rồi xóa và chèn trong một transaction (D30). */
    @Transactional
    public List<TeachSkillResponse> replaceTeachSkills(Long userId, List<TeachSkillRequest> items) {
        checkSkills(items.stream().map(TeachSkillRequest::skillId).toList(),
                Set.copyOf(teachRepository.findSkillIdsByUserId(userId)),
                Set.copyOf(learnRepository.findSkillIdsByUserId(userId)),
                "muốn học");

        teachRepository.deleteAllByUserId(userId);
        teachRepository.saveAll(items.stream()
                .map(item -> new UserTeachSkill(userId, item.skillId(), item.level().shortValue()))
                .toList());
        return teachRepository.findResponsesByUserId(userId);
    }

    /** Thay danh sách muốn học: như replaceTeachSkills, với hai danh sách đổi vai. */
    @Transactional
    public List<LearnSkillResponse> replaceLearnSkills(Long userId, List<LearnSkillRequest> items) {
        checkSkills(items.stream().map(LearnSkillRequest::skillId).toList(),
                Set.copyOf(learnRepository.findSkillIdsByUserId(userId)),
                Set.copyOf(teachRepository.findSkillIdsByUserId(userId)),
                "dạy");

        learnRepository.deleteAllByUserId(userId);
        learnRepository.saveAll(items.stream()
                .map(item -> new UserLearnSkill(userId, item.skillId(),
                        item.targetLevel() == null ? null : item.targetLevel().shortValue()))
                .toList());
        return learnRepository.findResponsesByUserId(userId);
    }

    /**
     * Kiểm các skillId gửi lên, theo thứ tự đã chốt ở bước 9; gặp lỗi đầu tiên thì dừng (chưa ghi gì vào DB).
     *
     * @param requested     skillId theo thứ tự trong request
     * @param alreadyInList kỹ năng đang có trong CHÍNH danh sách này (được giữ dù đã bị ẩn, D20)
     * @param inOtherList   kỹ năng đang có trong danh sách kia (dạy ↔ học, D19)
     * @param otherListName tên danh sách kia, dùng trong thông báo lỗi
     */
    private void checkSkills(List<Long> requested, Set<Long> alreadyInList, Set<Long> inOtherList,
                             String otherListName) {
        // 1. Trùng trong cùng request (D21).
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < requested.size(); i++) {
            if (!seen.add(requested.get(i))) {
                throw skillError(i, "Kỹ năng này bị gửi trùng");
            }
        }

        // Một câu SELECT cho mọi kỹ năng, thay vì mỗi kỹ năng một câu.
        Map<Long, Skill> skills = skillRepository.findAllById(requested).stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity()));

        // 2. Không có trong danh mục.
        for (int i = 0; i < requested.size(); i++) {
            if (!skills.containsKey(requested.get(i))) {
                throw skillError(i, "Kỹ năng không có trong danh mục");
            }
        }

        // 3. Đã bị ẩn và chưa có trong danh sách này: không thêm mới được (D20).
        for (int i = 0; i < requested.size(); i++) {
            Long skillId = requested.get(i);
            if (!skills.get(skillId).isActive() && !alreadyInList.contains(skillId)) {
                throw skillError(i, "Kỹ năng đã ngừng sử dụng, không thêm mới được");
            }
        }

        // 4. Đang nằm trong danh sách kia (D19).
        for (int i = 0; i < requested.size(); i++) {
            if (inOtherList.contains(requested.get(i))) {
                throw skillError(i, "Kỹ năng này đang nằm trong danh sách " + otherListName
                        + ", hãy bỏ nó khỏi danh sách đó trước");
            }
        }
    }

    private static FieldValidationException skillError(int index, String message) {
        return new FieldValidationException("[" + index + "].skillId", message);
    }
}
