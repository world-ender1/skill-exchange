# Lượt chạy thực nghiệm chon-cau-hinh

| Mục | Giá trị |
| --- | --- |
| Trạng thái | Xong |
| Lệnh | `java -cp target/classes com.skillexchange.evaluation.EvaluationMain --giai-doan chon --ten chon-cau-hinh` |
| Giai đoạn | chon |
| Seed | 1, 2 |
| Commit | 4d4bc65 |
| Thay đổi chưa commit trong `backend/` | không |
| CPU | Intel64 Family 6 Model 154 Stepping 3, GenuineIntel |
| RAM | 15.7 GB |
| Java | 21.0.12.1+1-LTS |
| Bắt đầu | 2026-11-14T04:20:38+07:00 |
| Thời gian chạy | 3.9 giây |

## File

- `metrics.csv`: chỉ số gộp của từng (seed, N, mức nhiễu, phương án), dạng dài (D72).
- `per_user.csv`: chỉ số của từng người dùng, dùng cho kiểm định theo cặp.
- `kiem_dinh.csv`: bootstrap và Wilcoxon theo cặp (D62); cột ở kế hoạch tuần 5, bước 7.
- `log.txt`: log lượt chạy, có giờ.
