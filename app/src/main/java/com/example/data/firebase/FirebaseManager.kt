package com.example.data.firebase

import android.util.Log
import com.example.data.config.AppConfig
import com.example.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseManager private constructor() {

    companion object {
        private const val TAG = "FirebaseManager"

        @Volatile
        private var INSTANCE: FirebaseManager? = null

        fun getInstance(): FirebaseManager {
            return INSTANCE ?: synchronized(this) {
                val instance = FirebaseManager()
                INSTANCE = instance
                instance
            }
        }
    }

    val auth: FirebaseAuth = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get FirebaseAuth instance: ${e.message}", e)
        throw e
    }

    // Explicitly connect to the project's Realtime Database URL
    val database: FirebaseDatabase = try {
        FirebaseDatabase.getInstance(AppConfig.FIREBASE_DATABASE_URL).apply {
            Log.i(TAG, "FirebaseDatabase instance connected to ${AppConfig.FIREBASE_DATABASE_URL}")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get FirebaseDatabase instance: ${e.message}", e)
        FirebaseDatabase.getInstance()
    }

    val requestsRef: DatabaseReference = database.getReference("requests")
    val chatsRef: DatabaseReference = database.getReference("chats")
    val ratingsRef: DatabaseReference = database.getReference("ratings")
    val reportsRef: DatabaseReference = database.getReference("reports")
    val usersRef: DatabaseReference = database.getReference("users")

    // -------------------------------------------------------------
    // AUTHENTICATION
    // -------------------------------------------------------------

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun getAuthStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            Log.d(TAG, "AuthState changed: user=${auth.currentUser?.uid}")
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, pass: String): Result<FirebaseUser> {
        return try {
            Log.i(TAG, "Attempting signIn for $email")
            val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user ?: throw Exception("Không thể lấy thông tin người dùng")
            Log.i(TAG, "signIn successful: ${user.uid}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "signIn failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signUp(
        email: String,
        pass: String,
        displayName: String,
        phone: String,
        vehicleType: String,
        vehicleName: String,
        licensePlate: String
    ): Result<FirebaseUser> {
        return try {
            Log.i(TAG, "Attempting signUp for $email")
            val result = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user ?: throw Exception("Không thể tạo tài khoản")

            // Save user profile to Realtime Database "users" node
            val profile = UserProfile(
                uid = user.uid,
                email = email.trim(),
                displayName = displayName.trim(),
                phone = phone.trim(),
                role = AppConfig.UserRole.USER,
                vehicleType = vehicleType,
                vehicleName = vehicleName.trim(),
                licensePlate = licensePlate.trim(),
                createdAt = System.currentTimeMillis()
            )
            try {
                usersRef.child(user.uid).setValue(profile.toMap()).await()
                Log.i(TAG, "Profile saved to database for ${user.uid}")
            } catch (dbErr: Exception) {
                Log.e(TAG, "Failed to save profile to Realtime Database: ${dbErr.message}", dbErr)
            }

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "signUp failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "sendPasswordReset failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        Log.i(TAG, "Signing out user ${auth.currentUser?.uid}")
        auth.signOut()
    }

    // -------------------------------------------------------------
    // USER PROFILE
    // -------------------------------------------------------------

    fun getUserProfileFlow(uid: String): Flow<UserProfile?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val uidVal = snapshot.child("uid").getValue(String::class.java) ?: uid
                    val emailVal = snapshot.child("email").getValue(String::class.java) ?: ""
                    val nameVal = snapshot.child("displayName").getValue(String::class.java)
                        ?: snapshot.child("name").getValue(String::class.java) ?: ""
                    val phoneVal = snapshot.child("phone").getValue(String::class.java) ?: ""
                    val roleVal = snapshot.child("role").getValue(String::class.java) ?: AppConfig.UserRole.USER
                    val vType = snapshot.child("vehicleType").getValue(String::class.java) ?: "Ô tô 4-7 chỗ"
                    val vName = snapshot.child("vehicleName").getValue(String::class.java) ?: ""
                    val plate = snapshot.child("licensePlate").getValue(String::class.java) ?: ""
                    val created = snapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                    val profile = UserProfile(
                        uid = uidVal,
                        email = emailVal,
                        displayName = nameVal,
                        phone = phoneVal,
                        role = roleVal,
                        vehicleType = vType,
                        vehicleName = vName,
                        licensePlate = plate,
                        createdAt = created
                    )
                    trySend(profile)
                } else {
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "getUserProfileFlow onCancelled [code=${error.code}]: ${error.message} - ${error.details}")
                trySend(null)
                close()
            }
        }

        val ref = usersRef.child(uid)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun updateUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            usersRef.child(profile.uid).updateChildren(profile.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateUserProfile failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // RESCUE REQUESTS (Node: "requests")
    // -------------------------------------------------------------

    fun getAllRequestsFlow(): Flow<List<RescueRequest>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<RescueRequest>()
                for (child in snapshot.children) {
                    val req = parseRescueRequest(child)
                    if (req != null) {
                        list.add(req)
                    }
                }
                // Sort by newest timestamp first
                list.sortByDescending { it.timestamp }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "getAllRequestsFlow onCancelled [code=${error.code}]: ${error.message} - ${error.details}")
                trySend(emptyList())
                close()
            }
        }

        requestsRef.addValueEventListener(listener)
        awaitClose { requestsRef.removeEventListener(listener) }
    }

    fun getUserRequestsFlow(userId: String): Flow<List<RescueRequest>> = callbackFlow {
        val query = requestsRef.orderByChild("userId").equalTo(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<RescueRequest>()
                for (child in snapshot.children) {
                    val req = parseRescueRequest(child)
                    if (req != null) {
                        list.add(req)
                    }
                }
                list.sortByDescending { it.timestamp }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "getUserRequestsFlow onCancelled [code=${error.code}]: ${error.message} - ${error.details}")
                trySend(emptyList())
                close()
            }
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    fun getRequestByIdFlow(requestId: String): Flow<RescueRequest?> = callbackFlow {
        val ref = requestsRef.child(requestId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val req = parseRescueRequest(snapshot)
                trySend(req)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "getRequestByIdFlow onCancelled [code=${error.code}]: ${error.message} - ${error.details}")
                trySend(null)
                close()
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun createRescueRequest(request: RescueRequest): Result<String> {
        return try {
            val key = requestsRef.push().key ?: throw Exception("Không thể tạo ID cho yêu cầu")
            val fullRequest = request.copy(id = key, timestamp = System.currentTimeMillis())
            requestsRef.child(key).setValue(fullRequest.toMap()).await()
            Log.i(TAG, "createRescueRequest successful: $key")
            Result.success(key)
        } catch (e: Exception) {
            Log.e(TAG, "createRescueRequest failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateRequestStatus(
        requestId: String,
        status: String,
        staffId: String? = null,
        staffName: String? = null,
        staffPhone: String? = null,
        cost: Long? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any?>("status" to status)
            if (staffId != null) updates["staffId"] = staffId
            if (staffName != null) updates["staffName"] = staffName
            if (staffPhone != null) updates["staffPhone"] = staffPhone
            if (cost != null) updates["cost"] = cost

            requestsRef.child(requestId).updateChildren(updates).await()
            Log.i(TAG, "updateRequestStatus successful for $requestId to $status")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateRequestStatus failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun cancelRequest(requestId: String): Result<Unit> {
        return try {
            val updates = mapOf<String, Any?>("status" to AppConfig.RequestStatus.CANCELLED)
            requestsRef.child(requestId).updateChildren(updates).await()
            Log.i(TAG, "cancelRequest successful for $requestId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "cancelRequest failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseRescueRequest(snapshot: DataSnapshot): RescueRequest? {
        if (!snapshot.exists()) return null
        val id = snapshot.key ?: snapshot.child("id").getValue(String::class.java) ?: ""
        val userId = snapshot.child("userId").getValue(String::class.java) ?: ""
        val userName = snapshot.child("userName").getValue(String::class.java) ?: ""
        val userPhone = snapshot.child("userPhone").getValue(String::class.java) ?: ""
        val issueType = snapshot.child("issueType").getValue(String::class.java) ?: "Hỏng xe"
        val desc = snapshot.child("description").getValue(String::class.java) ?: ""
        val addr = snapshot.child("address").getValue(String::class.java) ?: ""

        val lat = when (val latVal = snapshot.child("latitude").value) {
            is Double -> latVal
            is Long -> latVal.toDouble()
            is Number -> latVal.toDouble()
            else -> 0.0
        }
        val lng = when (val lngVal = snapshot.child("longitude").value) {
            is Double -> lngVal
            is Long -> lngVal.toDouble()
            is Number -> lngVal.toDouble()
            else -> 0.0
        }

        val vType = snapshot.child("vehicleType").getValue(String::class.java) ?: ""
        val plate = snapshot.child("licensePlate").getValue(String::class.java) ?: ""
        val status = snapshot.child("status").getValue(String::class.java) ?: AppConfig.RequestStatus.PENDING
        val staffId = snapshot.child("staffId").getValue(String::class.java)
        val staffName = snapshot.child("staffName").getValue(String::class.java)
        val staffPhone = snapshot.child("staffPhone").getValue(String::class.java)
        val cost = when (val costVal = snapshot.child("cost").value) {
            is Long -> costVal
            is Number -> costVal.toLong()
            else -> null
        }
        val time = when (val timeVal = snapshot.child("timestamp").value) {
            is Long -> timeVal
            is Number -> timeVal.toLong()
            else -> System.currentTimeMillis()
        }
        val rating = snapshot.child("rating").getValue(Int::class.java)
        val ratingComment = snapshot.child("ratingComment").getValue(String::class.java)

        return RescueRequest(
            id = id,
            userId = userId,
            userName = userName,
            userPhone = userPhone,
            issueType = issueType,
            description = desc,
            address = addr,
            latitude = lat,
            longitude = lng,
            vehicleType = vType,
            licensePlate = plate,
            status = status,
            staffId = staffId,
            staffName = staffName,
            staffPhone = staffPhone,
            cost = cost,
            timestamp = time,
            rating = rating,
            ratingComment = ratingComment
        )
    }

    // -------------------------------------------------------------
    // CHATS (Node: "chats/{requestId}/{messageId}")
    // -------------------------------------------------------------

    fun getChatMessagesFlow(requestId: String): Flow<List<ChatMessage>> = callbackFlow {
        val ref = chatsRef.child(requestId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    val msgId = child.key ?: child.child("id").getValue(String::class.java) ?: ""
                    val sId = child.child("senderId").getValue(String::class.java) ?: ""
                    val sName = child.child("senderName").getValue(String::class.java) ?: ""
                    val sRole = child.child("senderRole").getValue(String::class.java) ?: "User"
                    val rId = child.child("receiverId").getValue(String::class.java) ?: ""
                    val text = child.child("message").getValue(String::class.java) ?: ""
                    val t = when (val tVal = child.child("timestamp").value) {
                        is Long -> tVal
                        is Number -> tVal.toLong()
                        else -> System.currentTimeMillis()
                    }
                    val isRead = child.child("read").getValue(Boolean::class.java) ?: false

                    list.add(
                        ChatMessage(
                            id = msgId,
                            requestId = requestId,
                            senderId = sId,
                            senderName = sName,
                            senderRole = sRole,
                            receiverId = rId,
                            message = text,
                            timestamp = t,
                            read = isRead
                        )
                    )
                }
                list.sortBy { it.timestamp }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "getChatMessagesFlow onCancelled [code=${error.code}]: ${error.message} - ${error.details}")
                trySend(emptyList())
                close()
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun sendChatMessage(message: ChatMessage): Result<String> {
        return try {
            val ref = chatsRef.child(message.requestId)
            val key = ref.push().key ?: throw Exception("Không thể tạo ID tin nhắn")
            val fullMsg = message.copy(id = key, timestamp = System.currentTimeMillis())
            ref.child(key).setValue(fullMsg.toMap()).await()
            Log.i(TAG, "sendChatMessage successful: $key")
            Result.success(key)
        } catch (e: Exception) {
            Log.e(TAG, "sendChatMessage failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // RATINGS (Node: "ratings")
    // -------------------------------------------------------------

    fun getUserRatingsFlow(userId: String): Flow<List<RatingItem>> = callbackFlow {
        val query = ratingsRef.orderByChild("userId").equalTo(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<RatingItem>()
                for (child in snapshot.children) {
                    val rId = child.key ?: child.child("id").getValue(String::class.java) ?: ""
                    val reqId = child.child("requestId").getValue(String::class.java) ?: ""
                    val uId = child.child("userId").getValue(String::class.java) ?: ""
                    val uName = child.child("userName").getValue(String::class.java) ?: ""
                    val sId = child.child("staffId").getValue(String::class.java) ?: ""
                    val sName = child.child("staffName").getValue(String::class.java) ?: ""
                    val star = child.child("rating").getValue(Int::class.java) ?: 5
                    val comment = child.child("comment").getValue(String::class.java) ?: ""
                    val t = when (val tVal = child.child("timestamp").value) {
                        is Long -> tVal
                        is Number -> tVal.toLong()
                        else -> System.currentTimeMillis()
                    }
                    list.add(RatingItem(rId, reqId, uId, uName, sId, sName, star, comment, t))
                }
                list.sortByDescending { it.timestamp }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "getUserRatingsFlow onCancelled [code=${error.code}]: ${error.message} - ${error.details}")
                trySend(emptyList())
                close()
            }
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    suspend fun createRating(rating: RatingItem): Result<String> {
        return try {
            val key = ratingsRef.push().key ?: throw Exception("Không thể tạo ID đánh giá")
            val fullRating = rating.copy(id = key, timestamp = System.currentTimeMillis())
            ratingsRef.child(key).setValue(fullRating.toMap()).await()

            // Also update the request node with rating info
            if (rating.requestId.isNotBlank()) {
                requestsRef.child(rating.requestId).updateChildren(
                    mapOf(
                        "rating" to rating.rating,
                        "ratingComment" to rating.comment
                    )
                ).await()
            }

            Log.i(TAG, "createRating successful: $key")
            Result.success(key)
        } catch (e: Exception) {
            Log.e(TAG, "createRating failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // REPORTS (Node: "reports")
    // -------------------------------------------------------------

    fun getUserReportsFlow(userId: String): Flow<List<ReportItem>> = callbackFlow {
        val query = reportsRef.orderByChild("userId").equalTo(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ReportItem>()
                for (child in snapshot.children) {
                    val rId = child.key ?: child.child("id").getValue(String::class.java) ?: ""
                    val uId = child.child("userId").getValue(String::class.java) ?: ""
                    val uName = child.child("userName").getValue(String::class.java) ?: ""
                    val uPhone = child.child("userPhone").getValue(String::class.java) ?: ""
                    val reqId = child.child("requestId").getValue(String::class.java)
                    val rType = child.child("reportType").getValue(String::class.java) ?: "Góp ý"
                    val content = child.child("content").getValue(String::class.java) ?: ""
                    val status = child.child("status").getValue(String::class.java) ?: "Chờ xử lý"
                    val t = when (val tVal = child.child("timestamp").value) {
                        is Long -> tVal
                        is Number -> tVal.toLong()
                        else -> System.currentTimeMillis()
                    }
                    list.add(ReportItem(rId, uId, uName, uPhone, reqId, rType, content, status, t))
                }
                list.sortByDescending { it.timestamp }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "getUserReportsFlow onCancelled [code=${error.code}]: ${error.message} - ${error.details}")
                trySend(emptyList())
                close()
            }
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    suspend fun createReport(report: ReportItem): Result<String> {
        return try {
            val key = reportsRef.push().key ?: throw Exception("Không thể tạo ID báo cáo")
            val fullReport = report.copy(id = key, timestamp = System.currentTimeMillis())
            reportsRef.child(key).setValue(fullReport.toMap()).await()
            Log.i(TAG, "createReport successful: $key")
            Result.success(key)
        } catch (e: Exception) {
            Log.e(TAG, "createReport failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
