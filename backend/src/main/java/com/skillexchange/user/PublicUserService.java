package com.skillexchange.user;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillexchange.common.FieldValidationException;
import com.skillexchange.common.InvalidRequestException;
import com.skillexchange.common.LikePatterns;
import com.skillexchange.common.NotFoundException;

/**
 * Hồ sơ công khai và tìm kiếm (TK 4.3, FR-10; D37, D45, D46).
 * Quyền Active của người GỌI đã kiểm ở tầng filter (D44). Ở đây chỉ lọc người ĐƯỢC XEM / ĐƯỢC TÌM:
 * phải là sinh viên ACTIVE.
 */
@Service
public class PublicUserService {

    /** D45(b): bằng độ dài tối đa của full_name. */
    static final int MAX_QUERY_LENGTH = 100;

    private final UserRepository userRepository;
    private final UserTeachSkillRepository teachRepository;
    private final UserLearnSkillRepository learnRepository;
    private final AvailabilityService availabilityService;

    public PublicUserService(UserRepository userRepository,
                             UserTeachSkillRepository teachRepository,
                             UserLearnSkillRepository learnRepository,
                             AvailabilityService availabilityService) {
        this.userRepository = userRepository;
        this.teachRepository = teachRepository;
        this.learnRepository = learnRepository;
        this.availabilityService = availabilityService;
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse getProfile(long id) {
        // Không tồn tại, chưa xác thực, bị khóa, admin: cùng một 404, cùng một câu (TK 4.3, TC160).
        // Trả 403 hay câu khác nhau thì người ngoài dò được "id này có tài khoản nhưng bị khóa".
        User user = userRepository.findById(id)
                .filter(PublicUserService::isVisible)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        List<PublicSkillResponse> teachSkills = teachRepository.findRowsByUserIds(List.of(id)).stream()
                .map(TeachSkillRow::toResponse)
                .toList();
        List<PublicLearnSkillResponse> learnSkills = learnRepository.findResponsesByUserId(id).stream()
                .map(PublicLearnSkillResponse::from)
                .toList();
        return new PublicProfileResponse(user.getId(), user.getFullName(), user.getBio(), user.getLearningMode(),
                teachSkills, learnSkills, availabilityService.getAvailability(id));
    }

    /**
     * @param q       chuỗi tên người gọi gõ, có thể null
     * @param skillId kỹ năng dạy cần tìm, có thể null; không tồn tại thì kết quả rỗng (D45c)
     */
    @Transactional(readOnly = true)
    public List<UserSearchResultResponse> search(long callerId, String q, Long skillId) {
        // D45(a): cắt khoảng trắng; còn rỗng thì coi như không truyền.
        String name = q == null || q.isBlank() ? null : q.strip();
        if (name == null && skillId == null) {
            throw new InvalidRequestException("Cần ít nhất một tham số: q hoặc skillId");
        }
        if (name != null && name.length() > MAX_QUERY_LENGTH) {
            throw new FieldValidationException("q", "q dài tối đa " + MAX_QUERY_LENGTH + " ký tự");
        }

        String namePattern = name == null ? null : LikePatterns.contains(name);
        List<User> users = userRepository.searchActiveStudents(callerId, namePattern, skillId);
        if (users.isEmpty()) {
            return List.of();
        }

        // Một câu cho kỹ năng của mọi người tìm được, rồi gom theo userId.
        Map<Long, List<PublicSkillResponse>> teachByUser = teachRepository
                .findRowsByUserIds(users.stream().map(User::getId).toList()).stream()
                .collect(Collectors.groupingBy(TeachSkillRow::userId,
                        Collectors.mapping(TeachSkillRow::toResponse, Collectors.toList())));

        return users.stream()
                .map(user -> new UserSearchResultResponse(user.getId(), user.getFullName(), user.getLearningMode(),
                        teachByUser.getOrDefault(user.getId(), List.of())))
                .toList();
    }

    /** Ai được người khác xem và tìm thấy (TK 4.3): chỉ sinh viên đã xác thực. */
    private static boolean isVisible(User user) {
        return user.getStatus() == UserStatus.ACTIVE && user.getRole() == Role.STUDENT;
    }
}
