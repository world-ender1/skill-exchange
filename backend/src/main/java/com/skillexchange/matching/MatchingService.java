package com.skillexchange.matching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillexchange.common.ForbiddenException;
import com.skillexchange.matching.MatchingDataRepository.UserRow;
import com.skillexchange.user.AvailabilitySlotResponse;

/**
 * Vỏ của lõi matching (D34 phần 3): nạp dữ liệu thô → tính Reputation → dựng MatchProfile
 * → gọi MatchingEngine → đổi sang DTO và thêm ratingAverage, reviewCount, hasPendingRequest.
 * Quyền Active đã kiểm ở tầng filter (D44), không kiểm lại ở đây.
 */
@Service
public class MatchingService {

    private final MatchingDataRepository data;

    public MatchingService(MatchingDataRepository data) {
        this.data = data;
    }

    /**
     * readOnly = true: mọi câu SQL chạy trong cùng một giao dịch chỉ đọc. Không ghi gì nên không cần
     * khóa hay flush; các câu nạp dùng chung một kết nối thay vì mỗi câu mượn một kết nối riêng.
     */
    @Transactional(readOnly = true)
    public MatchListResponse findMatches(long callerId, MatchType type, int limit) {
        List<UserRow> users = data.findActiveStudents();
        Map<Long, List<TeachItem>> teach = data.findTeachSkills();
        Map<Long, List<LearnItem>> learn = data.findLearnSkills();
        Map<Long, Set<Slot>> slots = data.findSlots();
        Map<Long, List<Integer>> ratings = data.findRatingsReceived();
        Map<Long, List<SessionOutcome>> sessions = data.findSessionOutcomes();

        Map<Long, UserRow> usersById = new HashMap<>();
        List<MatchProfile> profiles = new ArrayList<>();
        MatchProfile caller = null;
        for (UserRow user : users) {
            long id = user.id();
            // Người chưa có dòng nào trong một bảng = chưa khai báo: danh sách/tập rỗng, không phải null.
            double reputation = ReputationCalculator.compute(
                    ratings.getOrDefault(id, List.of()), sessions.getOrDefault(id, List.of()));
            MatchProfile profile = new MatchProfile(id, user.status(), user.role(),
                    learn.getOrDefault(id, List.of()), teach.getOrDefault(id, List.of()),
                    slots.getOrDefault(id, Set.of()), user.learningMode(), reputation);
            usersById.put(id, user);
            profiles.add(profile);
            if (id == callerId) {
                caller = profile;
            }
        }

        if (caller == null) {
            // Filter đã cho qua nên chỉ xảy ra khi tài khoản bị khóa đúng lúc đang gọi.
            throw new ForbiddenException("Chỉ sinh viên đã xác thực mới dùng được gợi ý ghép cặp");
        }
        if (caller.learn().isEmpty()) {
            return MatchListResponse.needsLearnSkills(type);
        }

        // Danh sách ứng viên có cả người gọi: HardFilter tự loại (điều kiện v ≠ u).
        MatchingEngine engine = new MatchingEngine(new SkillSimilarity(data.findSkillRelations()), MatchingWeights.DEFAULT);
        MatchingResult result = engine.match(caller, profiles);
        List<MatchResult> group = type == MatchType.TWO_WAY ? result.twoWay() : result.oneWay();

        Set<Long> pendingReceivers = data.findPendingReceivers(callerId);
        List<MatchItemResponse> items = group.stream()
                .limit(limit)
                .map(match -> toItem(match, usersById.get(match.userId()),
                        ratings.getOrDefault(match.userId(), List.of()),
                        pendingReceivers.contains(match.userId())))
                .toList();
        return new MatchListResponse(type, false, group.size(), items);
    }

    private static MatchItemResponse toItem(MatchResult match, UserRow user, List<Integer> ratings, boolean pending) {
        return new MatchItemResponse(
                match.userId(),
                user.fullName(),
                match.type(),
                Rounding.round3(match.score()),
                ScoreResponse.from(match.forward()),
                match.backward() == null ? null : ScoreResponse.from(match.backward()),
                user.learningMode(),
                ratingAverage(ratings),
                ratings.size(),
                pending,
                match.youLearn().stream().map(SkillMatchResponse::youLearn).toList(),
                match.theyLearn() == null ? null : match.theyLearn().stream().map(SkillMatchResponse::theyLearn).toList(),
                match.commonSlots().stream()
                        .map(slot -> new AvailabilitySlotResponse((short) slot.dayOfWeek(), slot.period()))
                        .toList());
    }

    /** Trung bình số sao nhận được; null khi chưa có đánh giá (giao diện hiện nhãn "Mới", TK 4.5). */
    private static Double ratingAverage(List<Integer> ratings) {
        if (ratings.isEmpty()) {
            return null;
        }
        double average = ratings.stream().mapToInt(Integer::intValue).average().orElseThrow();
        return Rounding.round3(average);
    }
}
