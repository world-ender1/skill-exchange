# KẾ HOẠCH TUẦN 4

Lập ngày 2026-11-04. Áp dụng `docs/tuan_4/Quyet_dinh_tuan_4.md` (D31–D38, D43–D50) và `docs/tuan_4/kiem_thu/ca_kiem_thu.md` (87 ca). Các bước 1–10 ứng với C3–C12 của bài học tuần 4.

Quy ước chung:
- Lõi ở `backend/src/main/java/com/skillexchange/matching/`, test lõi ở `backend/src/test/java/com/skillexchange/matching/`.
- **Bước 1–7 là lõi:** JUnit thuần, không `@SpringBootTest`, không import `org.springframework`, không gọi Repository (D34). Lõi được dùng các enum thuần Java có sẵn ở package `user` (`UserStatus`, `Role`, `LearningMode`, `Period`).
- **Bước 2–7 làm TDD hai lượt:** lượt TEST viết test, chạy thấy đỏ, người học đọc từng số mong đợi; lượt CODE viết code cho xanh, không sửa test. Không dùng `/buoc-tuan` cho các bước này vì skill làm test và code liền một lần.
- Test tích hợp (bước 8–9) kế thừa `IntegrationTestBase`. Mỗi test có `@DisplayName("TCxxx ...")`.
- Sau mỗi bước: `git diff --stat -- backend/src/test` để chắc test đã duyệt không bị sửa; `.\mvnw.cmd test` xanh toàn bộ, kể cả test tuần 3.

## 1. Việc làm trước (không phải code, tự làm)

| Trước bước | Việc |
| --- | --- |
| 1 | Tự tính tay ví dụ An–Bình (bài học C2) và ví dụ LevelFit spec 4.6 trên giấy |
| 8 | Docker đang chạy, DB `skillexchange_test` đã có (D39). Đọc các CHECK của `exchange_requests`, `exchange_sessions`, `reviews` trong `V1__init.sql` để chèn dữ liệu test đúng |

## 2. Các bước

### Bước 1 – Khung lõi (C3; D34, D35, D43, D46, D47)

- **Tạo** trong `matching/`:
  - Record dữ liệu vào: `LearnItem(skillId, skillName, Integer targetLevel)`, `TeachItem(skillId, skillName, int level)`, `Slot(dayOfWeek, Period)`, `MatchProfile(userId, status, role, learn, teach, slots, mode, reputation)`, `SessionOutcome(SessionStatus, started)` và enum `SessionStatus`.
  - `MatchingWeights` (record, hằng số mặc định, kiểm từng trọng số trong [0, 1] và tổng bằng 1).
  - Record kết quả: `ScoreBreakdown`, `SkillMatch(want, teach, sim)`, `MatchResult` (`backward`, `theyLearn` là `null` khi `ONE_WAY`), enum `MatchType`, `MatchingResult(twoWay, oneWay)`.
  - Lớp có chữ ký phương thức, thân `throw new UnsupportedOperationException()`: `SkillSimilarity`, `ScoreCalculator`, `ReputationCalculator`, `HardFilter`, `MatchingEngine`, `RankingComparator`.
- **Test:** `MatchingWeightsTest`; lớp tiện ích `MatchingFixtures` dựng sẵn hồ sơ An, Bình, ví dụ spec 4.6 và A.3 cho các bước sau dùng chung.
- **Ca:** TC126.
- **Chốt ở bước này:**
  - `LearnItem`, `TeachItem` mang `skillName` để lõi tự xếp `youLearn`, `theyLearn` theo D46(b); nhờ vậy TC144 là unit test.
  - Mục tiêu dùng `Integer` (được null, spec 4.6); trình độ dạy dùng `int` (`level` là `NOT NULL`).
  - Các hàm điểm của `ScoreCalculator` nhận đúng phần dữ liệu cần (`List<LearnItem>`, `List<TeachItem>`, `Set<Slot>`, `LearningMode`), không nhận cả `MatchProfile`, để unit test bước 2–4 viết gọn. Riêng `breakdown(learner, teacher, weights)` nhận hồ sơ và tính $D$ (bước 7). Hàm chọn kỹ năng dạy là `bestTeachFor` (bước 3).
  - `ReputationCalculator.compute` là phương thức `static` vì là hàm thuần, không giữ trạng thái.
  - Cặp liên quan là record `SkillRelation(skillA, skillB)`; `SkillSimilarity` nhận danh sách cặp này.
  - `MatchingEngine(similarity, weights)` tự tạo `ScoreCalculator` và `HardFilter` bên trong; `match(caller, candidates)` trả `MatchingResult`.
  - `MatchProfile`, `MatchResult`, `MatchingResult` chép danh sách sang bản không sửa được (`List.copyOf`), để record bất biến thật sự.
  - Tổng trọng số so với 1 bằng sai số $10^{-9}$ (D35, D47): bộ (0.1, 0.2, 0.3, 0.2, 0.2) hợp lệ dù tổng trong `double` không ra đúng 1.0.
- **Tự kiểm:** `grep -r "org.springframework" backend/src/main/java/com/skillexchange/matching` không ra gì.
- **Commit:** `feat(matching): add matching core skeleton`

### Bước 2 – Sim và SkillFit (C4; spec 4.1)

- **Sửa:** `SkillSimilarity` (dựng từ tập cặp liên quan, tra được cả hai chiều), `ScoreCalculator.skillFit`.
- **Test:** `SkillSimilarityTest`, `SkillFitTest`.
- **Ca:** TC101–TC107.
- **Tự kiểm:** `sim(Kotlin, Java)` bằng `sim(Java, Kotlin)`; $W_u$ rỗng không chia cho 0; xóa tạm dòng `s == t → 1.0` thì TC101 đỏ.
- **Commit:** `feat(matching): add skill similarity and SkillFit`

### Bước 3 – LevelFit (C5; spec 4.3, 4.6, D46)

- **Sửa:** `ScoreCalculator.levelFit` và hàm chọn kỹ năng dạy cho mỗi kỹ năng muốn học (Sim cao nhất → trình độ cao hơn → `skillId` nhỏ hơn). Hàm chọn này dùng lại ở bước 7 để dựng `youLearn`, `theyLearn`.
- **Test:** `LevelFitTest` (`@ParameterizedTest` cho bảng mục tiêu/trình độ).
- **Ca:** TC108–TC115.
- **Chốt ở bước này:** không có kỹ năng muốn học nào có Sim > 0 (ứng viên chưa qua lọc cứng, hoặc $W_u$ rỗng) thì `levelFit` trả 0.0, không chia cho 0, cùng cách với SkillFit (TC107). Spec 4.6 chỉ nói trường hợp này không xảy ra sau lọc cứng; hàm vẫn phải an toàn vì TC141 gọi trên dữ liệu ngẫu nhiên.
- **Tự kiểm:** TC110 ra 0.75 (ra 0.5 là đã chia cả Figma); TC113 cho thấy Sim đứng trước trình độ.
- **Commit:** `feat(matching): add LevelFit`

### Bước 4 – ScheduleFit và ModeFit (C6; spec 4.3, 4.6)

- **Sửa:** `ScoreCalculator.scheduleFit`, `ScoreCalculator.modeFit`.
- **Test:** `ScheduleAndModeFitTest`.
- **Ca:** TC116–TC119.
- **Chốt ở bước này:** `modeFit(ONLINE, OFFLINE)` (cặp đã bị loại ở lọc cứng) ném lỗi hay trả 0. Chọn một cách, có một test, ghi lý do trong commit message (bộ ca, mục "Không kiểm ở tuần 4").
- **Tự kiểm:** chia cho `min`, không phải `max`; "chưa khai báo" + `ANY` ra 0.5.
- **Commit:** `feat(matching): add ScheduleFit and ModeFit`

### Bước 5 – Reputation (C7; spec 4.3, D43)

- **Sửa:** `ReputationCalculator.compute(List<Integer> ratings, List<SessionOutcome> sessions)`.
- **Test:** `ReputationCalculatorTest`.
- **Ca:** TC120–TC125.
- **Tự kiểm:** tìm mọi phép chia có mẫu số có thể bằng 0; có `assertFalse(Double.isNaN(...))`; TC123 ra 0.7625 (quy tắc cũ ra 0.7125).
- **Commit:** `feat(matching): add reputation calculator`

### Bước 6 – Lọc cứng (C8; spec 4.2, D43)

- **Sửa:** `HardFilter`.
- **Test:** `HardFilterTest`; mỗi ca có ứng viên đối chứng hợp lệ trong cùng danh sách.
- **Ca:** TC127–TC133.
- **Tự kiểm:** cho hàm trả danh sách rỗng thì TC133 phải đỏ.
- **Commit:** `feat(matching): add hard filter`

### Bước 7 – D, MutualScore, phân loại, xếp hạng (C9; spec 4.4, 4.5, D36, D46)

- **Sửa:** `MatchingEngine` (tính $D$ hai chiều, phân loại, `youLearn`, `theyLearn`, `commonSlots`), `RankingComparator`.
- **Test:** `MatchingEngineTest`, `RankingComparatorTest`.
- **Ca:** TC134–TC144.
- **Tự kiểm:** MutualScore so với `Math.sqrt(0.6875 * 0.925)`; chỉ có **một** comparator dùng cho cả hai nhóm; không có `if` riêng cho dữ liệu mẫu; TC141 dùng seed cố định.
- **Commit:** `feat(matching): add matching engine with ranking`

### Bước 8 – `MatchingService` và `GET /api/matches` (C10; TK 3.5, 4.5, D32, D38, D43, D44, D49)

- **Sửa:**
  - `auth/AccountStatusFilter.java`: gắn thêm quyền `ACTIVE_STUDENT` khi `status = ACTIVE` và claim `role = STUDENT` (D44).
  - `common/SecurityConfig.java`: luật `hasAuthority("ACTIVE_STUDENT")` cho `/api/matches/**` và `/api/users/**`.
  - `common/GlobalExceptionHandler.java`: override `handleTypeMismatch` (D38); lỗi thiếu tham số và `limit` ngoài 1–50 cũng trả Problem Details tiếng Việt.
- **Tạo:**
  - `matching/MatchingDataRepository.java` (`@Repository`, `JdbcTemplate`, SQL thuần): nạp ứng viên `ACTIVE` + `STUDENT`, kỹ năng dạy/học, lịch rảnh, cặp liên quan, điểm đánh giá, phiên dạng thô (JOIN `exchange_requests` theo `sender_id`/`receiver_id`), danh sách người nhận có yêu cầu `PENDING` của người gọi, `ratingAverage`/`reviewCount`.
  - `matching/MatchingService.java` (`@Transactional(readOnly = true)`), `matching/MatchingController.java`.
  - DTO `MatchListResponse`, `MatchItemResponse`, `ScoreResponse`, `SkillMatchResponse`; `@JsonInclude(NON_NULL)` chỉ trên `backward`, `theyLearn` (D49).
- **Test:** `matching/MatchesApiTest`; `common/ActiveAccessTest` (phần `/matches`); helper trong test để tạo sinh viên đã xác thực kèm hồ sơ, kỹ năng, lịch.
- **Ca:** TC145–TC156, TC185, TC176, TC177; phần `/matches` của TC75, TC173, TC174, TC175, TC178. TC157 thủ công.
- **Chốt ở bước này:** dữ liệu thô đọc bằng `JdbcTemplate` thay vì Entity, vì `exchange_requests`, `exchange_sessions`, `reviews`, `skill_relations` chưa có Entity (tuần 6–7 mới tạo) và SQL thuần dễ đối chiếu với TK 3.5. Tuần 6 tạo Entity cho các bảng này thì xem lại có chuyển sang JPQL không.
- **Tự kiểm:** bật log SQL, số câu không đổi khi tăng từ 20 lên 40 người (TC157); admin gọi `/matches?type=FOO` ra 403; đăng nhập An, gọi API thấy Bình `score` 0.797.
- **Commit:** `feat(matching): add matches API`

### Bước 9 – Hồ sơ công khai và tìm kiếm (C11; TK 4.3, D37, D38, D45, D46)

- **Tạo:**
  - `user/PublicUserController.java` (`GET /api/users/{id}`, `GET /api/users/search`), `user/PublicUserService.java`.
  - DTO `PublicProfileResponse`, `UserSearchResultResponse`, `PublicSkillResponse` (không có `isActive`).
  - `common/LikePatterns.java`: escape `\`, `%`, `_` cho `ILIKE ... ESCAPE '\'`.
- **Sửa:** `user/UserRepository.java` (truy vấn tìm kiếm: lọc `ACTIVE` + `STUDENT`, bỏ người gọi, xếp theo `full_name` rồi `id`, tối đa 20).
- **Test:** `user/PublicProfileTest`, `user/UserSearchTest`; phần `/users` của `ActiveAccessTest`.
- **Ca:** TC158–TC172, TC76; phần `/users` của TC75, TC173, TC174, TC175, TC178.
- **Tự kiểm:** `q=%` không trả về tất cả; bốn trường hợp ở TC160 cùng `detail`; ghi lệnh thử vào `hoc_vibe_coding/http/tuan_4.http`.
- **Commit:** `feat(user): add public profile and user search`

### Bước 10 – Frontend S05, S06, S10 (C12; TK 6.1, 6.3, D31, D45, D49, D50)

- **Sửa:** `src/api/types.ts` (kiểu kết quả matching với `backward?`, `theyLearn?` theo D49; hồ sơ công khai; kết quả tìm kiếm), `src/App.tsx` (route `/matches`, `/users/:id`, `/search`), `src/components/Layout.tsx` (menu).
- **Tạo:** `src/pages/MatchesPage.tsx` (S05), `src/pages/UserProfilePage.tsx` (S06), `src/pages/SearchPage.tsx` (S10), `src/components/MatchCard.tsx`, `src/format.ts` (`formatPercent` theo D50, nhãn khung giờ).
- **Ca:** TC179–TC184 (thủ công).
- **Tự kiểm:** `npm run build`, `npm run lint` không lỗi; màn hình rộng 360 px; không có nút "Gửi yêu cầu"; frontend chưa có công cụ test nên `formatPercent` kiểm bằng tay với 0.78 và 0.797.
- **Commit:** mỗi màn hình một commit, ví dụ `feat(frontend): add matches page`

## 3. Đối chiếu ca kiểm thử

Đã đối chiếu 87 ca: mọi ca đều thuộc một bước.
- Ca trải trên hai bước (luật D44 thêm ở bước 8, API `/users` có ở bước 9): TC75, TC173, TC174, TC175, TC178.
- Ca thủ công: TC157 → bước 8; TC179–TC184 → bước 10.
- TC141 cần đủ mọi thành phần điểm nên nằm ở bước 7.

## 4. Sau bước 10 – Kiểm tra cả tuần (C13–C15)

Hook tự chạy test lõi; truy vết ca → test; làm hỏng code để chắc test đỏ; chạy lại kiểm thử lược đồ tuần 2 và cập nhật `docs/tuan_2/kiem_thu/ket_qua.txt`; đối chiếu code với D31–D38, D43–D50; viết `docs/tuan_4/kiem_thu/README.md` và nội dung báo cáo tuần 4.
