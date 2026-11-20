package com.skillexchange.exchange;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillexchange.common.ConflictException;
import com.skillexchange.common.FieldValidationException;
import com.skillexchange.common.ForbiddenException;
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
 * Yêu cầu trao đổi (FR-05, TK 4.6). Bước 5: tạo yêu cầu (D89). Bước 6: hộp yêu cầu, chi tiết, chấp nhận,
 * từ chối, hủy (D90, D91).
 *
 * Quyền Active của người gọi đã kiểm ở tầng filter (D44, D87). Người gọi (viewerId, senderId) lấy từ vé,
 * không bao giờ từ body. Các API theo {id} kiểm theo thứ tự D87: 404 → 403 không tham gia → 403 sai vai trò
 * → 409; lỗi 400 ({id} không phải số, body sai) Spring đã trả trước khi gọi tới đây.
 */
@Service
public class ExchangeRequestService {

    // Câu detail của bảng 2.1 (Quyet_dinh_tuan_6.md).
    private static final String REQUEST_NOT_FOUND = "Không tìm thấy yêu cầu";
    private static final String NOT_PARTICIPANT = "Bạn không tham gia yêu cầu này";
    private static final String ONLY_RECEIVER = "Chỉ người nhận được chấp nhận hoặc từ chối yêu cầu";
    private static final String ONLY_SENDER = "Chỉ người gửi được hủy yêu cầu";
    private static final String NOT_PENDING = "Yêu cầu không còn ở trạng thái chờ";
    private static final String TOO_LATE = "Đã tới thời điểm đề xuất, không thể chấp nhận yêu cầu";
    private static final String SENDER_INACTIVE = "Người gửi không còn hoạt động";

    private final ExchangeRequestRepository requestRepository;
    private final ExchangeSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final UserLearnSkillRepository learnRepository;
    private final UserTeachSkillRepository teachRepository;
    private final MatchingDataRepository matchingDataRepository;
    private final Clock clock;

    public ExchangeRequestService(ExchangeRequestRepository requestRepository,
                                  ExchangeSessionRepository sessionRepository,
                                  UserRepository userRepository,
                                  UserLearnSkillRepository learnRepository,
                                  UserTeachSkillRepository teachRepository,
                                  MatchingDataRepository matchingDataRepository,
                                  Clock clock) {
        this.requestRepository = requestRepository;
        this.sessionRepository = sessionRepository;
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

        // Đọc lại kèm tên người, tên kỹ năng bằng cùng câu truy vấn mà hộp yêu cầu dùng.
        RequestRow row = requestRepository.findRowById(saved.getId()).orElseThrow();
        return RequestItem.from(row, senderId);
    }

    // ================= Hộp yêu cầu và chi tiết (D91) =================

    /**
     * GET /api/requests?box=&status=. box chỉ nhận đúng chữ thường như TK 4.6 (Chốt ở bước 6), nên nhận String
     * rồi xét ở đây: bộ chuyển chuỗi sang enum của Spring phân biệt hoa thường. status sai đã bị Spring trả 400.
     */
    @Transactional(readOnly = true)
    public List<RequestItem> list(long viewerId, String box, RequestStatus status) {
        List<RequestRow> rows = switch (box) {
            case "sent" -> requestRepository.findSentRows(viewerId, status);
            case "received" -> requestRepository.findReceivedRows(viewerId, status);
            // Cùng câu với lỗi tham số sai kiểu (D38): với người gọi, cả hai đều là "giá trị không hợp lệ".
            default -> throw new InvalidRequestException("Tham số box có giá trị không hợp lệ: " + box);
        };
        return rows.stream().map(row -> RequestItem.from(row, viewerId)).toList();
    }

    /** GET /api/requests/{id}: chỉ người gửi và người nhận (D91). */
    @Transactional(readOnly = true)
    public RequestItem get(long viewerId, long requestId) {
        RequestRow row = requestRepository.findRowById(requestId)
                .orElseThrow(() -> new NotFoundException(REQUEST_NOT_FOUND));
        checkParticipant(row.senderId(), row.receiverId(), viewerId);
        return RequestItem.from(row, viewerId);
    }

    // ================= Phản hồi (D90) =================

    /**
     * POST /api/requests/{id}/accept. Đổi trạng thái yêu cầu và tạo phiên trong MỘT transaction: lệnh tạo phiên
     * lỗi thì Spring rollback cả việc đổi trạng thái (TC327), không bao giờ có yêu cầu ACCEPTED mà không có phiên.
     */
    @Transactional
    public AcceptResponse accept(long viewerId, long requestId) {
        Instant now = clock.instant();
        ExchangeRequest request = findForParticipant(requestId, viewerId);       // 404, 403 không tham gia
        if (request.getReceiverId() != viewerId) {                                // 403 sai vai trò
            throw new ForbiddenException(ONLY_RECEIVER);
        }
        // Ba lỗi 409 xét đúng thứ tự của D90.
        checkPending(request);
        if (!now.isBefore(request.getProposedStartAt())) {                        // đúng giờ đề xuất cũng là muộn (R34)
            throw new ConflictException(TOO_LATE);
        }
        if (!isActive(request.getSenderId())) {                                   // R37
            throw new ConflictException(SENDER_INACTIVE);
        }

        request.accept(now);
        // Ghi ngay, trước khi tạo phiên: nếu ai đó vừa đổi yêu cầu này (version đã khác), lỗi khóa lạc quan
        // nổ ra ở đây và GlobalExceptionHandler trả 409.
        requestRepository.flush();
        ExchangeSession session = sessionRepository.save(ExchangeSession.scheduleFrom(request, now));
        return new AcceptResponse(request.getId(), session.getId());
    }

    /** POST /api/requests/{id}/reject. Quá giờ đề xuất vẫn từ chối được (R34). note đã cắt, có thể null. */
    @Transactional
    public RequestItem reject(long viewerId, long requestId, String note) {
        ExchangeRequest request = findForParticipant(requestId, viewerId);
        if (request.getReceiverId() != viewerId) {
            throw new ForbiddenException(ONLY_RECEIVER);
        }
        checkPending(request);

        request.reject(note, clock.instant());
        return flushAndRead(request, viewerId);
    }

    /** POST /api/requests/{id}/cancel. Quá giờ đề xuất vẫn hủy được (R34). */
    @Transactional
    public RequestItem cancel(long viewerId, long requestId) {
        ExchangeRequest request = findForParticipant(requestId, viewerId);
        if (request.getSenderId() != viewerId) {
            throw new ForbiddenException(ONLY_SENDER);
        }
        checkPending(request);

        request.cancel(clock.instant());
        return flushAndRead(request, viewerId);
    }

    /** 404 nếu không có yêu cầu, 403 nếu người gọi không phải người gửi hay người nhận. */
    private ExchangeRequest findForParticipant(long requestId, long viewerId) {
        ExchangeRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException(REQUEST_NOT_FOUND));
        checkParticipant(request.getSenderId(), request.getReceiverId(), viewerId);
        return request;
    }

    private static void checkParticipant(long senderId, long receiverId, long viewerId) {
        if (viewerId != senderId && viewerId != receiverId) {
            throw new ForbiddenException(NOT_PARTICIPANT);
        }
    }

    private static void checkPending(ExchangeRequest request) {
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ConflictException(NOT_PENDING);
        }
    }

    private boolean isActive(long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElse(false);
    }

    /**
     * Ghi thay đổi xuống DB rồi đọc lại bằng câu JPQL của hộp yêu cầu, để response giống hệt GET /{id}.
     * flush là chỗ lỗi khóa lạc quan (nếu có) nổ ra; câu đọc sau đó thấy trạng thái mới vì cùng transaction.
     */
    private RequestItem flushAndRead(ExchangeRequest request, long viewerId) {
        requestRepository.flush();
        RequestRow row = requestRepository.findRowById(request.getId()).orElseThrow();
        return RequestItem.from(row, viewerId);
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
