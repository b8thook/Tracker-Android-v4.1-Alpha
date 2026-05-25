package co.neatfolk.triptracker.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import co.neatfolk.triptracker.data.Trip
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Central Store sync — multi-user version.
 *
 * Auth API (v2 — Trip Tracker user accounts):
 *   POST /api/users/register  { user_id, pin }  → { token, expires, reset_code }
 *   POST /api/users/auth      { user_id, pin }  → { token, expires }
 *
 * Data API (per-user):
 *   GET  /api/users/{user_id}/data/trips   → { data: [...] }
 *   POST /api/users/{user_id}/data/trips   { data: [...] } → { status: saved }
 *
 * Token stored in SharedPreferences as "tt_user_token" with expiry.
 * user_id stored as "tt_user_id".
 */
class StoreSync(private val context: Context) {

    companion object {
        const val STORE_URL     = "https://triptracker.neatfolk.co/store"
        const val PREFS_NAME    = "trip_tracker_prefs"
        const val TOKEN_KEY     = "tt_user_token"
        const val TOKEN_EXP_KEY = "tt_user_token_exp"
        const val USER_ID_KEY   = "tt_user_id"
        private const val TAG   = "StoreSync"
    }

    private val client = OkHttpClient.Builder().build()
    private val gson: Gson = GsonBuilder().create()
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Stored credentials ────────────────────────────────────────────────────

    fun getToken(): String? {
        val token = prefs.getString(TOKEN_KEY, null) ?: return null
        val exp   = prefs.getLong(TOKEN_EXP_KEY, 0)
        return if (exp > System.currentTimeMillis() / 1000) token else null
    }

    fun getUserId(): String? = prefs.getString(USER_ID_KEY, null)

    fun isAuthenticated(): Boolean = getToken() != null && getUserId() != null

    fun saveCredentials(userId: String, token: String, expiryUnixSeconds: Long) {
        prefs.edit()
            .putString(USER_ID_KEY,   userId)
            .putString(TOKEN_KEY,     token)
            .putLong(TOKEN_EXP_KEY,   expiryUnixSeconds)
            .apply()
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(TOKEN_KEY)
            .remove(TOKEN_EXP_KEY)
            .remove(USER_ID_KEY)
            .apply()
    }

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Register a new driver account.
     * Returns RegisterResult with token on success, or error message.
     */
    fun register(userId: String, pin: String): RegisterResult {
        return try {
            val payload = gson.toJson(mapOf("user_id" to userId, "pin" to pin))
            val body = payload.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$STORE_URL/api/users/register")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            val json = gson.fromJson(responseBody, Map::class.java)

            if (response.isSuccessful) {
                val token     = json["token"] as? String ?: ""
                val expires   = (json["expires"] as? Double)?.toLong() ?: 0L
                val resetCode = json["reset_code"] as? String ?: ""
                RegisterResult(success = true, token = token,
                    expiryUnixSeconds = expires, resetCode = resetCode)
            } else {
                val error = json["error"] as? String ?: "Registration failed"
                RegisterResult(success = false, error = error)
            }
        } catch (e: IOException) {
            Log.e(TAG, "register error: ${e.message}")
            RegisterResult(success = false, error = "Network error — check your connection")
        }
    }

    data class RegisterResult(
        val success: Boolean,
        val token: String = "",
        val expiryUnixSeconds: Long = 0,
        val resetCode: String = "",
        val error: String = ""
    )

    // ── Authentication ────────────────────────────────────────────────────────

    /**
     * Authenticate with user_id + PIN.
     * Returns the token string on success, null on failure.
     * Sets errorMessage on failure.
     */
    fun authenticate(userId: String, pin: String): AuthResult {
        return try {
            val payload = gson.toJson(mapOf("user_id" to userId, "pin" to pin))
            val body = payload.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$STORE_URL/api/users/auth")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            val json = gson.fromJson(responseBody, Map::class.java)

            if (response.isSuccessful) {
                val token   = json["token"]   as? String ?: ""
                val expires = (json["expires"] as? Double)?.toLong() ?: 0L
                AuthResult(success = true, token = token, expiryUnixSeconds = expires)
            } else {
                val error = json["error"] as? String ?: "Authentication failed"
                AuthResult(success = false, error = error)
            }
        } catch (e: IOException) {
            Log.e(TAG, "auth error: ${e.message}")
            AuthResult(success = false, error = "Network error — check your connection")
        }
    }

    data class AuthResult(
        val success: Boolean,
        val token: String = "",
        val expiryUnixSeconds: Long = 0,
        val error: String = ""
    )

    // ── Auto token refresh ────────────────────────────────────────────────────
    // Called before data operations — refreshes if expired using stored PIN.
    // Since we don't store PIN, user must re-enter if token expires.
    // Token is 24h so in practice this is rare during normal daily use.

    // ── Fetch trips from store ────────────────────────────────────────────────

    fun fetchTrips(): List<Trip>? {
        val token  = getToken() ?: return null
        val userId = getUserId() ?: return null
        return try {
            val request = Request.Builder()
                .url("$STORE_URL/api/users/$userId/data/trips")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "fetchTrips: HTTP ${response.code}")
                return null
            }
            val responseBody = response.body?.string() ?: return null
            val wrapper = gson.fromJson(responseBody, StoreResponse::class.java)
            val dataJson = gson.toJson(wrapper.data)
            val type = object : TypeToken<List<Trip>>() {}.type
            gson.fromJson<List<Trip>>(dataJson, type)
        } catch (e: IOException) {
            Log.e(TAG, "fetchTrips error: ${e.message}")
            null
        }
    }

    // ── Push trips to store ───────────────────────────────────────────────────

    fun pushTrips(trips: List<Trip>): Boolean {
        val token  = getToken() ?: return false
        val userId = getUserId() ?: return false
        return try {
            val payload = gson.toJson(StorePayload(data = trips))
            val body = payload.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$STORE_URL/api/users/$userId/data/trips")
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            response.isSuccessful.also { ok ->
                if (!ok) Log.w(TAG, "pushTrips: HTTP ${response.code}")
            }
        } catch (e: IOException) {
            Log.e(TAG, "pushTrips error: ${e.message}")
            false
        }
    }

    // ── Legacy token methods (kept for backward compat) ───────────────────────
    // These are no-ops now — token is managed via saveCredentials/clearCredentials
    @Deprecated("Use saveCredentials instead")
    fun saveToken(token: String, expiryUnixSeconds: Long) {
        // No-op — use saveCredentials with userId
    }

    @Deprecated("Use clearCredentials instead")
    fun clearToken() = clearCredentials()

    // ── Data classes ──────────────────────────────────────────────────────────
    data class StoreResponse(val data: Any?)
    data class StorePayload(val data: Any)
}
