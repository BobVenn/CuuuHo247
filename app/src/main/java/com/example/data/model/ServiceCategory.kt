package com.example.data.model

data class RescueServiceItem(
    val id: String,
    val title: String,
    val category: String, // "CAR", "MOTORBIKE", "ALL"
    val basePrice: Long,
    val priceDisplay: String,
    val description: String,
    val iconName: String,
    val badge: String = "24/7"
)

data class HotlineContact(
    val title: String,
    val number: String,
    val desc: String,
    val isNational: Boolean = false
)

data class HandbookGuide(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val dangerLevel: String, // "CAO", "TRUNG BÌNH", "LƯU Ý"
    val steps: List<String>,
    val tips: String
)

data class DashboardAlertLight(
    val name: String,
    val meaning: String,
    val action: String,
    val isRedCritical: Boolean
)

object RescueDataCatalog {
    val services = listOf(
        RescueServiceItem(
            id = "BATTERY_JUMP",
            title = "Kích bình Ắc quy khẩn cấp",
            category = "CAR",
            basePrice = 250000,
            priceDisplay = "200.000đ - 300.000đ",
            description = "Kích bình kích nổ tận nơi cho xe ô tô con, SUV, bán tải. Đo kiểm điện áp và tình trạng sạc ắc quy miễn phí.",
            iconName = "BatteryChargingFull",
            badge = "Phổ biến"
        ),
        RescueServiceItem(
            id = "TIRE_PUNCTURE",
            title = "Vá lốp & Thay bánh sơ cua",
            category = "CAR",
            basePrice = 250000,
            priceDisplay = "200.000đ - 350.000đ",
            description = "Vá dùi, vá nấm hoặc thay bánh phụ sơ cua ô tô lưu động trên đường phố hoặc cao tốc. Kê kích thủy lực an toàn.",
            iconName = "TireRepair",
            badge = "Nhanh chóng"
        ),
        RescueServiceItem(
            id = "TOW_TRUCK",
            title = "Cẩu kéo xe sàn trượt về Gara",
            category = "CAR",
            basePrice = 600000,
            priceDisplay = "600.000đ (mở cửa) + 20k/km",
            description = "Xe sàn trượt thủy lực hiện đại, kéo xe số tự động, xe 2 cầu AWD, xe gầm thấp không lo trầy cản.",
            iconName = "LocalShipping",
            badge = "Chuyên dụng"
        ),
        RescueServiceItem(
            id = "FUEL_DELIVERY",
            title = "Tiếp xăng dầu khẩn cấp",
            category = "ALL",
            basePrice = 150000,
            priceDisplay = "150.000đ + Giá nhiên liệu",
            description = "Mang can xăng RON 95 hoặc dầu Diesel đạt chuẩn tận nơi khi xe hết nhiên liệu giữa đường.",
            iconName = "LocalGasStation",
            badge = "Khẩn cấp"
        ),
        RescueServiceItem(
            id = "UNLOCK_DOOR",
            title = "Mở khóa xe quên chìa",
            category = "CAR",
            basePrice = 300000,
            priceDisplay = "300.000đ - 450.000đ",
            description = "Sử dụng đồ nghề chuyên dụng bóng hơi và que mở chốt, cam kết không trầy xước sơn và gioăng cửa.",
            iconName = "Key",
            badge = "An toàn"
        ),
        RescueServiceItem(
            id = "FLOOD_RESCUE",
            title = "Cứu hộ ngập nước thủy kích",
            category = "ALL",
            basePrice = 700000,
            priceDisplay = "700.000đ - 1.200.000đ",
            description = "Cứu hộ kéo xe ra khỏi vùng nước ngập, kiểm tra lọc gió buồng đốt, chống gãy tay biên động cơ.",
            iconName = "WaterDamage",
            badge = "Quan trọng"
        ),
        RescueServiceItem(
            id = "MOTO_PUNCTURE",
            title = "Vá săm lốp xe máy đêm",
            category = "MOTORBIKE",
            basePrice = 70000,
            priceDisplay = "50.000đ - 90.000đ",
            description = "Vá vỏ lốp không săm (tubeless), vá ruột săm xe máy, xe ga lưu động ban đêm 24/7.",
            iconName = "TwoWheeler",
            badge = "Đêm khuya"
        ),
        RescueServiceItem(
            id = "MOTO_TOW",
            title = "Chở xe máy về nhà / trạm",
            category = "MOTORBIKE",
            basePrice = 180000,
            priceDisplay = "150.000đ + 15k/km",
            description = "Xe bán tải hoặc xe cứu hộ chuyên chở xe máy phân khối lớn PKL, xe tay ga đứt dây curoa.",
            iconName = "ElectricMoped",
            badge = "Tiện lợi"
        )
    )

    val hotlines = listOf(
        HotlineContact("Tổng đài Cứu hộ Giao thông Quốc gia", "1900545566", "Phục vụ 24/7 trên toàn quốc, điều phối xe cứu hộ gần nhất", true),
        HotlineContact("Cứu hộ Cao tốc Bắc - Nam & Nội Bài Lào Cai", "19006489", "Đơn vị quản lý và cứu hộ chuyên trách đường cao tốc VEC", true),
        HotlineContact("Cảnh sát Giao thông Khẩn cấp", "113", "Báo cáo tai nạn, ùn tắc nghiêm trọng và điều phối hiện trường", true),
        HotlineContact("Cứu thương Y tế Khẩn cấp", "115", "Hỗ trợ y tế khi có người bị thương trong sự cố giao thông", true),
        HotlineContact("Cứu nạn PCCC Giao thông", "114", "Xử lý cháy nổ, kẹt cabin xe khi tai nạn nặng", true),
        HotlineContact("Cứu hộ Cao tốc TP.HCM - Dầu Giây", "02862529191", "Tổng đài ứng trực cao tốc phía Nam 24/7", false)
    )

    val handbookGuides = listOf(
        HandbookGuide(
            id = "HIGHWAY_BREAKDOWN",
            title = "Sự cố trên Cao tốc: 3 Quy tắc Vàng",
            category = "An toàn cao tốc",
            dangerLevel = "CAO",
            summary = "Gặp sự cố trên cao tốc tiềm ẩn nguy cơ đâm va từ phía sau ở vận tốc 100-120km/h. Cần xử lý lập tức.",
            steps = listOf(
                "Bước 1: Bật ngay đèn khẩn cấp (Hazard) và cố gắng tấp xe vào làn dừng khẩn cấp bên phải.",
                "Bước 2: Tất cả mọi người trên xe lập tức rời xe và đứng ra ngoài lan can/hộ lan tôn sóng an toàn.",
                "Bước 3: Đặt tam giác cảnh báo hoặc cọc phản quang cách đuôi xe tối thiểu 150m - 200m về phía sau.",
                "Bước 4: Gọi ngay Hotline Cứu hộ Cao tốc hoặc bấm nút SOS trên app. Tuyệt đối không tự ý đứng sau đuôi xe để sửa chữa."
            ),
            tips = "Không bao giờ được ngồi trong xe chờ đợi trên cao tốc vì nguy cơ xe tải đâm từ phía sau rất cao."
        ),
        HandbookGuide(
            id = "BATTERY_JUMP_GUIDE",
            title = "Hướng dẫn tự Câu Bình Ắc quy Chuẩn",
            category = "Ắc quy & Điện",
            dangerLevel = "TRUNG BÌNH",
            summary = "Quy tắc nối dây cáp sạc tránh chập nổ ắc quy hoặc hư hỏng hộp đen ECU ô tô hiện đại.",
            steps = listOf(
                "Bước 1: Tắt máy cả hai xe, kéo phanh tay.",
                "Bước 2: Kẹp kìm ĐỎ (+) vào cọc DƯƠNG (+) của ắc quy xe bị hết bình.",
                "Bước 3: Kẹp đầu còn lại của kìm ĐỎ (+) vào cọc DƯƠNG (+) của ắc quy xe cứu hộ.",
                "Bước 4: Kẹp kìm ĐEN (-) vào cọc ÂM (-) của ắc quy xe cứu hộ.",
                "Bước 5: Kẹp đầu kìm ĐEN (-) còn lại vào một thanh kim loại không sơn trên khung máy của xe hết bình (tiếp mát, tránh kẹp trực tiếp vào cực âm bình chết để tránh phát tia lửa).",
                "Bước 6: Khởi động xe cứu hộ chạy ga nhẹ 3 phút, sau đó đề nổ xe chết máy. Khi nổ thành công, tháo dây theo thứ tự ngược lại."
            ),
            tips = "Khi xe đã nổ được máy, không tắt máy ngay mà hãy giữ nổ tối thiểu 20-30 phút để máy phát sạc lại bình."
        ),
        HandbookGuide(
            id = "FLOOD_WATER_GUIDE",
            title = "Ngập Nước: Tránh Thủy Kích Động Cơ",
            category = "Động cơ & Thời tiết",
            dangerLevel = "CAO",
            summary = "Nước lọt vào buồng đốt khi piston nén sẽ gây gãy tay biên, thủng lốc máy chi phí sửa chữa hàng trăm triệu đồng.",
            steps = listOf(
                "Bước 1: Nếu mức nước ngập quá tâm bánh xe hoặc tràn nắp capo, KHÔNG CỐ VƯỢT QUA.",
                "Bước 2: Nếu xe đang đi qua nước bỗng dưng tắt máy: TUYỆT ĐỐI KHÔNG ĐƯỢC ĐỀ LẠI!",
                "Bước 3: Rút chìa khóa, ngắt cọc âm ắc quy nếu có thể để bảo vệ hệ thống điện tử.",
                "Bước 4: Đẩy xe lên chỗ cao ráo nếu an toàn và gọi ngay xe cứu hộ sàn trượt kéo về gara kiểm tra buồng đốt, lọc gió."
            ),
            tips = "Chỉ 1 lần cố đề nổ xe trong nước có thể làm gãy piston ngay tức khắc. Hãy giữ bình tĩnh và gọi cứu hộ kéo xe."
        ),
        HandbookGuide(
            id = "TIRE_BLOWOUT",
            title = "Nổ Lốp Khi Chạy Tốc Độ Cao",
            category = "Lốp & Vận hành",
            dangerLevel = "CAO",
            summary = "Phản xạ sai lầm nhất khi nổ lốp là đạp cứng phanh khiến xe mất thăng bằng lật nhào.",
            steps = listOf(
                "Bước 1: Hai tay giữ thật chặt vô lăng để kiểm soát hướng đi của xe.",
                "Bước 2: Tuyệt đối KHÔNG đạp phanh gấp! Nhả từ từ chân ga để xe giảm tốc độ theo quán tính.",
                "Bước 3: Bật xi nhan phải hoặc đèn hazard khi xe đã ổn định tốc độ.",
                "Bước 4: Khi tốc độ còn dưới 30km/h, rà phanh nhẹ nhàng đưa xe vào lề đường phẳng."
            ),
            tips = "Kiểm tra áp suất lốp định kỳ mỗi tháng một lần và thay lốp khi đã sử dụng quá 5 năm hoặc mòn đến gờ báo."
        )
    )

    val dashAlertLights = listOf(
        DashboardAlertLight("Đèn Áp suất Dầu động cơ (Bình nhớt)", "Áp suất dầu bôi trơn quá thấp, thiếu dầu hoặc bơm dầu hỏng.", "Dừng xe ngay lập tức, tắt máy. Tuyệt đối không chạy tiếp kẻo bó kẹt máy.", true),
        DashboardAlertLight("Đèn Nhiệt độ Nước làm mát (Nhiệt kế)", "Động cơ bị quá nhiệt (sôi nước), quạt hỏng hoặc rò rỉ két nước.", "Bật sưởi tối đa tản nhiệt, tấp vào lề tắt máy. Chờ máy nguội mới kiểm tra.", true),
        DashboardAlertLight("Đèn Hệ thống Phanh (Dấu chấm than !)", "Mức dầu phanh cạn hoặc phanh tay chưa hạ hết, má phanh mòn nguy hiểm.", "Dừng xe kiểm tra phanh ngay, không tiếp tục lưu thông nếu đạp phanh hẫng.", true),
        DashboardAlertLight("Đèn Báo Sạc Ắc quy (Hình bình ắc quy)", "Máy phát điện (dynamo) không nạp điện cho bình hoặc đứt dây curoa.", "Tắt bớt điều hòa, đèn không cần thiết và lái ngay đến gara hoặc gọi cứu hộ.", true),
        DashboardAlertLight("Đèn Cá vàng Check Engine", "Lỗi động cơ, cảm biến khí thải, bugi hoặc phun xăng.", "Nếu đèn sáng vàng xe vẫn chạy bình thường thì đặt lịch gara; nếu đèn nhấp nháy đỏ cần dừng xe.", false),
        DashboardAlertLight("Đèn Cảnh báo Áp suất Lốp (TPMS)", "Một hoặc nhiều lốp bị non hơi hoặc thủng lốp cán đinh.", "Giảm tốc độ, tấp vào kiểm tra và bơm vá lốp kịp thời.", false)
    )
}
