# Đề xuất sửa spec: kết quả rà soát phân tích yêu cầu

- **Tài liệu được rà soát:** [Phan_tich_yeu_cau_Skill_Exchange_Tuan_1.md](Phan_tich_yeu_cau_Skill_Exchange_Tuan_1.md), có đối chiếu với [Skill_Exchange_Proposal_9_Tuan.md](../Skill_Exchange_Proposal_9_Tuan.md) và [De_xuat_chinh_sua.md](De_xuat_chinh_sua.md)
- **Ngày rà soát:** 2026-10-22
- **Người rà soát:** Claude (AI). Kết quả cần được người thực hiện kiểm chứng lại.
- **Tổng số vấn đề:** 26 (🔴 6 nghiêm trọng, 🟠 5 cao, 🟡 12 trung bình, 🟢 5 thấp)

**Phân loại:**
- **[Lỗi]**: sai hoặc mâu thuẫn chắc chắn, cần sửa.
- **[Quyết định]**: spec bỏ trống, người thực hiện cần chọn phương án.

**Cách dùng file này:** điền cột **Quyết định** sau khi chốt phương án, đánh dấu ☑ khi đã cập nhật vào spec.

**Cập nhật 2026-10-24:** Đợt 1 (N1–N5, C1–C3, T1, T2) đã được chốt và sửa trong cả `Tuần 1_Phân tích yêu cầu.docx` và `Phan_tich_yeu_cau_Skill_Exchange_Tuan_1.md`. Hai bản đã đồng bộ các quyết định này.

**Cập nhật 2026-10-24 (lần 2):** Đã sửa vào file `.md` các mục có quyết định từ báo cáo tuần 1–2 và giả định G1–G7 của thiết kế tuần 2 (T3 phần ModeFit, T4, T7–T12, L2–L5), đồng thời đồng bộ phạm vi và kế hoạch tuần với proposal. **Bản `.docx` chưa được cập nhật lần này.** Các điểm thiết kế thực nghiệm (N6, C4, C5, T3 phần nhãn 2, T5, T6) để lại, ghi ở mục 5.5 của spec, chốt trước tuần 5.

---

## 🔴 Nghiêm trọng: chặn việc code thuật toán hoặc làm sai kết quả đánh giá

| # | Vị trí | Loại | Vấn đề | Hậu quả | Quyết định | Xong |
|---|---|---|---|---|---|---|
| N1 | 4.3, dòng 92–93 | Lỗi | Công thức CompletionRate bị cắt giữa chừng, tiêu đề mục 5 dính vào. Quy tắc khi thiếu dữ liệu cũng mất theo. Bản đầy đủ còn trong `De_xuat_chinh_sua.md` mục 5 | Không code được Reputation. Bản `.docx` có thể cũng lỗi | Khôi phục công thức và quy tắc dữ liệu thiếu; khôi phục tiêu đề mục 5 | ☑ |
| N2 | 4.0, 4.3 | Lỗi | Công thức tổng D(u,v), các trọng số và MutualScore không được định nghĩa trong file. Trọng số chỉ xuất hiện gián tiếp ở 5.4, công thức chỉ có trong Proposal | Code theo file này sẽ thiếu đúng phần cốt lõi | Thêm mục 4.4 (D(u,v)) và 4.5 (MutualScore) | ☑ |
| N3 | 4.0, 7.1 (dòng 265) | Quyết định | Chưa định nghĩa thế nào là "ghép hai chiều". Kỹ năng liên quan (Sim = 0.5) có tính không? Lưu ý: dù không có kỹ năng chiều ngược lại, D(v,u) vẫn lớn hơn 0 nhờ các thành phần khác, nên MutualScore lớn hơn 0 | Phân loại sai: người chỉ hợp một chiều lọt vào tab "Hai chiều" | Hai chiều ⇔ SkillFit(u,v) > 0 và SkillFit(v,u) > 0, tính cả kỹ năng liên quan (mục 4.5) | ☑ |
| N4 | 4.3 LevelFit, 5.1 | Quyết định | LevelFit không nói tính thế nào khi u muốn học nhiều kỹ năng, hoặc khi kỹ năng chỉ là kỹ năng liên quan | Không code được LevelFit. Nhãn 2 và nhãn 0 ở 5.1 cũng mơ hồ theo | Trung bình LevelFit trên các kỹ năng có Sim > 0, ghép với t có Sim cao nhất; kỹ năng liên quan không giảm thêm | ☑ |
| N5 | Mục 2 so với 4.2, 4.3, 5.1 | Lỗi | Lịch rảnh và hình thức học nằm ở mức "Nên có", nhưng lọc cứng, ScheduleFit (trọng số 0.15), ModeFit và nhãn 2 đều cần chúng | Nếu cắt "nên có" khi thiếu thời gian, thuật toán và cách đánh giá đều hỏng | Đưa lịch rảnh, hình thức học lên MVP | ☑ |
| N6 | 5.2, 5.3 | Quyết định | P có hai danh sách (hai chiều xếp theo MutualScore, một chiều xếp theo D), nhưng chỉ số đo trên một Top-5 duy nhất, còn nhãn chỉ gán theo hướng "u học từ v" | Không biết đánh giá danh sách nào, tuần 5 không làm được | Để sau, ghi ở spec mục 5.5; chốt trước tuần 5 | ☐ |

## 🟠 Cao: chặn thiết kế database/API ở tuần 2, hoặc dễ bị phản biện

| # | Vị trí | Loại | Vấn đề | Hậu quả | Quyết định | Xong |
|---|---|---|---|---|---|---|
| C1 | 6.1, 6.2 | Quyết định | Khi ACCEPTED, hệ thống "tạo ngay phiên SCHEDULED đã chốt lịch và hình thức". Nhưng giờ, hình thức và địa điểm lấy từ đâu? Không có bước hai bên thống nhất lịch | Không thiết kế được bảng phiên và API đặt lịch | Người gửi đề xuất thời điểm, thời lượng, hình thức, địa điểm/link trong yêu cầu; chấp nhận = đồng ý lịch (mục 6.4) | ☑ |
| C2 | 6.x | Quyết định | Trao đổi hai chiều (A dạy B, B dạy A) là một phiên hay hai phiên? Yêu cầu có ghi rõ kỹ năng nào, hướng nào không? | Ảnh hưởng trực tiếp tới cấu trúc DB, cách tính CompletionRate và đánh giá | Một yêu cầu = một phiên = một buổi; ghi rõ loại TWO_WAY/ONE_WAY và kỹ năng từng chiều | ☑ |
| C3 | 6.2, 6.3, mục 2 | Lỗi | "Báo cáo sự cố" là lối thoát duy nhất khi bất đồng, nhưng không có FR nào cho nó, mục 2 lại xếp vào "mở rộng", và không nói admin xử lý xong thì phiên chuyển sang trạng thái gì | Phiên kẹt ở WAITING_CONFIRMATION mãi mãi | Báo cáo sự cố lên MVP, thêm FR-09; admin chốt phiên sang COMPLETED hoặc CANCELLED | ☑ |
| C4 | 5.2, 5.3 | Quyết định | IDCG tính trên tập ứng viên nào? B0 và B1 có áp dụng lọc lịch và hình thức không? | B0 có ít ứng viên có thể đạt NDCG cao một cách giả tạo, làm so sánh B0/B1/P sai | Để sau, ghi ở spec mục 5.5 | ☐ |
| C5 | 5.1 so với 5.2 | Quyết định | Nhãn Ground Truth được xây từ chính các đặc trưng mà P dùng (kỹ năng, lịch, hình thức, trình độ), trong khi B1 chỉ dùng SkillFit. Nhãn không dùng Reputation, nên cấu hình "Ưu tiên uy tín" chắc chắn bị điểm kém hơn. Không phải lỗi logic, nhưng P thắng một phần là do cách thiết kế thí nghiệm | Câu phản biện dễ gặp nhất khi bảo vệ: "Em tự đặt đáp án rồi tự chấm?" Cần nêu rõ trong phần hạn chế, hoặc thiết kế nhãn khác đi | Để sau, ghi ở spec mục 5.5 | ☐ |

## 🟡 Trung bình: gây khó khi code hoặc làm giảm chất lượng đánh giá

| # | Vị trí | Loại | Vấn đề | Hậu quả | Quyết định | Xong |
|---|---|---|---|---|---|---|
| T1 | 4.3, FR-03 | Quyết định | Chưa định nghĩa thang trình độ (mấy bậc, tên gì) | "Thấp hơn 1 bậc" không tính được | Thang 4 bậc có mô tả; mục tiêu không bắt buộc (mục 4.6) | ☑ |
| T2 | 4.2, 4.3 | Quyết định | Chưa định nghĩa "khung giờ" | Không thiết kế được bảng lịch rảnh | 21 khung/tuần: 7 ngày × sáng/chiều/tối (mục 4.6) | ☑ |
| T3 | 4.3 ModeFit, 5.1 | Lỗi | Trường hợp ANY gặp bên chưa khai báo vừa được 1.0 vừa được 0.5. Nhãn 2 cần "tương thích hoàn toàn", nhưng không rõ ANY + ONLINE có tính là hoàn toàn không | Code và nhãn có thể hiểu khác nhau | Phần ModeFit đã chốt trong báo cáo (Bảng 4.8): một bên chưa khai báo thì 0.5, kể cả khi bên kia chọn ANY. Phần nhãn 2 ("tương thích hoàn toàn") để sau, ghi ở spec mục 5.5 | ◐ |
| T4 | 5.2 | Quyết định | Không có quy tắc khi bằng điểm. Với B0 (xếp theo số kỹ năng trùng), rất nhiều ứng viên cùng có 1 kỹ năng trùng | Thứ tự phụ thuộc thứ tự dữ liệu, kết quả khó tái lập | Bằng điểm thì xếp theo mã người dùng tăng dần, áp dụng cho P, B0, B1 (spec mục 4.4) | ☑ |
| T5 | 5.3 | Quyết định | Precision@5 tính cả nhãn 1, mà nhãn 1 là "mọi trường hợp hợp lệ còn lại" | Gần như ai có từ 5 ứng viên trở lên cũng đạt khoảng 1.0, chỉ số không phân biệt được các phương án | Để sau, ghi ở spec mục 5.5 | ☐ |
| T6 | 5.4 | Quyết định | Dữ liệu giả lập cho mọi người đều khai lịch và hình thức học | Các nhánh "chưa khai báo → 0.5" không bao giờ được đánh giá | Để sau, ghi ở spec mục 5.5 | ☐ |
| T7 | 6.1 | Quyết định | Phiên chuyển từ SCHEDULED sang IN_PROGRESS do hệ thống tự động hay người dùng bấm? Không có thời hạn cho PENDING và IN_PROGRESS | Nếu tự động thì cần job chạy định kỳ. Yêu cầu và phiên có thể treo mãi | Theo G1, G2: người tham gia bấm "Bắt đầu" từ 15 phút trước giờ hẹn; chưa có thời hạn tự hết hạn trong MVP (spec 6.1, 6.2) | ☑ |
| T8 | 6.2 | Quyết định | A hủy phiên đang diễn ra thì CompletionRate của B có bị tính vào mẫu số không? | Người bị hủy oan cũng bị giảm uy tín | Theo G3: có, tính cho cả hai bên; ghi là hạn chế đã biết (spec 6.2) | ☑ |
| T9 | Mục 3 so với Proposal | Lỗi | Proposal bắt buộc các chức năng: quên mật khẩu, tìm kiếm, giải thích lý do phù hợp, quản lý/khóa tài khoản. Mục 3 không có FR cho những chức năng này, dù 5.1 và 7.1 có nhắc tới "tài khoản bị khóa" | Hai tài liệu nói khác nhau về phạm vi bắt buộc | Thêm FR-10 tìm kiếm; FR-04 có giải thích kết quả; FR-08 thêm khóa/mở khóa tài khoản; quên mật khẩu hạ xuống "Nên có" (FR-23) ở cả spec và proposal | ☑ |
| T10 | Mục 9 | Lỗi | Danh mục kỹ năng và kỹ năng liên quan cần có từ tuần 3–4, nhưng "quản trị danh mục" lại xếp ở tuần 7. Kế hoạch tuần 7–8 cũng khác với Proposal | Tuần 3–4 thiếu dữ liệu. Có thể khắc phục bằng dữ liệu mẫu nạp sẵn qua Flyway, nhưng cần ghi rõ | Danh mục mẫu nạp sẵn bằng migration ở tuần 3; kế hoạch hợp nhất dùng chung cho spec mục 9 và proposal mục 7 | ☑ |
| T11 | Mục 8 | Lỗi | Ghi "chống khai khống qua xác thực email trường", mâu thuẫn với Proposal 4.1. Vẫn thiếu nguồn và ngày truy cập (yêu cầu ở mục 7 của `De_xuat_chinh_sua.md`) | Dễ bị bắt lỗi khi bảo vệ | Đã sửa trước đó: mục 8 ghi "không chống khai khống tuyệt đối", có ghi chú nguồn và ngày truy cập | ☑ |
| T12 | 7.1 | Lỗi | Thiếu unit test cho gán nhãn, các chỉ số NDCG/Precision, chuyển trạng thái bị cấm, và quyền xem liên hệ | Đúng những phần quyết định kết quả nghiên cứu lại không có test | Thêm nhóm 4 (gán nhãn, chỉ số) và nhóm 5 (chuyển trạng thái bị cấm, quyền truy cập) vào spec 7.1 | ☑ |

## 🟢 Thấp: câu chữ và định dạng

| # | Vấn đề | Quyết định | Xong |
|---|---|---|---|
| L1 | "Một trong hai bên chưa khai báo" chưa nói tới trường hợp cả hai đều chưa khai báo (ScheduleFit, ModeFit) | Báo cáo Bảng 4.8: một hoặc cả hai bên chưa khai báo đều được 0.5 | ☑ |
| L2 | Tên không thống nhất: `CANCELLED_REQ`/`CANCELLED_SES` (sơ đồ) so với `CANCELLED` (bảng); `ONLINE`/"Online"/`ANY / CẢ HAI` | Sơ đồ hiển thị nhãn `CANCELLED`, mã nội bộ chỉ để vẽ; thống nhất dùng `ANY` | ☑ |
| L3 | Sau khi phiên kết thúc hoặc bị hủy, quyền xem liên hệ còn không? Quyền admin xem liên hệ chưa có trong NFR-02 | Theo thiết kế tuần 2: hai bên vẫn xem được sau khi phiên kết thúc; admin xem được khi tra cứu vi phạm (spec 6.3, NFR-02) | ☑ |
| L4 | Không có quy định về gửi trùng yêu cầu cho cùng một người. Chưa định nghĩa k hiển thị trên giao diện (đánh giá dùng 5) | Theo G4: tối đa một yêu cầu `PENDING` tới cùng người nhận; giao diện hiển thị tối đa 10 mỗi nhóm (spec 4.4, 6.2) | ☑ |
| L5 | Ba tiêu đề cấp 1 lặp nhau ở đầu file. Mục 10 tự nhận "công thức có định nghĩa cho từng thành phần", điều này chưa đúng (xem N2, N4) | Gộp còn một tiêu đề; N2, N4 đã xong nên mục 10 đúng | ☑ |

---

## Thứ tự xử lý đề xuất

| Đợt | Hạn | Vấn đề | Lý do |
|---|---|---|---|
| 1 | Trước khi thiết kế DB (tuần 2) | N1–N5, C1–C3, T1, T2 | Quyết định bảng và cột trong DB |
| 2 | Trước tuần 4 | T3, T7, T8, phần test của T12 | Ảnh hưởng code thuật toán và vòng đời phiên |
| 3 | Trước tuần 5 | N6, C4, C5, T4–T6 | Thiết kế thí nghiệm |
| 4 | Trước khi viết báo cáo | T9–T11, nhóm 🟢 | Đồng bộ tài liệu, trình bày |

## Kiểm chứng của người thực hiện

Kết quả trên do AI rà soát. Trước khi sửa spec, tự mở file gốc và kiểm tra lại ít nhất các mục sau:

- [ ] N1: dòng 92–93 có đúng bị hỏng không?
- [x] N5: mục 2 có đúng xếp lịch rảnh vào "Nên có" không? → Đúng; đã chuyển lịch rảnh, hình thức học và báo cáo sự cố lên MVP.
- [ ] T11: mục 8 có đúng ghi "chống khai khống qua xác thực email trường" không?
- [ ] Mục nào bạn **không đồng ý** với AI? Ghi lý do vào cột Quyết định.
