package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ContactDao
import com.example.data.local.dao.RescueDao
import com.example.data.local.dao.VehicleDao
import com.example.data.local.entity.EmergencyContactEntity
import com.example.data.local.entity.RescueRequestEntity
import com.example.data.local.entity.SavedVehicleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [RescueRequestEntity::class, SavedVehicleEntity::class, EmergencyContactEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rescueDao(): RescueDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cuu_ho_xe_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        seedInitialData(database)
                    }
                }
            }
        }

        private suspend fun seedInitialData(database: AppDatabase) {
            val vehicleDao = database.vehicleDao()
            val contactDao = database.contactDao()
            val rescueDao = database.rescueDao()

            // Seed default vehicle
            vehicleDao.insertVehicle(
                SavedVehicleEntity(
                    name = "Mazda CX-5 2.0L",
                    vehicleType = "Xe SUV / Bán tải",
                    licensePlate = "30H - 688.99",
                    brand = "Mazda",
                    color = "Đỏ Pha Lê",
                    isDefault = true
                )
            )
            vehicleDao.insertVehicle(
                SavedVehicleEntity(
                    name = "Honda SH 150i ABS",
                    vehicleType = "Xe máy / Xe điện",
                    licensePlate = "29B1 - 929.38",
                    brand = "Honda",
                    color = "Trắng Đen",
                    isDefault = false
                )
            )

            // Seed emergency contacts
            contactDao.insertContact(
                EmergencyContactEntity(
                    name = "Tổng đài Cứu hộ Quốc gia",
                    phone = "1900545566",
                    relationship = "Đường dây nóng 24/7"
                )
            )
            contactDao.insertContact(
                EmergencyContactEntity(
                    name = "Cứu hộ Cao tốc Việt Nam",
                    phone = "19006489",
                    relationship = "Cứu hộ Cao tốc"
                )
            )
            contactDao.insertContact(
                EmergencyContactEntity(
                    name = "Cảnh sát Giao thông",
                    phone = "113",
                    relationship = "Khẩn cấp Nhà nước"
                )
            )

            // Seed one completed historical request for immediate receipt demonstration
            rescueDao.insertRequest(
                RescueRequestEntity(
                    orderCode = "CH-77291",
                    timestamp = System.currentTimeMillis() - 86400000L * 2, // 2 days ago
                    vehicleType = "Ô tô con (4-7 chỗ)",
                    serviceType = "Kích bình ắc quy",
                    licensePlate = "30H - 688.99",
                    contactName = "Anh Tuấn",
                    contactPhone = "0987654321",
                    locationAddress = "Hầm B2 Tòa Keangnam, Mễ Trì, Nam Từ Liêm, Hà Nội",
                    latitude = 21.0173,
                    longitude = 105.7838,
                    description = "Xe để quên đèn trần qua đêm hết sạch bình không đề được máy.",
                    status = "COMPLETED",
                    technicianName = "Trần Đình Trọng",
                    technicianPhone = "0912345678",
                    technicianVehicle = "Xe cứu hộ cơ động 29C-432.18",
                    estimatedDistanceKm = 0.0,
                    estimatedMinutes = 0,
                    estimatedCost = 250000,
                    actualCost = 250000,
                    rating = 5,
                    ratingComment = "Đến rất nhanh sau 10 phút, kích bình nổ ngay, nhiệt tình hướng dẫn sạc!"
                )
            )
        }
    }
}
