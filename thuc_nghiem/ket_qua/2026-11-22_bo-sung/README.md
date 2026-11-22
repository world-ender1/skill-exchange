# Lượt chạy thực nghiệm bo-sung

| Mục | Giá trị |
| --- | --- |
| Trạng thái | Xong |
| Lệnh | `java -cp target/classes com.skillexchange.evaluation.EvaluationMain --giai-doan bo-sung --ten bo-sung` |
| Giai đoạn | bo-sung |
| Seed | 3, 4, 5 |
| Commit | 10ecc31 |
| Thay đổi chưa commit trong `backend/` | không |
| CPU | Intel64 Family 6 Model 154 Stepping 3, GenuineIntel |
| RAM | 15.7 GB |
| Java | 21.0.12.1+1-LTS |
| Bắt đầu | 2026-11-22T20:32:54+07:00 |
| Thời gian chạy | 22.0 giây |

## File

- `metrics.csv`: chỉ số gộp của từng (seed, N, mức nhiễu, phương án), dạng dài (D72).
- `per_user.csv`: chỉ số của từng người dùng, dùng cho kiểm định theo cặp.
- `kiem_dinh.csv`: bootstrap và Wilcoxon theo cặp (D62); cột ở kế hoạch tuần 5, bước 7.
- `log.txt`: log lượt chạy, có giờ.

Giai đoạn bổ sung theo nhận xét báo cáo tuần 5 (D97–D100): B1, P và P_mot_ds (P xếp một danh sách) ở mọi quy mô; bỏ từng thành phần chia lại theo tỉ lệ (_tl); chỉ số trong từng nhóm hai chiều / một chiều (_hai_chieu, _mot_chieu). kiem_dinh.csv có cả phép gộp các seed lẫn từng seed riêng.
