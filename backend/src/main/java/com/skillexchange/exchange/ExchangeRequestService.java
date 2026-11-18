package com.skillexchange.exchange;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillexchange.common.ConflictException;
import com.skillexchange.common.FieldValidationException;
import com.skillexchange.common.InvalidRequestException;
import com.skillexchange.common.NotFoundException;
import com.skillexchange.matching.MatchingDataRepository;
import com.skillexchange.matching.SkillSimilarity;
import com.skillexchange.user.Role;
import com.skillexchange.user.User;
import com.skillexchange.user.UserLearnSkillRepository;
import com.skillexchange.user.UserRepository;
import com.skillexchange.user.UserStatus;
import com.skillexchange.user.UserTeachSkillRepository;

/**
 * Yêu cầu trao đổi (FR-05, TK 4.6). Bước 5: tạo yêu cầu (D89). Hộp yêu cầu và phản hồi thêm ở bước 6.
 *
 * Quyền Active của người gọi đã kiểm ở tầng filter (D44, D87). senderId lấy từ vé, không bao giờ từ body.
 */
@Service
public class ExchangeRequestService {

    private final ExchangeRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final UserLearnSkillRepository learnRepository;
    private final UserTeachSkillRepository teachRepository;
    private final MatchingDataRepository matchingDataRepository;
    private final Clock clock;

    public ExchangeRequestService(ExchangeRequestRepository requestRepository,
                                  UserRepository userRepository,
                                  UserLearnSkillRepository learnRepository,
                                  UserTeachSkillRepository teachRepository,
                                  MatchingDataRepository matchingDataRepository,
                                  Clock clock) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.learnRepository = learnRepository;
        this.teachRepository = teachRepository;
        this.matchingDataRepository = matchingDataRepository;
        this.clock = clock;
    }

    /**
     * POST /api/requests. Bước (1) của D89 (Bean Validation) đã chạy trước khi tới đây; các bước còn lại
     * chạy đúng thứ tự, gặp lỗi đầu tiên thì dừng.
     */
    @Transactional
    public RequestItem create(long senderId, CreateRequestRequest request) {
        Instant now = clock.instant();

        checkReceiverSkillMatchesType(request);                                   // (2)
        if (!request.proposedStartAt().isAfter(now)) {                            // (3) bằng hiện tại cũng sai
            throw new FieldValidationException("proposedStartAt", "Thời điểm phải ở tương lai");
        }
        if (request.receiverId() == senderId) {                                   // (4)
            throw new InvalidRequestException("Không thể gửi yêu cầu cho chính mình");
        }
        long receiverId = findVisibleReceiver(request.receiverId()).getId();      // (5)
        checkSkillsAreInLearnLists(senderId, receiverId, request);                // (6)
        checkSkillsAreTaught(senderId, receiverId, request);                      // (7)
        if (requestRepository.existsBySenderIdAndReceiverIdAndStatus(             // (8)
                senderId, receiverId, RequestStatus.PENDING)) {
            // Hai request song song có thể cùng qua kiểm này; khi đó chỉ mục uq_requests_one_pending của DB
            // chặn câu INSERT thứ hai và lưới D24 trả 409 (Chốt ở bước 5).
            throw new ConflictException("Bạn đã có một yêu cầu đang chờ gửi tới người này");
        }

        ExchangeRequest saved = requestRepository.save(ExchangeRequest.create(
                senderId, receiverId, request.type(),
                request.senderLearnSkillId(), request.receiverLearnSkillId(),
                request.proposedStartAt(), request.durationMinutes().shortValue(), request.mode(),
                request.locationOrLink(), request.message(), now));

        // Đọc lại kèm tên người, tên kỹ năng bằng cùng câu truy vấn mà hộp yêu cầu sẽ dùng (bước 6).
        RequestRow row = requestRepository.findRowById(saved.getId()).orElseThrow();
        return RequestItem.from(row, senderId);
    }

    /** (2) TWO_WAY phải có kỹ năng người nhận muốn học; ONE_WAY thì không được có. */
    private static void checkReceiverSkillMatchesType(CreateRequestRequest request) {
        boolean hasReceiverSkill = request.receiverLearnSkillId() != null;
        if (request.type() == RequestType.TWO_WAY && !hasReceiverSkill) {
            throw new FieldValidationException("receiverLearnSkillId",
                    "Yêu cầu hai chiều cần kỹ năng người nhận muốn học");
        }
        if (request.type() == RequestType.ONE_WAY && hasReceiverSkill) {
            throw new FieldValidationException("receiverLearnSkillId",
                    "Yêu cầu một chiều không có kỹ năng người nhận muốn học");
        }
    }

    /**
     * (5) Người nhận phải là sinh viên ACTIVE. Không tồn tại, chưa xác thực, bị khóa, admin: cùng một 404,
     * cùng câu với GET /api/users/{id} (TK 4.3), để người gửi không dò được trạng thái tài khoản của ai.
     */
    private User findVisibleReceiver(long receiverId) {
        return userRepository.findById(receiverId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE && user.getRole() == Role.STUDENT)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
    }

    /** (6) Mỗi bên chỉ được đề nghị học kỹ năng có trong danh sách muốn học của chính bên đó (R36). */
    private void checkSkillsAreInLearnLists(long senderId, long receiverId, CreateRequestRequest request) {
        if (!learnRepository.findSkillIdsByUserId(senderId).contains(request.senderLearnSkillId())) {
            throw new FieldValidationException("senderLearnSkillId",
                    "Kỹ năng này không có trong danh sách muốn học của bạn");
        }
        if (request.type() == RequestType.TWO_WAY
                && !learnRepository.findSkillIdsByUserId(receiverId).contains(request.receiverLearnSkillId())) {
            throw new FieldValidationException("receiverLearnSkillId",
                    "Kỹ năng này không có trong danh sách muốn học của người nhận");
        }
    }

    /**
     * (7) Bên dạy phải dạy đúng kỹ năng đó hoặc một kỹ năng liên quan: Sim > 0 (spec 4.1).
     * Sim tính bằng SkillSimilarity của lõi matching, cùng công thức với gợi ý ghép cặp, nên người dùng gửi được
     * yêu cầu cho mọi ứng viên mà /api/matches gợi ý ở chiều tương ứng.
     */
    private void checkSkillsAreTaught(long senderId, long receiverId, CreateRequestRequest request) {
        SkillSimilarity similarity = new SkillSimilarity(matchingDataRepository.findSkillRelations());
        if (!teachesRelated(similarity, request.senderLearnSkillId(), teachRepository.findSkillIdsByUserId(receiverId))) {
            throw new FieldValidationException("senderLearnSkillId",
                    "Người nhận không dạy kỹ năng này hay kỹ năng liên quan");
        }
        if (request.type() == RequestType.TWO_WAY
                && !teachesRelated(similarity, request.receiverLearnSkillId(), teachRepository.findSkillIdsByUserId(senderId))) {
            throw new FieldValidationException("receiverLearnSkillId",
                    "Bạn không dạy kỹ năng này hay kỹ năng liên quan");
        }
    }

    private static boolean teachesRelated(SkillSimilarity similarity, long wantSkillId, List<Long> teachSkillIds) {
        return teachSkillIds.stream().anyMatch(teachSkillId -> similarity.sim(wantSkillId, teachSkillId) > 0);
    }
}
