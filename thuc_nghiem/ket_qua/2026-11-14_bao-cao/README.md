# Lượt chạy thực nghiệm bao-cao

| Mục | Giá trị |
| --- | --- |
| Trạng thái | Xong |
| Lệnh | `java -cp target/classes com.skillexchange.evaluation.EvaluationMain --giai-doan bao-cao --ten bao-cao --trong-so lich --schedule tran3` |
| Giai đoạn | bao-cao |
| Seed | 3, 4, 5 |
| Cấu hình đã chọn (D69) | trọng số P_w_lich (0.5, 0.15, 0.25, 0.05, 0.05), ScheduleFit tran3 |
| Commit | 3a01bfc |
| Thay đổi chưa commit trong `backend/` | không |
| CPU | Intel64 Family 6 Model 154 Stepping 3, GenuineIntel |
| RAM | 15.7 GB |
| Java | 21.0.12.1+1-LTS |
| Bắt đầu | 2026-11-14T04:28:29+07:00 |
| Thời gian chạy | 50.2 giây |

## File

- `metrics.csv`: chỉ số gộp của từng (seed, N, mức nhiễu, phương án), dạng dài (D72).
- `per_user.csv`: chỉ số của từng người dùng, dùng cho kiểm định theo cặp.
- `kiem_dinh.csv`: bootstrap và Wilcoxon theo cặp (D62); cột ở kế hoạch tuần 5, bước 7.
- `log.txt`: log lượt chạy, có giờ.

Ghi chú latency (D61): đo trong bộ nhớ, đơn luồng, sau một lượt làm nóng. B0, B1, R chạy qua cùng lõi với P (D65) nên latency của chúng gần P.
