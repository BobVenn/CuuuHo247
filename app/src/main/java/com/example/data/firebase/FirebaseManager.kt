package com.example.data.firebase

import android.util.Log
import com.example.RescueApplication
import com.example.data.config.AppConfig
import com.example.data.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
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
                ensureFirebaseInitialized()
                val instance = FirebaseManager()
                INSTANCE = instance
                instance
            }
        }

        fun ensureFirebaseInitialized() {
            try {
                val app = try { FirebaseApp.getInstance() } catch (_: Exception) { null }
                if (app == null) {
                    val context = try { RescueApplication.instance } catch (_: Exception) { null }
                    if (context != null) {
                        val options = FirebaseOptions.Builder()
                            .setApplicationId("1:342096270055:android:ed752cfcb1e14d3ff44842")
                            .setApiKey("AIzaSyBp0u6_-VVNUghScAyrZO-CoBNXOROys4I")
                            .setDatabaseUrl("https://gen-lang-client-0618945200-default-rtdb.firebaseio.com/")
                            .setProjectId("gen-lang-client-0618945200")
                            .setStorageBucket("gen-lang-client-0618945200.firebasestorage.app")
                            .build()
                        FirebaseApp.initializeApp(context, options)
                        Log.i(TAG, "ensureFirebaseInitialized: initialized default FirebaseApp")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "ensureFirebaseInitialized notice: ${e.message}")
            }
        }
    }

    val auth: FirebaseAuth = try {
        ensureFirebaseInitialized()
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get FirebaseAuth instance: ${e.message}", e)
        ensureFirebaseInitialized()
        FirebaseAuth.getInstance()
    }

    // Connect to FirebaseDatabase instance safely
    val database: FirebaseDatabase = try {
        ensureFirebaseInitialized()
        FirebaseDatabase.getInstance().apply {
            Log.i(TAG, "FirebaseDatabase instance connected to default URL")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get default FirebaseDatabase, trying custom URL: ${e.message}", e)
        ensureFirebaseInitialized()
        try {
            FirebaseDatabase.getInstance(AppConfig.FIREBASE_DATABASE_URL)
        } catch (e2: Exception) {
            Log.e(TAG, "Failed to get custom FirebaseDatabase instance: ${e2.message}", e2)
            FirebaseDatabase.getInstance()
        }
    }

    val requestsRef: DatabaseReference = database.getReference("requests")
    val chatsRef: DatabaseReference = database.getReference("chats")
    val ratingsRef: DatabaseReference = database.getReference("ratings")
    val reportsRef: DatabaseReference = database.getReference("reports")
    val usersRef: DatabaseReference = database.getReference("users")
    val staffApplicationsRef: DatabaseReference = database.getReference("staff_applications")
    val servicesAvailabilityRef: DatabaseReference = database.getReference("services_availability")

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

    suspend fun signInWithGoogleIdToken(idToken: String): Result<FirebaseUser> {
        return try {
            Log.i(TAG, "Attempting signInWithGoogleIdToken")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("Không thể lấy thông tin người dùng Google")
            Log.i(TAG, "signInWithGoogle successful: ${user.uid}")

            // Check if profile exists; if not, create default profile
            try {
                val snapshot = usersRef.child(user.uid).get().await()
                if (!snapshot.exists()) {
                    val profile = UserProfile(
                        uid = user.uid,
                        email = user.email ?: "",
                        displayName = user.displayName ?: "Người dùng Google",
                        phone = user.phoneNumber ?: "",
                        role = AppConfig.UserRole.USER,
                        vehicleType = AppConfig.VEHICLE_TYPES[0],
                        vehicleName = "",
                        licensePlate = "",
                        createdAt = System.currentTimeMillis()
                    )
                    usersRef.child(user.uid).setValue(profile.toMap()).await()
                    Log.i(TAG, "Created default profile for Google user: ${user.uid}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Notice checking/creating Google user profile: ${e.message}")
            }

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "signInWithGoogle failed: ${e.message}", e)
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
        licensePlate: String,
        role: String = AppConfig.UserRole.USER
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
                role = role,
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
                    val pStatus = snapshot.child("partnerStatus").getValue(String::class.java) ?: AppConfig.PartnerApplicationStatus.NONE
                    val pReason = snapshot.child("partnerRejectionReason").getValue(String::class.java)
                    val emerContact = snapshot.child("emergencyContactName").getValue(String::class.java) ?: ""
                    val emerPhone = snapshot.child("emergencyContactPhone").getValue(String::class.java) ?: ""
                    val emerRel = snapshot.child("emergencyRelationship").getValue(String::class.java) ?: "Người thân"
                    val bType = snapshot.child("bloodType").getValue(String::class.java) ?: ""
                    val eNotes = snapshot.child("emergencyNotes").getValue(String::class.java) ?: ""
                    val hAddr = snapshot.child("homeAddress").getValue(String::class.java) ?: ""
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
                        partnerStatus = pStatus,
                        partnerRejectionReason = pReason,
                        emergencyContactName = emerContact,
                        emergencyContactPhone = emerPhone,
                        emergencyRelationship = emerRel,
                        bloodType = bType,
                        emergencyNotes = eNotes,
                        homeAddress = hAddr,
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
            val currentFbUser = auth.currentUser
            if (currentFbUser != null && currentFbUser.uid == profile.uid && profile.displayName.isNotBlank()) {
                try {
                    val updateReq = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(profile.displayName)
                        .build()
                    currentFbUser.updateProfile(updateReq).await()
                } catch (e: Exception) {
                    Log.w(TAG, "FirebaseAuth updateProfile non-fatal: ${e.message}")
                }
            }
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

    suspend fun updatePaymentStatus(requestId: String, paymentStatus: String): Result<Unit> {
        return try {
            val updates = mapOf<String, Any?>("paymentStatus" to paymentStatus)
            requestsRef.child(requestId).updateChildren(updates).await()
            Log.i(TAG, "updatePaymentStatus successful for $requestId to $paymentStatus")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updatePaymentStatus failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun submitRating(requestId: String, rating: Int, comment: String): Result<Unit> {
        return try {
            val updates = mapOf<String, Any?>(
                "rating" to rating,
                "ratingComment" to comment
            )
            requestsRef.child(requestId).updateChildren(updates).await()
            Log.i(TAG, "submitRating successful for $requestId with rating $rating")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "submitRating failed: ${e.message}", e)
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
        val paymentStatus = snapshot.child("paymentStatus").getValue(String::class.java) ?: "UNPAID"
        val imageUrl = snapshot.child("imageUrl").getValue(String::class.java)
        val time = when (val timeVal = snapshot.child("timestamp").value) {
            is Long -> timeVal
            is Number -> timeVal.toLong()
            else -> System.currentTimeMillis()
        }
        val rating = snapshot.child("rating").getValue(Int::class.java)
        val ratingComment = snapshot.child("ratingComment").getValue(String::class.java)
        val emerContact = snapshot.child("emergencyContact").getValue(String::class.java)
        val emerPhone = snapshot.child("emergencyContactPhone").getValue(String::class.java)

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
            paymentStatus = paymentStatus,
            imageUrl = imageUrl,
            emergencyContact = emerContact,
            emergencyContactPhone = emerPhone,
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

    // -------------------------------------------------------------
    // STAFF & PARTNER APPLICATIONS (Node: "staff_applications")
    // -------------------------------------------------------------

    suspend fun submitStaffApplication(application: StaffApplication): Result<Unit> {
        return try {
            val key = application.userId.ifBlank { staffApplicationsRef.push().key ?: System.currentTimeMillis().toString() }
            val appWithId = application.copy(id = key)
            staffApplicationsRef.child(key).setValue(appWithId.toMap()).await()
            // Update user's partnerStatus in users node
            usersRef.child(application.userId).updateChildren(
                mapOf(
                    "partnerStatus" to AppConfig.PartnerApplicationStatus.PENDING,
                    "partnerRejectionReason" to null
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "submitStaffApplication failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getStaffApplicationFlow(userId: String): Flow<StaffApplication?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    trySend(parseStaffApplication(snapshot))
                } else {
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "getStaffApplicationFlow cancelled: ${error.message}")
                trySend(null)
                close()
            }
        }
        val ref = staffApplicationsRef.child(userId)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun getAllStaffApplicationsFlow(): Flow<List<StaffApplication>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<StaffApplication>()
                for (child in snapshot.children) {
                    val app = parseStaffApplication(child)
                    if (app != null) {
                        list.add(app)
                    }
                }
                list.sortByDescending { it.appliedAt }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "getAllStaffApplicationsFlow cancelled: ${error.message}")
                trySend(emptyList())
                close()
            }
        }
        staffApplicationsRef.addValueEventListener(listener)
        awaitClose { staffApplicationsRef.removeEventListener(listener) }
    }

    suspend fun reviewStaffApplication(
        userId: String,
        approve: Boolean,
        reason: String? = null,
        reviewerName: String = "Admin Tổng Đài"
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val newStatus = if (approve) AppConfig.PartnerApplicationStatus.APPROVED else AppConfig.PartnerApplicationStatus.REJECTED
            val updates = mapOf<String, Any?>(
                "status" to newStatus,
                "reviewedAt" to now,
                "reviewedBy" to reviewerName,
                "rejectionReason" to reason
            )
            staffApplicationsRef.child(userId).updateChildren(updates).await()

            // Update user role and partner status
            val userUpdates = mutableMapOf<String, Any?>(
                "partnerStatus" to newStatus,
                "partnerRejectionReason" to reason
            )
            if (approve) {
                userUpdates["role"] = AppConfig.UserRole.STAFF
            }
            usersRef.child(userId).updateChildren(userUpdates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "reviewStaffApplication failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseStaffApplication(snapshot: DataSnapshot): StaffApplication? {
        return try {
            StaffApplication(
                id = snapshot.child("id").getValue(String::class.java) ?: snapshot.key ?: "",
                userId = snapshot.child("userId").getValue(String::class.java) ?: "",
                userName = snapshot.child("userName").getValue(String::class.java) ?: "",
                userEmail = snapshot.child("userEmail").getValue(String::class.java) ?: "",
                phone = snapshot.child("phone").getValue(String::class.java) ?: "",
                idCardNumber = snapshot.child("idCardNumber").getValue(String::class.java) ?: "",
                operatingArea = snapshot.child("operatingArea").getValue(String::class.java) ?: "",
                companyOrGarageName = snapshot.child("companyOrGarageName").getValue(String::class.java) ?: "",
                taxOrBusinessCode = snapshot.child("taxOrBusinessCode").getValue(String::class.java) ?: "",
                garageAddress = snapshot.child("garageAddress").getValue(String::class.java) ?: "",
                operatingRadiusKm = snapshot.child("operatingRadiusKm").getValue(Int::class.java) ?: 20,
                rescueVehicleType = snapshot.child("rescueVehicleType").getValue(String::class.java) ?: "Xe cẩu sàn trượt",
                vehiclePlate = snapshot.child("vehiclePlate").getValue(String::class.java) ?: "",
                servicesOffered = snapshot.child("servicesOffered").getValue(String::class.java) ?: "Cứu hộ xe",
                documentsInfo = snapshot.child("documentsInfo").getValue(String::class.java) ?: "Đã xác thực",
                documentImageUris = snapshot.child("documentImageUris").getValue(String::class.java) ?: "",
                status = snapshot.child("status").getValue(String::class.java) ?: AppConfig.PartnerApplicationStatus.PENDING,
                rejectionReason = snapshot.child("rejectionReason").getValue(String::class.java),
                appliedAt = snapshot.child("appliedAt").getValue(Long::class.java) ?: System.currentTimeMillis(),
                reviewedAt = snapshot.child("reviewedAt").getValue(Long::class.java),
                reviewedBy = snapshot.child("reviewedBy").getValue(String::class.java)
            )
        } catch (e: Exception) {
            Log.e(TAG, "parseStaffApplication error: ${e.message}", e)
            null
        }
    }

    // -------------------------------------------------------------
    // SERVICES AVAILABILITY (TOWING, JUMP-START, TIRE CHANGE, ETC.)
    // -------------------------------------------------------------

    fun getDefaultServiceAvailabilityList(): List<ServiceAvailabilityItem> = listOf(
        ServiceAvailabilityItem(
            id = "towing",
            name = "Cẩu kéo xe cứu hộ sàn trượt",
            issueType = "Tai nạn",
            category = "TOWING",
            isAvailable = true,
            availabilityStatus = "AVAILABLE",
            statusText = "Đang sẵn sàng • 4 xe trực",
            etaMinutes = 20,
            activeTechnicians = 4,
            priceDisplay = "Từ 450.000đ",
            basePrice = 450000L,
            description = "Xe sàn trượt thủy lực hiện đại kéo xe ô tô 4-7 chỗ, gầm thấp, số tự động hoặc tai nạn về Gara theo yêu cầu.",
            iconKey = "towing",
            operatingHours = "24/7 Toàn quốc"
        ),
        ServiceAvailabilityItem(
            id = "jump_start",
            name = "Kích nổ bình ắc quy ô tô & xe máy",
            issueType = "Hết bình",
            category = "BATTERY",
            isAvailable = true,
            availabilityStatus = "AVAILABLE",
            statusText = "Đang sẵn sàng • 6 xe trực",
            etaMinutes = 12,
            activeTechnicians = 6,
            priceDisplay = "Từ 200.000đ",
            basePrice = 200000L,
            description = "Kích nổ bình điện khẩn cấp lưu động bằng máy kích Lithium cao cấp, đo kiểm điện áp và sạc máy phát miễn phí.",
            iconKey = "jump_start",
            operatingHours = "24/7 Toàn quốc"
        ),
        ServiceAvailabilityItem(
            id = "tire_change",
            name = "Vá lốp & Thay bánh sơ cua lưu động",
            issueType = "Thủng lốp",
            category = "TIRE",
            isAvailable = true,
            availabilityStatus = "AVAILABLE",
            statusText = "Đang sẵn sàng • 5 xe trực",
            etaMinutes = 15,
            activeTechnicians = 5,
            priceDisplay = "Từ 120.000đ",
            basePrice = 120000L,
            description = "Vá nấm, vá dùi hoặc thay bánh phụ sơ cua ô tô, xe máy tận nơi. Trang bị kích thủy lực an toàn không móp gầm xe.",
            iconKey = "tire_change",
            operatingHours = "24/7 Toàn quốc"
        ),
        ServiceAvailabilityItem(
            id = "fuel_delivery",
            name = "Tiếp xăng dầu khẩn cấp tận nơi",
            issueType = "Xe hết xăng",
            category = "FUEL",
            isAvailable = true,
            availabilityStatus = "AVAILABLE",
            statusText = "Đang sẵn sàng • Can 5L-10L",
            etaMinutes = 15,
            activeTechnicians = 3,
            priceDisplay = "150.000đ + Nhiên liệu",
            basePrice = 150000L,
            description = "Giao can xăng RON 95 hoặc dầu Diesel đạt chuẩn Petrolimex tận nơi khi xe hết nhiên liệu giữa đường.",
            iconKey = "fuel_delivery",
            operatingHours = "24/7 Toàn quốc"
        ),
        ServiceAvailabilityItem(
            id = "locksmith",
            name = "Mở khóa ô tô quên chìa trong xe",
            issueType = "Khóa xe",
            category = "LOCKSMITH",
            isAvailable = true,
            availabilityStatus = "AVAILABLE",
            statusText = "Đang sẵn sàng • Chuyên dụng",
            etaMinutes = 20,
            activeTechnicians = 2,
            priceDisplay = "Từ 300.000đ",
            basePrice = 300000L,
            description = "Mở khóa cửa ô tô bằng bóng hơi và cây gạt chốt chuyên dụng, cam kết tuyệt đối không trầy sơn hay móp cửa.",
            iconKey = "locksmith",
            operatingHours = "24/7 Toàn quốc"
        ),
        ServiceAvailabilityItem(
            id = "mobile_repair",
            name = "Khám xe & Sửa chữa lưu động",
            issueType = "Hỏng xe",
            category = "MECHANIC",
            isAvailable = true,
            availabilityStatus = "AVAILABLE",
            statusText = "Đang sẵn sàng • Thợ lành nghề",
            etaMinutes = 25,
            activeTechnicians = 3,
            priceDisplay = "Từ 150.000đ",
            basePrice = 150000L,
            description = "Xử lý đứt curoa, sôi nước làm mát, mất phanh, sự cố điện và khởi động xe tại chỗ.",
            iconKey = "mobile_repair",
            operatingHours = "24/7 Toàn quốc"
        )
    )

    fun getServiceAvailabilityFlow(): Flow<List<ServiceAvailabilityItem>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ServiceAvailabilityItem>()
                if (snapshot.exists() && snapshot.childrenCount > 0) {
                    for (child in snapshot.children) {
                        val item = parseServiceAvailability(child)
                        if (item != null) {
                            list.add(item)
                        }
                    }
                }
                if (list.isEmpty()) {
                    // Fallback to default services while seeding
                    trySend(getDefaultServiceAvailabilityList())
                } else {
                    trySend(list)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "getServiceAvailabilityFlow onCancelled: ${error.message}")
                trySend(getDefaultServiceAvailabilityList())
                close()
            }
        }

        servicesAvailabilityRef.addValueEventListener(listener)
        awaitClose { servicesAvailabilityRef.removeEventListener(listener) }
    }

    suspend fun seedDefaultServicesIfEmpty(): Result<Unit> {
        return try {
            val snapshot = servicesAvailabilityRef.get().await()
            if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                Log.i(TAG, "Seeding default service availability data to Firebase Realtime Database...")
                val defaults = getDefaultServiceAvailabilityList()
                for (item in defaults) {
                    servicesAvailabilityRef.child(item.id).setValue(item.toMap()).await()
                }
                Log.i(TAG, "Seeding default services complete: ${defaults.size} services uploaded.")
            } else {
                Log.d(TAG, "Services already present in Firebase (${snapshot.childrenCount} nodes).")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "seedDefaultServicesIfEmpty error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateServiceAvailability(
        serviceId: String,
        isAvailable: Boolean,
        statusText: String? = null,
        etaMinutes: Int? = null,
        activeTechnicians: Int? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "isAvailable" to isAvailable,
                "availabilityStatus" to if (isAvailable) "AVAILABLE" else "BUSY",
                "updatedTimestamp" to System.currentTimeMillis()
            )
            if (statusText != null) {
                updates["statusText"] = statusText
            } else {
                updates["statusText"] = if (isAvailable) "Sẵn sàng phục vụ 24/7" else "Tạm bận • Đang xử lý ca khác"
            }
            if (etaMinutes != null) {
                updates["etaMinutes"] = etaMinutes
            }
            if (activeTechnicians != null) {
                updates["activeTechnicians"] = activeTechnicians
            }

            servicesAvailabilityRef.child(serviceId).updateChildren(updates).await()
            Log.i(TAG, "updateServiceAvailability succeeded for $serviceId (available=$isAvailable)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateServiceAvailability failed for $serviceId: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun parseServiceAvailability(snapshot: DataSnapshot): ServiceAvailabilityItem? {
        return try {
            val id = snapshot.child("id").getValue(String::class.java)
                ?: snapshot.key ?: return null
            val name = snapshot.child("name").getValue(String::class.java) ?: "Dịch vụ cứu hộ"
            val issueType = snapshot.child("issueType").getValue(String::class.java) ?: "Hỏng xe"
            val category = snapshot.child("category").getValue(String::class.java) ?: "EMERGENCY"
            val isAvailable = snapshot.child("isAvailable").getValue(Boolean::class.java) ?: true
            val availabilityStatus = snapshot.child("availabilityStatus").getValue(String::class.java)
                ?: if (isAvailable) "AVAILABLE" else "BUSY"
            val statusText = snapshot.child("statusText").getValue(String::class.java)
                ?: if (isAvailable) "Sẵn sàng phục vụ 24/7" else "Tạm ngưng"
            val etaMinutes = snapshot.child("etaMinutes").getValue(Int::class.java) ?: 15
            val activeTechnicians = snapshot.child("activeTechnicians").getValue(Int::class.java) ?: 3
            val priceDisplay = snapshot.child("priceDisplay").getValue(String::class.java) ?: "Liên hệ"
            val basePrice = snapshot.child("basePrice").getValue(Long::class.java) ?: 0L
            val description = snapshot.child("description").getValue(String::class.java) ?: ""
            val iconKey = snapshot.child("iconKey").getValue(String::class.java) ?: id
            val operatingHours = snapshot.child("operatingHours").getValue(String::class.java) ?: "24/7"
            val updatedTimestamp = snapshot.child("updatedTimestamp").getValue(Long::class.java)
                ?: System.currentTimeMillis()

            ServiceAvailabilityItem(
                id = id,
                name = name,
                issueType = issueType,
                category = category,
                isAvailable = isAvailable,
                availabilityStatus = availabilityStatus,
                statusText = statusText,
                etaMinutes = etaMinutes,
                activeTechnicians = activeTechnicians,
                priceDisplay = priceDisplay,
                basePrice = basePrice,
                description = description,
                iconKey = iconKey,
                operatingHours = operatingHours,
                updatedTimestamp = updatedTimestamp
            )
        } catch (e: Exception) {
            Log.e(TAG, "parseServiceAvailability error: ${e.message}", e)
            null
        }
    }
}
