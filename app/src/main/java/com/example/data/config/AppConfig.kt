package com.example.data.config

/**
 * Cấu hình tập trung cho ứng dụng Cứu Hộ Xe 24/7.
 * Bạn có thể dễ dàng thay đổi số điện thoại hotline cứu hộ, URL Firebase, và các danh mục tại đây.
 */
object AppConfig {
    // Firebase Project & Database Configuration
    const val FIREBASE_PROJECT_ID = "gen-lang-client-0618945200"
    const val FIREBASE_DATABASE_URL = "https://gen-lang-client-0618945200-default-rtdb.firebaseio.com/"

    // Số điện thoại cứu hộ khẩn cấp mặc định (Đường dây nóng cứu hộ khẩn cấp 24/7)
    // Hotline cài đặt trước: 0898212031
    const val DEFAULT_RESCUE_HOTLINE = "0898212031"
    const val RESCUE_HOTLINE_DISPLAY = "0898 212 031"

    // Các loại sự cố cứu hộ theo yêu cầu
    val ISSUE_TYPES = listOf(
        "Xe hết xăng",
        "Hỏng xe",
        "Thủng lốp",
        "Hết bình",
        "Tai nạn",
        "Khóa xe",
        "Khác"
    )

    // Các trạng thái của đơn cứu hộ
    object RequestStatus {
        const val PENDING = "Chờ tiếp nhận"
        const val ACCEPTED = "Đã tiếp nhận"
        const val EN_ROUTE = "Đang đến"
        const val ARRIVED = "Đã đến nơi"
        const val IN_PROGRESS = "Đang xử lý"
        const val COMPLETED = "Hoàn thành"
        const val CANCELLED = "Đã hủy"

        val ALL_STATUSES = listOf(
            PENDING,
            ACCEPTED,
            EN_ROUTE,
            ARRIVED,
            IN_PROGRESS,
            COMPLETED,
            CANCELLED
        )
    }

    // Vai trò tài khoản người dùng
    object UserRole {
        const val USER = "User"
        const val STAFF = "Rescue Staff"
        const val ADMIN = "Admin"
    }

    // Trạng thái hồ sơ đối tác / thợ cứu hộ
    object PartnerApplicationStatus {
        const val NONE = "NONE"
        const val PENDING = "PENDING"
        const val APPROVED = "APPROVED"
        const val REJECTED = "REJECTED"
    }

    // Các loại phương tiện
    val VEHICLE_TYPES = listOf(
        "Ô tô 4-7 chỗ",
        "Xe SUV / Bán tải",
        "Xe máy / Xe điện",
        "Xe tải / Chuyên dụng"
    )

    // Các loại báo cáo phản hồi
    val REPORT_TYPES = listOf(
        "Phản ánh thái độ nhân viên",
        "Sai lệch chi phí cứu hộ",
        "Đến muộn quá thời gian hẹn",
        "Sự cố kỹ thuật ứng dụng",
        "Góp ý cải tiến dịch vụ",
        "Khác"
    )
}
