# Kết quả thực nghiệm

Nguồn duy nhất cho mọi con số đưa vào báo cáo (`docs/To_chuc_ket_qua.md` §3.1, D72). Số ở chỗ khác lệch file này thì coi như sai. Số mới thay số cũ thì sửa tại chỗ và ghi lý do thay.

## Tuần 5: thực nghiệm thuật toán ghép cặp (spec 5)

- **Code:** tag `v1.0-thuc-nghiem`, commit `3a01bfc` (giai đoạn báo cáo). Giai đoạn chọn chạy ở commit `4d4bc65`; `backend/` của hai commit giống hệt nhau (`3a01bfc` chỉ thêm D69 và kết quả giai đoạn chọn).
- **Lượt chạy:** `thuc_nghiem/ket_qua/2026-11-14_chon-cau-hinh/` (seed 1–2) và `thuc_nghiem/ket_qua/2026-11-14_bao-cao/` (seed 3–5, `--trong-so lich --schedule tran3`). Lệnh, trạng thái, thời gian chạy ở `README.md` của từng thư mục.
- **Máy** (`README.md` của hai lượt chạy): CPU Intel64 Family 6 Model 154 Stepping 3, GenuineIntel; RAM 15.7 GB; Java 21.0.12.1+1-LTS.
- **Cách tính:** bảng 2, 4, 5, 6 ghi trung bình ± độ lệch chuẩn mẫu (chia $k - 1$, $k = 3$) của giá trị seed 3, 4, 5 trong `metrics.csv` (D62); kiểm lại bằng Excel với `AVERAGE` và `STDEV.S` trên 3 dòng seed. Bảng 1 ghi giá trị từng seed và trung bình cộng (D69). Bảng 3 chép từ `kiem_dinh.csv`. Bảng 7 chỉ có một seed.
- **Làm tròn:** chỉ số dạng tỷ lệ, hiệu và khoảng tin cậy 4 chữ số thập phân (bảng 1: 6 chữ số); số đếm 1 chữ số; latency 3 chữ số (ms); $z$ 2 chữ số; $p$ 4 chữ số, nhỏ hơn 0.0001 ghi `< 0.0001` (`kiem_dinh.csv` ghi `0.0` khi $p$ nhỏ hơn độ chính xác của `double`).
- **Tên:** phương án và chỉ số theo D72. Định nghĩa: Precision@5 hai bản (D58); NDCG@5 hai bản, `so_ca_idcg_0`, `so_nguoi_bi_loai` (D59); `coverage`, `ty_le_du_5_ung_vien`, `ty_le_co_nhan_2` (D60); latency (D61); chỉ số `_it_khung` (D70 d, kế hoạch tuần 5, bước 7). Phương án: spec 5.2, D56, D65–D68; bộ trọng số ở kế hoạch tuần 5, bước 6.
- **Mặc định của hệ thống không đổi (D69; giữ nguyên sau khi xem khoảng tin cậy, D101):** `P` là bộ trọng số mặc định với công thức `MIN`. `P_w_lich`, `P_sf_tran3` là cấu hình đã chọn, chỉ là kết quả để báo cáo.

### Ghi chú khi đọc số

- `so_ca_idcg_0` và `so_nguoi_bi_loai` chỉ phụ thuộc dữ liệu thật và nhãn (tập lý tưởng, D57), nên giống nhau ở mọi phương án và mọi mức nhiễu của cùng seed, cùng N (D64).
- `ty_le_co_nhan_2` xét cả danh sách, không phụ thuộc thứ tự, nên giống nhau ở các phương án dùng chung bước lọc (D57, D65).
- `ty_le_danh_sach_it_khung` giống nhau ở 3 công thức vì cả danh sách không đổi khi chỉ đổi thứ tự. So `ty_le_top5_it_khung` với nó để biết ứng viên khai 1–3 khung được đưa lên top 5 nhiều hơn hay ít hơn tỷ lệ của họ trong danh sách.
- Latency của `B0` thấp hơn hẳn các phương án khác vì `B0` có ít ứng viên hơn: tổng số ứng viên qua hai lượt (làm nóng và đo) là 303 064, so với 520 626 của `B1`, `R`, `P` (`log.txt` của lượt báo cáo). Ghi chú của `README.md` (theo D61) rằng latency của `B0`, `B1`, `R` gần `P` đúng với `B1`, `R`, không đúng với `B0`.

### 1. Chọn cấu hình (D69)

NDCG@5 bản (2) (`ndcg_at_5_loai_suy_bien`), N = 500, nhiễu mặc định. Trung bình = trung bình cộng giá trị của seed 1 và seed 2. Chọn riêng bộ trọng số tốt nhất (so `P` với ba `P_w_…`) và công thức tốt nhất (so `P` với hai `P_sf_…`).

Nguồn: `thuc_nghiem/ket_qua/2026-11-14_chon-cau-hinh/metrics.csv` · commit `4d4bc65` · ngày 2026-11-14

| Phương án | Cấu hình | Seed 1 | Seed 2 | Trung bình | Số người bản (2) |  |
| --- | --- | --- | --- | --- | --- | --- |
| `P` | mặc định (bộ mặc định, `MIN`) | 0.601900 | 0.583375 | 0.592638 | 370; 363 |  |
| `P_w_ky_nang` | bộ trọng số `ky_nang`, `MIN` | 0.589575 | 0.570963 | 0.580269 | 370; 363 |  |
| `P_w_lich` | bộ trọng số `lich`, `MIN` | 0.602840 | 0.589234 | 0.596037 | 370; 363 | **chọn** |
| `P_w_uy_tin` | bộ trọng số `uy_tin`, `MIN` | 0.603026 | 0.586477 | 0.594751 | 370; 363 |  |
| `P_sf_jaccard` | bộ mặc định, công thức `jaccard` | 0.586831 | 0.566406 | 0.576618 | 370; 363 |  |
| `P_sf_tran3` | bộ mặc định, công thức `tran3` | 0.618714 | 0.600202 | 0.609458 | 370; 363 | **chọn** |

### 2. Bảng chính (D70 a)

N = 500, nhiễu mặc định, seed 3–5: trung bình ± độ lệch chuẩn trên 3 seed.

Nguồn: `thuc_nghiem/ket_qua/2026-11-14_bao-cao/metrics.csv` · commit `3a01bfc` · ngày 2026-11-14

| Chỉ số | `B0` | `B1` | `R` | `P` | `P_bo_level` | `P_bo_schedule` | `P_bo_mode` | `P_w_lich` | `P_sf_tran3` |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `precision_at_5` | 0.9548 ± 0.0115 | 0.9552 ± 0.0110 | 0.9152 ± 0.0139 | 0.9649 ± 0.0132 | 0.9551 ± 0.0128 | 0.9535 ± 0.0122 | 0.9649 ± 0.0126 | 0.9691 ± 0.0118 | 0.9679 ± 0.0111 |
| `precision_at_5_nghiem_ngat` | 0.1539 ± 0.0030 | 0.1581 ± 0.0026 | 0.0817 ± 0.0044 | 0.1627 ± 0.0022 | 0.1489 ± 0.0070 | 0.1647 ± 0.0052 | 0.1573 ± 0.0012 | 0.1637 ± 0.0028 | 0.1825 ± 0.0018 |
| `ndcg_at_5_tat_ca` | 0.6872 ± 0.0138 | 0.6901 ± 0.0130 | 0.6115 ± 0.0190 | 0.7025 ± 0.0156 | 0.6867 ± 0.0175 | 0.6957 ± 0.0155 | 0.6980 ± 0.0144 | 0.7059 ± 0.0138 | 0.7191 ± 0.0145 |
| `ndcg_at_5_loai_suy_bien` | 0.5815 ± 0.0144 | 0.5858 ± 0.0154 | 0.4776 ± 0.0221 | 0.6010 ± 0.0186 | 0.5780 ± 0.0220 | 0.5937 ± 0.0188 | 0.5949 ± 0.0169 | 0.6042 ± 0.0169 | 0.6228 ± 0.0172 |
| `so_ca_idcg_0` | 0.0 ± 0.0 | 0.0 ± 0.0 | 0.0 ± 0.0 | 0.0 ± 0.0 | 0.0 ± 0.0 | 0.0 ± 0.0 | 0.0 ± 0.0 | 0.0 ± 0.0 | 0.0 ± 0.0 |
| `so_nguoi_bi_loai` | 136.3 ± 1.5 | 136.3 ± 1.5 | 136.3 ± 1.5 | 136.3 ± 1.5 | 136.3 ± 1.5 | 136.3 ± 1.5 | 136.3 ± 1.5 | 136.3 ± 1.5 | 136.3 ± 1.5 |
| `coverage` | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 |
| `ty_le_du_5_ung_vien` | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 |
| `ty_le_co_nhan_2` | 0.6147 ± 0.0153 | 0.6147 ± 0.0153 | 0.6147 ± 0.0153 | 0.6147 ± 0.0153 | 0.6147 ± 0.0153 | 0.6147 ± 0.0153 | 0.6147 ± 0.0153 | 0.6147 ± 0.0153 | 0.6147 ± 0.0153 |

### 3. Kiểm định theo cặp (D62)

N = 500, nhiễu mặc định, gộp người dùng của seed 3, 4, 5, chỉ người thuộc bản (2). Hiệu = phương án thứ nhất − phương án thứ hai, tính trên từng người. Khoảng tin cậy 95% bằng bootstrap ghép cặp 2000 lần; $p$ của Wilcoxon theo cặp (xấp xỉ chuẩn, hai phía). **8 phép so sánh, không hiệu chỉnh đa so sánh** ($p$ hiệu chỉnh Holm–Bonferroni ở bảng 12, D100). `P` ở đây là cấu hình mặc định, không phải cấu hình đã chọn.

Nguồn: `thuc_nghiem/ket_qua/2026-11-14_bao-cao/kiem_dinh.csv` · commit `3a01bfc` · ngày 2026-11-14

| So sánh | Chỉ số | Số cặp | Hiệu trung bình | KTC 95% | n Wilcoxon | W+ | z | p | Ghi chú |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `P` − `B1` | `ndcg_at_5` | 1091 | 0.0152 | [0.0038, 0.0261] | 860 | 203574.5 | 2.53 | 0.0113 |  |
| `P` − `B1` | `precision_at_5_nghiem_ngat` | 1091 | 0.0062 | [-0.0077, 0.0196] | 592 | 90449.5 | 0.68 | 0.4992 |  |
| `P` − `P_bo_level` | `ndcg_at_5` | 1091 | 0.0230 | [0.0180, 0.0282] | 644 | 146012.0 | 8.93 | < 0.0001 |  |
| `P` − `P_bo_level` | `precision_at_5_nghiem_ngat` | 1091 | 0.0189 | [0.0119, 0.0255] | 285 | 27071.0 | 5.30 | < 0.0001 |  |
| `P` − `P_bo_schedule` | `ndcg_at_5` | 1091 | 0.0073 | [0.0034, 0.0111] | 577 | 99374.5 | 3.99 | < 0.0001 |  |
| `P` − `P_bo_schedule` | `precision_at_5_nghiem_ngat` | 1091 | -0.0027 | [-0.0088, 0.0031] | 226 | 11977.0 | -0.97 | 0.3335 |  |
| `P` − `P_bo_mode` | `ndcg_at_5` | 1091 | 0.0062 | [0.0045, 0.0079] | 226 | 19724.5 | 7.02 | < 0.0001 |  |
| `P` − `P_bo_mode` | `precision_at_5_nghiem_ngat` | 1091 | 0.0073 | [0.0046, 0.0101] | 58 | 1445.5 | 5.25 | < 0.0001 |  |

### 4. Theo quy mô (D70 b)

Nhiễu mặc định, seed 3–5: trung bình ± độ lệch chuẩn trên 3 seed. Dòng N = 500 trùng bảng chính.

Nguồn: `thuc_nghiem/ket_qua/2026-11-14_bao-cao/metrics.csv` · commit `3a01bfc` · ngày 2026-11-14

| Chỉ số | N | `B0` | `B1` | `R` | `P` |
| --- | --- | --- | --- | --- | --- |
| `precision_at_5` | 50 | 0.7933 ± 0.0816 | 0.8707 ± 0.0546 | 0.8347 ± 0.0652 | 0.8613 ± 0.0506 |
| `precision_at_5` | 100 | 0.9253 ± 0.0257 | 0.9420 ± 0.0284 | 0.9027 ± 0.0162 | 0.9300 ± 0.0330 |
| `precision_at_5` | 500 | 0.9548 ± 0.0115 | 0.9552 ± 0.0110 | 0.9152 ± 0.0139 | 0.9649 ± 0.0132 |
| `precision_at_5` | 1000 | 0.9496 ± 0.0070 | 0.9523 ± 0.0088 | 0.9101 ± 0.0059 | 0.9683 ± 0.0035 |
| `precision_at_5_nghiem_ngat` | 50 | 0.1333 ± 0.0543 | 0.1280 ± 0.0560 | 0.0973 ± 0.0370 | 0.1147 ± 0.0671 |
| `precision_at_5_nghiem_ngat` | 100 | 0.1453 ± 0.0090 | 0.1413 ± 0.0129 | 0.0833 ± 0.0140 | 0.1293 ± 0.0287 |
| `precision_at_5_nghiem_ngat` | 500 | 0.1539 ± 0.0030 | 0.1581 ± 0.0026 | 0.0817 ± 0.0044 | 0.1627 ± 0.0022 |
| `precision_at_5_nghiem_ngat` | 1000 | 0.1613 ± 0.0249 | 0.1634 ± 0.0193 | 0.0902 ± 0.0071 | 0.1713 ± 0.0092 |
| `ndcg_at_5_tat_ca` | 50 | 0.7905 ± 0.0015 | 0.8340 ± 0.0204 | 0.7678 ± 0.0062 | 0.8165 ± 0.0099 |
| `ndcg_at_5_tat_ca` | 100 | 0.7868 ± 0.0189 | 0.7957 ± 0.0153 | 0.7145 ± 0.0026 | 0.7774 ± 0.0210 |
| `ndcg_at_5_tat_ca` | 500 | 0.6872 ± 0.0138 | 0.6901 ± 0.0130 | 0.6115 ± 0.0190 | 0.7025 ± 0.0156 |
| `ndcg_at_5_tat_ca` | 1000 | 0.6762 ± 0.0122 | 0.6780 ± 0.0120 | 0.6057 ± 0.0094 | 0.6950 ± 0.0129 |
| `ndcg_at_5_loai_suy_bien` | 50 | 0.7449 ± 0.0372 | 0.7737 ± 0.0550 | 0.6717 ± 0.0333 | 0.7403 ± 0.0440 |
| `ndcg_at_5_loai_suy_bien` | 100 | 0.7067 ± 0.0182 | 0.7124 ± 0.0218 | 0.5967 ± 0.0111 | 0.6902 ± 0.0295 |
| `ndcg_at_5_loai_suy_bien` | 500 | 0.5815 ± 0.0144 | 0.5858 ± 0.0154 | 0.4776 ± 0.0221 | 0.6010 ± 0.0186 |
| `ndcg_at_5_loai_suy_bien` | 1000 | 0.5655 ± 0.0212 | 0.5680 ± 0.0185 | 0.4665 ± 0.0132 | 0.5873 ± 0.0154 |
| `so_ca_idcg_0` | 50 | 0.7 ± 1.2 | 0.7 ± 1.2 | 0.7 ± 1.2 | 0.7 ± 1.2 |
| `so_ca_idcg_0` | 100 | 0.0 ± 0.0 | 0.0 ± 0.0 | 0.0 ± 0.0 | 0.0 ± 0.0 |
| `so_ca_idcg_0` | 500 | 0.0 ± 0.0 | 0.0 ± 0.0 | 0.0 ± 0.0 | 0.0 ± 0.0 |
| `so_ca_idcg_0` | 1000 | 0.0 ± 0.0 | 0.0 ± 0.0 | 0.0 ± 0.0 | 0.0 ± 0.0 |
| `so_nguoi_bi_loai` | 50 | 19.3 ± 7.6 | 19.3 ± 7.6 | 19.3 ± 7.6 | 19.3 ± 7.6 |
| `so_nguoi_bi_loai` | 100 | 32.0 ± 2.6 | 32.0 ± 2.6 | 32.0 ± 2.6 | 32.0 ± 2.6 |
| `so_nguoi_bi_loai` | 500 | 136.3 ± 1.5 | 136.3 ± 1.5 | 136.3 ± 1.5 | 136.3 ± 1.5 |
| `so_nguoi_bi_loai` | 1000 | 277.7 ± 4.5 | 277.7 ± 4.5 | 277.7 ± 4.5 | 277.7 ± 4.5 |
| `coverage` | 50 | 0.9867 ± 0.0231 | 0.9933 ± 0.0115 | 0.9933 ± 0.0115 | 0.9933 ± 0.0115 |
| `coverage` | 100 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 |
| `coverage` | 500 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 |
| `coverage` | 1000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 |
| `ty_le_du_5_ung_vien` | 50 | 0.6400 ± 0.1510 | 0.8333 ± 0.1007 | 0.8333 ± 0.1007 | 0.8333 ± 0.1007 |
| `ty_le_du_5_ung_vien` | 100 | 0.8900 ± 0.0458 | 0.9567 ± 0.0493 | 0.9567 ± 0.0493 | 0.9567 ± 0.0493 |
| `ty_le_du_5_ung_vien` | 500 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 |
| `ty_le_du_5_ung_vien` | 1000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 |
| `ty_le_co_nhan_2` | 50 | 0.4467 ± 0.1172 | 0.4467 ± 0.1172 | 0.4467 ± 0.1172 | 0.4467 ± 0.1172 |
| `ty_le_co_nhan_2` | 100 | 0.5133 ± 0.0208 | 0.5133 ± 0.0208 | 0.5133 ± 0.0208 | 0.5133 ± 0.0208 |
| `ty_le_co_nhan_2` | 500 | 0.6147 ± 0.0153 | 0.6147 ± 0.0153 | 0.6147 ± 0.0153 | 0.6147 ± 0.0153 |
| `ty_le_co_nhan_2` | 1000 | 0.6190 ± 0.0151 | 0.6190 ± 0.0151 | 0.6190 ± 0.0151 | 0.6190 ± 0.0151 |

### 5. Theo mức nhiễu (D70 c)

N = 500, seed 3–5: trung bình ± độ lệch chuẩn trên 3 seed. Dòng `mac_dinh` trùng bảng chính.

Nguồn: `thuc_nghiem/ket_qua/2026-11-14_bao-cao/metrics.csv` · commit `3a01bfc` · ngày 2026-11-14

| Chỉ số | Mức nhiễu | `B1` | `P` | `P_bo_level` | `P_bo_schedule` | `P_bo_mode` |
| --- | --- | --- | --- | --- | --- | --- |
| `precision_at_5` | `khong` | 1.0000 ± 0.0000 | 0.9975 ± 0.0030 | 0.9888 ± 0.0030 | 0.9985 ± 0.0018 | 0.9976 ± 0.0028 |
| `precision_at_5` | `mac_dinh` | 0.9552 ± 0.0110 | 0.9649 ± 0.0132 | 0.9551 ± 0.0128 | 0.9535 ± 0.0122 | 0.9649 ± 0.0126 |
| `precision_at_5` | `cao` | 0.9275 ± 0.0126 | 0.9367 ± 0.0139 | 0.9235 ± 0.0197 | 0.9281 ± 0.0135 | 0.9365 ± 0.0136 |
| `precision_at_5_nghiem_ngat` | `khong` | 0.1601 ± 0.0018 | 0.1863 ± 0.0057 | 0.1668 ± 0.0092 | 0.1720 ± 0.0091 | 0.1803 ± 0.0050 |
| `precision_at_5_nghiem_ngat` | `mac_dinh` | 0.1581 ± 0.0026 | 0.1627 ± 0.0022 | 0.1489 ± 0.0070 | 0.1647 ± 0.0052 | 0.1573 ± 0.0012 |
| `precision_at_5_nghiem_ngat` | `cao` | 0.1453 ± 0.0074 | 0.1455 ± 0.0028 | 0.1309 ± 0.0108 | 0.1464 ± 0.0058 | 0.1377 ± 0.0040 |
| `ndcg_at_5_tat_ca` | `khong` | 0.7145 ± 0.0068 | 0.7373 ± 0.0122 | 0.7177 ± 0.0142 | 0.7258 ± 0.0144 | 0.7323 ± 0.0099 |
| `ndcg_at_5_tat_ca` | `mac_dinh` | 0.6901 ± 0.0130 | 0.7025 ± 0.0156 | 0.6867 ± 0.0175 | 0.6957 ± 0.0155 | 0.6980 ± 0.0144 |
| `ndcg_at_5_tat_ca` | `cao` | 0.6669 ± 0.0145 | 0.6730 ± 0.0139 | 0.6554 ± 0.0161 | 0.6704 ± 0.0136 | 0.6670 ± 0.0125 |
| `ndcg_at_5_loai_suy_bien` | `khong` | 0.6075 ± 0.0091 | 0.6389 ± 0.0158 | 0.6119 ± 0.0183 | 0.6231 ± 0.0185 | 0.6320 ± 0.0128 |
| `ndcg_at_5_loai_suy_bien` | `mac_dinh` | 0.5858 ± 0.0154 | 0.6010 ± 0.0186 | 0.5780 ± 0.0220 | 0.5937 ± 0.0188 | 0.5949 ± 0.0169 |
| `ndcg_at_5_loai_suy_bien` | `cao` | 0.5610 ± 0.0205 | 0.5669 ± 0.0164 | 0.5435 ± 0.0186 | 0.5639 ± 0.0165 | 0.5582 ± 0.0143 |
| `so_ca_idcg_0` | `khong` | 0.0 ± 0.0 | 0.0 ± 0.0 | 0.0 ± 0.0 | 0.0 ± 0.0 | 0.0 ± 0.0 |
| `so_ca_idcg_0` | `mac_dinh` | 0.0 ± 0.0 | 0.0 ± 0.0 | 0.0 ± 0.0 | 0.0 ± 0.0 | 0.0 ± 0.0 |
| `so_ca_idcg_0` | `cao` | 0.0 ± 0.0 | 0.0 ± 0.0 | 0.0 ± 0.0 | 0.0 ± 0.0 | 0.0 ± 0.0 |
| `so_nguoi_bi_loai` | `khong` | 136.3 ± 1.5 | 136.3 ± 1.5 | 136.3 ± 1.5 | 136.3 ± 1.5 | 136.3 ± 1.5 |
| `so_nguoi_bi_loai` | `mac_dinh` | 136.3 ± 1.5 | 136.3 ± 1.5 | 136.3 ± 1.5 | 136.3 ± 1.5 | 136.3 ± 1.5 |
| `so_nguoi_bi_loai` | `cao` | 136.3 ± 1.5 | 136.3 ± 1.5 | 136.3 ± 1.5 | 136.3 ± 1.5 | 136.3 ± 1.5 |
| `coverage` | `khong` | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 |
| `coverage` | `mac_dinh` | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 |
| `coverage` | `cao` | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 |
| `ty_le_du_5_ung_vien` | `khong` | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 |
| `ty_le_du_5_ung_vien` | `mac_dinh` | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 |
| `ty_le_du_5_ung_vien` | `cao` | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 | 1.0000 ± 0.0000 |
| `ty_le_co_nhan_2` | `khong` | 0.6167 ± 0.0167 | 0.6167 ± 0.0167 | 0.6167 ± 0.0167 | 0.6167 ± 0.0167 | 0.6167 ± 0.0167 |
| `ty_le_co_nhan_2` | `mac_dinh` | 0.6147 ± 0.0153 | 0.6147 ± 0.0153 | 0.6147 ± 0.0153 | 0.6147 ± 0.0153 | 0.6147 ± 0.0153 |
| `ty_le_co_nhan_2` | `cao` | 0.6093 ± 0.0140 | 0.6093 ± 0.0140 | 0.6093 ± 0.0140 | 0.6093 ± 0.0140 | 0.6093 ± 0.0140 |

### 6. Nhóm khai ít khung (D70 d)

Người dùng có lịch tự khai 1–3 khung; N = 500, nhiễu mặc định, seed 3–5: trung bình ± độ lệch chuẩn trên 3 seed. `ty_le_top5_it_khung` và `ty_le_danh_sach_it_khung` gộp mọi vị trí của mọi người dùng (kế hoạch tuần 5, bước 7).

Nguồn: `thuc_nghiem/ket_qua/2026-11-14_bao-cao/metrics.csv` · commit `3a01bfc` · ngày 2026-11-14

| Chỉ số | `P` | `P_sf_jaccard` | `P_sf_tran3` |
| --- | --- | --- | --- |
| `precision_at_5_it_khung` | 0.9411 ± 0.0292 | 0.9441 ± 0.0319 | 0.9432 ± 0.0281 |
| `precision_at_5_nghiem_ngat_it_khung` | 0.1303 ± 0.0213 | 0.1102 ± 0.0209 | 0.1243 ± 0.0187 |
| `ndcg_at_5_tat_ca_it_khung` | 0.5971 ± 0.0272 | 0.5787 ± 0.0232 | 0.5941 ± 0.0230 |
| `ndcg_at_5_loai_suy_bien_it_khung` | 0.5176 ± 0.0321 | 0.4936 ± 0.0337 | 0.5128 ± 0.0315 |
| `ty_le_top5_it_khung` | 0.1128 ± 0.0213 | 0.0859 ± 0.0289 | 0.0743 ± 0.0221 |
| `ty_le_danh_sach_it_khung` | 0.0972 ± 0.0050 | 0.0972 ± 0.0050 | 0.0972 ± 0.0050 |
| Số người trong nhóm (`so_nguoi_tinh` của `precision_at_5_it_khung`) | 66.0 ± 1.0 | 66.0 ± 1.0 | 66.0 ± 1.0 |
| Số người trong nhóm thuộc bản (2) (`so_nguoi_tinh` của `ndcg_at_5_loai_suy_bien_it_khung`) | 54.0 ± 2.0 | 54.0 ± 2.0 | 54.0 ± 2.0 |

### 7. Latency (D61, NFR-03)

Đo trong bộ nhớ, seed 3, N = 1000, nhiễu mặc định, đơn luồng, sau một lượt làm nóng; thời gian tạo danh sách của từng người (lọc, chấm điểm, xếp hạng). Một seed nên không có độ lệch chuẩn.

Nguồn: `thuc_nghiem/ket_qua/2026-11-14_bao-cao/metrics.csv` · commit `3a01bfc` · ngày 2026-11-14

| Phương án | Trung bình (ms) | p95 (ms) | Số người đo |
| --- | --- | --- | --- |
| `B0` | 0.671 | 1.466 | 1000 |
| `B1` | 1.486 | 3.381 | 1000 |
| `R` | 1.543 | 3.531 | 1000 |
| `P` | 1.258 | 2.799 | 1000 |

## Bổ sung theo nhận xét báo cáo tuần 5 (2026-11-22)

Trả lời các câu hỏi của giảng viên về phương pháp thống kê và diễn giải kết quả (quyết định tuần 6, D97–D100).

- **Code:** commit `10ecc31` (giai đoạn `bo-sung` của module `evaluation`; lõi `matching` không đổi so với tag `v1.0-thuc-nghiem`).
- **Lượt chạy:** `thuc_nghiem/ket_qua/2026-11-22_bo-sung/` (seed 3–5, cùng N chính, các quy mô, nhiễu mặc định và số lần bootstrap như lượt báo cáo). Chạy 22.0 giây, cùng máy với lượt báo cáo (`README.md` của lượt chạy).
- **Phân tích sau khi chạy:** `thuc_nghiem/phan_tich/bo_sung_tuan_6.py` (hiệu chỉnh Holm–Bonferroni, kiểm định cấu hình đã chọn). Chạy từ thư mục gốc; mã thoát 0 khi mọi bước tự kiểm đạt.
- **Tái lập:** cùng dữ liệu, cùng lõi nên lượt bổ sung lặp lại đúng lượt báo cáo: 216/216 ô chỉ số của `B1`, `P` trùng `metrics.csv` của lượt báo cáo; hai phép `P` − `B1` ở N = 500 trùng từng ô với `kiem_dinh.csv` của lượt báo cáo.
- **Phương án mới:** `P_mot_ds` = chấm điểm như `P` (bộ mặc định, công thức ScheduleFit của lõi) nhưng gộp hai nhóm và xếp chung theo D chiều u học từ v (D97). `P_bo_<thành phần>_tl` = bỏ thành phần, chia lại theo tỉ lệ (D98). Chỉ số `_hai_chieu`, `_mot_chieu` = chỉ số trong từng nhóm (D99).
- **Cách tính:** như phần tuần 5 (trung bình ± độ lệch chuẩn mẫu trên 3 seed; kiểm định gộp người dùng của 3 seed theo D62). Thêm: kiểm định từng seed riêng; $p$ hiệu chỉnh Holm–Bonferroni theo hai họ (D100): **họ 1** = 8 phép của lượt báo cáo (bảng 3), **họ 2** = 38 phép gộp 3 seed của lượt bổ sung.

### 8. Theo quy mô, có `P` một danh sách (D97)

Nguồn: `thuc_nghiem/ket_qua/2026-11-22_bo-sung/metrics.csv` · commit `10ecc31` · ngày 2026-11-22

| N | Chỉ số | `B1` | `P` | `P_mot_ds` |
| --- | --- | --- | --- | --- |
| 50 | NDCG@5 (2) | 0.7737 ± 0.0550 | 0.7403 ± 0.0440 | 0.8764 ± 0.0225 |
| 50 | P@5 nghiêm ngặt | 0.1280 ± 0.0560 | 0.1147 ± 0.0671 | 0.1547 ± 0.0776 |
| 100 | NDCG@5 (2) | 0.7124 ± 0.0218 | 0.6902 ± 0.0295 | 0.8013 ± 0.0224 |
| 100 | P@5 nghiêm ngặt | 0.1413 ± 0.0129 | 0.1293 ± 0.0287 | 0.2067 ± 0.0289 |
| 500 | NDCG@5 (2) | 0.5858 ± 0.0154 | 0.6010 ± 0.0186 | 0.6916 ± 0.0160 |
| 500 | P@5 nghiêm ngặt | 0.1581 ± 0.0026 | 0.1627 ± 0.0022 | 0.2591 ± 0.0173 |
| 1000 | NDCG@5 (2) | 0.5680 ± 0.0185 | 0.5873 ± 0.0154 | 0.6757 ± 0.0149 |
| 1000 | P@5 nghiêm ngặt | 0.1634 ± 0.0193 | 0.1713 ± 0.0092 | 0.2632 ± 0.0137 |

### 9. Kiểm định theo quy mô (D97, D100)

Gộp người dùng của 3 seed, người thuộc bản (2). Cột cuối là dấu của hiệu trung bình khi chạy riêng từng seed (seed 3, 4, 5); "0" là hiệu đúng bằng 0.

Nguồn: `thuc_nghiem/ket_qua/2026-11-22_bo-sung/kiem_dinh.csv` · commit `10ecc31` · ngày 2026-11-22

| N | So sánh | Chỉ số | Số cặp | Hiệu | KTC 95% | p | p Holm (họ 2) | Dấu seed 3, 4, 5 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 50 | `P` − `B1` | NDCG@5 | 92 | -0.0304 | [-0.0657, 0.0044] | 0.1322 | 0.5604 | − − − |
| 50 | `P` − `B1` | P@5 ng. | 92 | -0.0217 | [-0.0522, 0.0065] | 0.1790 | 0.5604 | − − − |
| 50 | `P_mot_ds` − `B1` | NDCG@5 | 92 | 0.1044 | [0.0753, 0.1349] | < 0.0001 | < 0.0001 | + + + |
| 50 | `P_mot_ds` − `B1` | P@5 ng. | 92 | 0.0435 | [0.0174, 0.0717] | 0.0018 | 0.0164 | + 0 + |
| 50 | `P` − `P_mot_ds` | NDCG@5 | 92 | -0.1348 | [-0.1658, -0.1036] | < 0.0001 | < 0.0001 | − − − |
| 50 | `P` − `P_mot_ds` | P@5 ng. | 92 | -0.0652 | [-0.0891, -0.0435] | < 0.0001 | < 0.0001 | − − − |
| 100 | `P` − `B1` | NDCG@5 | 204 | -0.0220 | [-0.0475, 0.0026] | 0.0934 | 0.5604 | − − − |
| 100 | `P` − `B1` | P@5 ng. | 204 | -0.0176 | [-0.0412, 0.0059] | 0.1812 | 0.5604 | − − + |
| 100 | `P_mot_ds` − `B1` | NDCG@5 | 204 | 0.0890 | [0.0699, 0.1097] | < 0.0001 | < 0.0001 | + + + |
| 100 | `P_mot_ds` − `B1` | P@5 ng. | 204 | 0.0961 | [0.0735, 0.1196] | < 0.0001 | < 0.0001 | + + + |
| 100 | `P` − `P_mot_ds` | NDCG@5 | 204 | -0.1110 | [-0.1377, -0.0855] | < 0.0001 | < 0.0001 | − − − |
| 100 | `P` − `P_mot_ds` | P@5 ng. | 204 | -0.1137 | [-0.1402, -0.0882] | < 0.0001 | < 0.0001 | − − − |
| 500 | `P` − `B1` | NDCG@5 | 1091 | 0.0152 | [0.0038, 0.0261] | 0.0113 | 0.0904 | + + + |
| 500 | `P` − `B1` | P@5 ng. | 1091 | 0.0062 | [-0.0077, 0.0196] | 0.4992 | 0.5604 | + + + |
| 500 | `P_mot_ds` − `B1` | NDCG@5 | 1091 | 0.1057 | [0.0957, 0.1153] | < 0.0001 | < 0.0001 | + + + |
| 500 | `P_mot_ds` − `B1` | P@5 ng. | 1091 | 0.1388 | [0.1265, 0.1518] | < 0.0001 | < 0.0001 | + + + |
| 500 | `P` − `P_mot_ds` | NDCG@5 | 1091 | -0.0905 | [-0.1027, -0.0779] | < 0.0001 | < 0.0001 | − − − |
| 500 | `P` − `P_mot_ds` | P@5 ng. | 1091 | -0.1325 | [-0.1487, -0.1170] | < 0.0001 | < 0.0001 | − − − |
| 1000 | `P` − `B1` | NDCG@5 | 2167 | 0.0193 | [0.0121, 0.0272] | < 0.0001 | < 0.0001 | + + + |
| 1000 | `P` − `B1` | P@5 ng. | 2167 | 0.0109 | [0.0018, 0.0211] | 0.0476 | 0.3329 | + − + |
| 1000 | `P_mot_ds` − `B1` | NDCG@5 | 2167 | 0.1077 | [0.1006, 0.1149] | < 0.0001 | < 0.0001 | + + + |
| 1000 | `P_mot_ds` − `B1` | P@5 ng. | 2167 | 0.1382 | [0.1287, 0.1479] | < 0.0001 | < 0.0001 | + + + |
| 1000 | `P` − `P_mot_ds` | NDCG@5 | 2167 | -0.0884 | [-0.0971, -0.0798] | < 0.0001 | < 0.0001 | − − − |
| 1000 | `P` − `P_mot_ds` | P@5 ng. | 2167 | -0.1273 | [-0.1383, -0.1161] | < 0.0001 | < 0.0001 | − − − |

### 10. Bỏ từng thành phần: chia đều và chia theo tỉ lệ (D66, D98)

N = 500, nhiễu mặc định. Cột "chia đều" lấy từ lượt báo cáo (bảng 2, bảng 3), cột "theo tỉ lệ" từ lượt bổ sung. Trong ngoặc là $p$ Holm.

Nguồn: `metrics.csv`, `kiem_dinh.csv` của `2026-11-14_bao-cao/` và `2026-11-22_bo-sung/`

| Bỏ | NDCG@5 (2), chia đều | NDCG@5 (2), theo tỉ lệ | `P` − chia đều: hiệu NDCG@5 (họ 1) | `P` − theo tỉ lệ: hiệu NDCG@5 (họ 2) | `P` − chia đều: hiệu P@5 ng. (họ 1) | `P` − theo tỉ lệ: hiệu P@5 ng. (họ 2) |
| --- | --- | --- | --- | --- | --- | --- |
| LevelFit | 0.5780 ± 0.0220 | 0.5737 ± 0.0221 | 0.0230 (< 0.0001) | 0.0273 (< 0.0001) | 0.0189 (< 0.0001) | 0.0260 (< 0.0001) |
| ScheduleFit | 0.5937 ± 0.0188 | 0.5888 ± 0.0188 | 0.0073 (0.0003) | 0.0123 (< 0.0001) | -0.0027 (0.6670) | 0.0049 (0.5604) |
| ModeFit | 0.5949 ± 0.0169 | 0.5938 ± 0.0170 | 0.0062 (< 0.0001) | 0.0072 (< 0.0001) | 0.0073 (< 0.0001) | 0.0079 (< 0.0001) |
| (tham chiếu `P`) | 0.6010 ± 0.0186 | | | | | |

### 11. Chỉ số trong từng nhóm (D99)

N = 500, nhiễu mặc định. Tập lý tưởng của nhóm là chính các ứng viên của nhóm, nên chỉ số đo thứ tự xếp trong nhóm. Số người có ứng viên trong nhóm và thuộc bản (2), seed 3/4/5: hai chiều 420, 409, 406; một chiều 436, 436, 419. Trong nhóm một chiều, `P` và `P_mot_ds` xếp giống hệt nhau (đều theo D chiều u học từ v) nên số trùng nhau. Trong ngoặc là $p$ Holm (họ 2).

Nguồn: `thuc_nghiem/ket_qua/2026-11-22_bo-sung/metrics.csv`, `kiem_dinh.csv` · commit `10ecc31` · ngày 2026-11-22

| Nhóm | Chỉ số | `B1` | `P` | `P_mot_ds` | `P` − `B1` | `P_mot_ds` − `B1` |
| --- | --- | --- | --- | --- | --- | --- |
| Hai chiều | NDCG@5 (2) | 0.6987 ± 0.0082 | 0.7301 ± 0.0197 | 0.7886 ± 0.0147 | 0.0313 (< 0.0001) | 0.0899 (< 0.0001) |
| Hai chiều | P@5 ng. | 0.1411 ± 0.0054 | 0.1611 ± 0.0002 | 0.2179 ± 0.0021 | 0.0243 (< 0.0001) | 0.0933 (< 0.0001) |
| Một chiều | NDCG@5 (2) | 0.6626 ± 0.0097 | 0.7537 ± 0.0175 | 0.7537 ± 0.0175 | 0.0913 (< 0.0001) | 0.0913 (< 0.0001) |
| Một chiều | P@5 ng. | 0.1452 ± 0.0051 | 0.2369 ± 0.0131 | 0.2369 ± 0.0131 | 0.1066 (< 0.0001) | 0.1066 (< 0.0001) |

### 12. Hiệu chỉnh kiểm định bội cho 8 phép của bảng 3 (D100, họ 1)

Holm–Bonferroni trên 8 giá trị $p$ của bảng 3. Các phép có ý nghĩa ở mức 0.05 trước hiệu chỉnh vẫn có ý nghĩa sau hiệu chỉnh. Nếu tính phép `P` − `B1` (NDCG@5) trong họ 2 lớn hơn (38 phép, bảng 9) thì $p$ Holm là 0.0904.

Nguồn: `thuc_nghiem/ket_qua/2026-11-14_bao-cao/kiem_dinh.csv`, tính bằng `thuc_nghiem/phan_tich/bo_sung_tuan_6.py` · ngày 2026-11-22

| So sánh | Chỉ số | p | p Holm |
| --- | --- | --- | --- |
| `P` − `B1` | `ndcg_at_5` | 0.0113 | 0.0339 |
| `P` − `B1` | `precision_at_5_nghiem_ngat` | 0.4992 | 0.6670 |
| `P` − `P_bo_level` | `ndcg_at_5` | < 0.0001 | < 0.0001 |
| `P` − `P_bo_level` | `precision_at_5_nghiem_ngat` | < 0.0001 | < 0.0001 |
| `P` − `P_bo_schedule` | `ndcg_at_5` | < 0.0001 | 0.0003 |
| `P` − `P_bo_schedule` | `precision_at_5_nghiem_ngat` | 0.3335 | 0.6670 |
| `P` − `P_bo_mode` | `ndcg_at_5` | < 0.0001 | < 0.0001 |
| `P` − `P_bo_mode` | `precision_at_5_nghiem_ngat` | < 0.0001 | < 0.0001 |

### 13. `P` − `B1` ở N = 500 theo từng seed (D100)

Mỗi seed là một bộ dữ liệu sinh độc lập; người dùng trong cùng một bộ dùng chung tập ứng viên nên không hoàn toàn độc lập với nhau.

Nguồn: `thuc_nghiem/ket_qua/2026-11-22_bo-sung/kiem_dinh.csv` · commit `10ecc31` · ngày 2026-11-22

| Seed | Chỉ số | Số cặp | Hiệu | KTC 95% | p |
| --- | --- | --- | --- | --- | --- |
| 3, 4, 5 | NDCG@5 | 1091 | 0.0152 | [0.0038, 0.0261] | 0.0113 |
| 3 | NDCG@5 | 365 | 0.0053 | [-0.0126, 0.0226] | 0.7043 |
| 4 | NDCG@5 | 362 | 0.0237 | [0.0031, 0.0444] | 0.0236 |
| 5 | NDCG@5 | 364 | 0.0167 | [-0.0017, 0.0361] | 0.0894 |
| 3, 4, 5 | P@5 nghiêm ngặt | 1091 | 0.0062 | [-0.0077, 0.0196] | 0.4992 |
| 3 | P@5 nghiêm ngặt | 365 | 0.0016 | [-0.0181, 0.0225] | 0.9707 |
| 4 | P@5 nghiêm ngặt | 362 | 0.0138 | [-0.0099, 0.0387] | 0.3386 |
| 5 | P@5 nghiêm ngặt | 364 | 0.0033 | [-0.0203, 0.0264] | 0.8957 |

### 14. Cấu hình đã chọn so với `P` (kiểm định sau khi chạy, D101)

N = 500, nhiễu mặc định, gộp seed 3–5, người thuộc bản (2), lấy từ `per_user.csv` của lượt báo cáo (cấu hình đã chọn có trong bảng chính). Wilcoxon theo đúng công thức D62, tự kiểm bằng cách tính lại `P` − `B1`: ra đúng $W^+$ và $z$ của `kiem_dinh.csv`, $p$ lệch dưới $10^{-7}$ vì Java tính $\Phi$ bằng xấp xỉ. Khoảng tin cậy dùng bộ sinh số ngẫu nhiên của Python nên có thể lệch chữ số cuối so với cách Java tính. Không thuộc họ 1, họ 2.

Nguồn: `thuc_nghiem/ket_qua/2026-11-14_bao-cao/per_user.csv`, tính bằng `thuc_nghiem/phan_tich/bo_sung_tuan_6.py` · ngày 2026-11-22

| So sánh | Chỉ số | Số cặp | Hiệu | KTC 95% | p | Hiệu từng seed 3 / 4 / 5 (p) |
| --- | --- | --- | --- | --- | --- | --- |
| `P_sf_tran3` − `P` | NDCG@5 | 1091 | 0.0217 | [0.0179, 0.0256] | < 0.0001 | 0.0247 (< 0.0001) / 0.0193 (< 0.0001) / 0.0212 (< 0.0001) |
| `P_sf_tran3` − `P` | P@5 nghiêm ngặt | 1091 | 0.0273 | [0.0213, 0.0334] | < 0.0001 | 0.0307 (< 0.0001) / 0.0254 (< 0.0001) / 0.0258 (< 0.0001) |
| `P_w_lich` − `P` | NDCG@5 | 1091 | 0.0032 | [-0.0003, 0.0067] | 0.0916 | 0.0058 (0.1128) / 0.0017 (0.6254) / 0.0021 (0.4183) |
| `P_w_lich` − `P` | P@5 nghiêm ngặt | 1091 | 0.0015 | [-0.0037, 0.0073] | 0.6004 | 0.0027 (0.5847) / 0.0022 (0.6056) / -0.0005 (0.9066) |
