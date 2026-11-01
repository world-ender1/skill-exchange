**1. Quy tắc gán nhãn đúng cho thực nghiệm (quan trọng nhất)**

Báo cáo nhắc nhiều lần đến "quy tắc độc lập theo thứ tự thác nước" nhưng chưa trình bày cụ thể quy tắc này. Em bổ sung định nghĩa đầy đủ và trả lời câu hỏi sau: nếu quy tắc gán nhãn dùng chính các thuộc tính mà P dùng để chấm điểm (kỹ năng, trình độ, lịch, hình thức), còn B1 chỉ dùng SkillFit, thì P có bị "thắng sẵn" theo thiết kế không? Em lập luận thế nào để kết quả RQ2 đáng tin cậy?

**2. Công thức ScheduleFit**

Em chia số khung trùng cho min(|A_u|, |A_v|). Với cách tính này, người khai báo rất ít khung rảnh có được lợi không? Vì sao em chọn mẫu số này? Em trình bày lý do, hoặc so sánh với phương án khác trong phần phân tích độ nhạy.

**3. Mã nguồn**

Mục 5.2 chưa có link GitHub. Em bổ sung link repo, và mỗi tuần ghi link commit tương ứng.

**4. Bảo mật**

JWT có hạn 24 giờ và không dùng refresh token. Khi quản trị viên khóa một tài khoản, người đó có tiếp tục dùng được token cũ cho đến khi hết hạn không? Em xác định rõ backend có kiểm tra trạng thái tài khoản ở mỗi request không, và bổ sung vào mục 4.5.

**5. Hoàn thiện báo cáo**

* Mục 2.1 còn trống. Em trình bày ngắn về gợi ý Top-k, hệ gợi ý tương hỗ, Precision@k và NDCG@k, đây là các khái niệm thuật toán và thực nghiệm dựa vào.
* Em xóa phần chữ hướng dẫn màu xám ở những mục đã viết xong.