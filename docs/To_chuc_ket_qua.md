# Tổ chức kết quả dự án để đưa vào báo cáo

> Mục đích: thống nhất cách lưu kết quả kiểm thử và thực nghiệm, để mọi con số trong báo cáo truy ngược được về dữ liệu gốc và phiên bản code đã sinh ra nó.
> Áp dụng từ tuần 3 (kiểm thử), đầy đủ từ tuần 5 (thực nghiệm thuật toán).

## 1. Repo tham khảo

Tham khảo: https://github.com/minhnguyen-vnu/graduational_thesis (luận văn nghiên cứu, thêm bộ phát hiện lỗi phân quyền vào công cụ mã nguồn mở EvoMaster).

### 1.1. Cách repo đó tổ chức

Code chính **không nằm trong repo này**. Tác giả sửa EvoMaster trên một nhánh riêng của repo EvoMaster. Repo luận văn chứa mọi thứ còn lại:

| Thư mục / file | Chứa gì |
|---|---|
| File `.md` / `.html` / `.pdf` ở gốc | Đề cương, báo cáo tiến độ, đã xuất ra PDF |
| `docs/results.md` | **Nguồn duy nhất cho mọi con số** trong báo cáo. Số ghi ở chỗ khác mà lệch file này thì coi như sai |
| `docs/` (còn lại) | Thiết kế, tổng quan tài liệu, giải thích code (`code-walkthrough/`) |
| `infra/em-results/` | Kết quả thô của từng lượt chạy, mỗi lượt một thư mục đặt tên theo ngày (ví dụ `airflow-3users-20260924`), không ghi đè |
| `infra/<target>/` | Script dựng môi trường thử nghiệm, self-test |
| `.claude/`, `CLAUDE.md` | Harness cho AI: agent, hook chặn xóa dữ liệu kết quả, skill chạy thực nghiệm |

**Lý do tách repo:** không thể đưa tài liệu luận văn vào repo EvoMaster của người khác. Với Skill Exchange, code là của mình nên lý do này không áp dụng.

### 1.2. Các nguyên tắc áp dụng cho Skill Exchange

1. **Một file nguồn cho mọi con số.** Mỗi số ghi kèm file kết quả và commit code đã sinh ra nó. Ví dụ trong repo tham khảo: *"Tier-1 19/20, nguồn `p2e-continue-20261013/merged.csv`, jar commit `a544c996ac`"*.
2. **Mỗi lượt chạy một thư mục mới, có ngày, không ghi đè.** Kết quả cũ luôn còn để đối chiếu.
3. **Thống kê đúng mức.** Ghi khoảng tin cậy và p-value khi so sánh; không kết luận "nhân quả" khi p > 0,05. Áp dụng cho phần so sánh B0, B1, P (spec tuần 1, mục 5).
4. **Báo cáo viết bằng Markdown rồi xuất PDF**, lưu cả hai.
5. **Bảo vệ dữ liệu kết quả bằng harness**, không chỉ bằng lời dặn AI. Repo tham khảo có hook chặn xóa hoặc ghi đè thư mục kết quả.

## 2. Cấu trúc áp dụng

Kết quả nằm **trong repo code**, không tách repo:

```
C:\DACN\
├── docs/
│   ├── ket_qua.md                 ← nguồn duy nhất cho mọi con số trong báo cáo (từ tuần 5)
│   ├── tuan_N/kiem_thu/           ← kiểm thử từng tuần (đã có từ tuần 2)
│   └── To_chuc_ket_qua.md         ← file này
└── thuc_nghiem/
    └── ket_qua/
        └── 2026-11-10_B0-B1-P_seed42/   ← mỗi lượt chạy một thư mục, không ghi đè
```

Lý do: code và kết quả cùng một chỗ, cùng lịch sử commit, không bị lệch phiên bản.

Nếu giảng viên **yêu cầu** repo riêng: giữ nguyên cấu trúc trên, chuyển sang repo khác và ghi commit hash của repo code bên cạnh mỗi kết quả.

## 3. Quy tắc ghi kết quả

### 3.1. `docs/ket_qua.md`

- Mọi con số trong báo cáo lấy từ file này.
- Mỗi số có: giá trị, nguồn (đường dẫn file kết quả thô), commit code, ngày chạy.
- Số mới thay số cũ thì sửa tại chỗ, ghi lý do thay.

Mẫu một dòng:

| Chỉ số | B0 | B1 | P | Nguồn | Commit | Ngày |
|---|---|---|---|---|---|---|
| Precision@5 | … | … | … | `thuc_nghiem/ket_qua/2026-11-10_B0-B1-P_seed42/metrics.csv` | `abc1234` | 2026-11-10 |

### 3.2. Mỗi thư mục lượt chạy

Mỗi thư mục trong `thuc_nghiem/ket_qua/` có:

| File | Nội dung |
|---|---|
| `README.md` | Lệnh đã chạy, seed, commit code, cấu hình máy (CPU, RAM; NFR-03 yêu cầu), thời gian chạy |
| `metrics.csv` | Kết quả thô từng chỉ số |
| `log.txt` | Log lượt chạy |

Lượt chạy hỏng vẫn giữ thư mục và ghi lý do hỏng trong `README.md`.

### 3.3. Phiên bản code

Khi chạy thực nghiệm dùng cho báo cáo, gắn tag cho commit code:

```powershell
git tag -a v1.0-thuc-nghiem -m "Phiên bản code dùng cho thực nghiệm tuần 5"
```

## 4. Nộp cuối dự án

1. Gắn tag cho commit cuối, ví dụ `v1.0-bao-cao`.
2. Tạo GitHub Release từ tag đó, đính kèm PDF báo cáo và slide.
3. Video demo tải lên YouTube hoặc Google Drive, để link trong Release (GitHub không phù hợp với file lớn).
4. Kết quả khảo sát 15–20 sinh viên (tuần 8) chỉ lưu bản **đã ẩn danh**: bỏ tên, email, số điện thoại.

## 5. Lịch thực hiện

| Tuần | Việc |
|---|---|
| 3–4 | Tiếp tục `docs/tuan_N/kiem_thu/` như tuần 2 |
| 5 | Tạo `docs/ket_qua.md`, `thuc_nghiem/ket_qua/`; gắn tag phiên bản thực nghiệm; thêm hook chặn ghi đè thư mục kết quả |
| 6–8 | Mỗi lượt chạy hoặc kiểm thử mới: thêm thư mục, cập nhật `ket_qua.md` |
| 9 | Tag cuối, GitHub Release kèm PDF |
