ỨNG DỤNG AI – MỞ GEMINI LIVE – NHẬN LỆNH TIẾNG VIỆT (MÃ NGUỒN)

Mục tiêu
- Tên hiển thị ứng dụng: AI
- Khi Xiaomi AI mở ứng dụng AI, ứng dụng sẽ gọi thao tác mở Gemini Live. Dịch vụ nghe nền nhận lệnh “Ây Ai” hoặc “Mở AI”.
- Dịch vụ Trợ năng tìm nút “Open Gemini Live” theo resource-id hoặc mô tả trợ năng rồi bấm nút đó.

QUAN TRỌNG
- Đây là mã nguồn dự án Android, chưa phải tệp APK đã biên dịch.
- Môi trường tạo tệp hiện tại không có Android SDK/Gradle để biên dịch và kiểm thử APK thực tế.
- Cần bật Trợ năng thủ công trên điện thoại; Android không cho ứng dụng tự cấp quyền này.
- Xiaomi AI phải có khả năng mở ứng dụng theo tên “AI”. Tính năng này phụ thuộc bản ROM.
- Google có thể thay đổi giao diện hoặc resource-id; nếu vậy ứng dụng có thể cần cập nhật.
- Dịch vụ nghe nền dùng foreground service. Vuốt màn hình Cài đặt khỏi đa nhiệm không phải là lệnh tắt nghe; tuy vậy một số ROM Xiaomi có thể tự giới hạn dịch vụ và cần bật Tự khởi động/Không hạn chế pin.
- Câu đánh thức hiện hỗ trợ “Ây Ai”, “Mở AI”. Mô hình Vosk tiếng Việt cần được tải lần đầu.

CÁCH BIÊN DỊCH
1. Cài Android Studio trên máy tính.
2. Giải nén thư mục dự án.
3. Android Studio > Open > chọn thư mục AI_GeminiLive_Project.
4. Chờ Gradle Sync hoàn tất; cài Android SDK Platform 35 nếu được yêu cầu.
5. Chọn Build > Build APK(s).
6. APK thường được tạo tại app/build/outputs/apk/debug/app-debug.apk.

CÁCH DÙNG TRÊN ĐIỆN THOẠI
1. Cài APK AI.
2. Mở AI một lần.
3. Bấm “Mở cài đặt Trợ năng”.
4. Tìm “AI – mở Gemini Live” và bật quyền.
5. Thử mở ứng dụng AI bằng Xiaomi AI.
6. Nếu Xiaomi AI chỉ mở được Gemini trực tiếp chứ không mở ứng dụng AI, cách này sẽ không tự chạy được.

Quyền Trợ năng có khả năng đọc nội dung giao diện để tìm nút Gemini Live. Chỉ bật nếu bạn hiểu và chấp nhận quyền này.

CÁCH BUILD APK KHÔNG CẦN CÀI ANDROID STUDIO (GitHub Actions)
1. Tạo repo GitHub riêng tư, tải toàn bộ thư mục dự án này lên (nhớ cả thư mục .github).
2. Vào tab Actions > "Build APK" > Run workflow.
3. Chờ xong, tải artifact "AI-debug-apk" > giải nén được app-debug.apk, cài lên điện thoại.
