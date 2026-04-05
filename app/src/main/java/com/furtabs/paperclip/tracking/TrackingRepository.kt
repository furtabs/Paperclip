package com.furtabs.paperclip.tracking

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.furtabs.paperclip.BuildConfig
import com.furtabs.paperclip.R
import com.furtabs.paperclip.tracking.utils.TrackingCarrier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.CookieJar
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class TrackingRepository(private val context: Context) {
    private val userPrefs = com.furtabs.paperclip.data.UserPreferences(context)
    private val client = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            private val cookieStore = mutableMapOf<String, List<Cookie>>()
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore[url.host] = cookies
            }
            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host] ?: listOf()
            }
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    data class UspsOAuthTokenResponse(
        val access_token: String?,
        val token_type: String?,
        val expires_in: Long?
    )

    private suspend fun getValidUspsToken(): String? = withContext(Dispatchers.IO) {
        val cached = userPrefs.getUspsAccessToken()
        val expiry = userPrefs.getUspsAccessTokenExpiry()
        if (cached.isNotBlank() && System.currentTimeMillis() + 60_000L < expiry) {
            Log.d("TrackingRepository", "Using cached USPS token until ${expiry}")
            return@withContext cached
        }

        val clientId = BuildConfig.USPS_CLIENT_KEY
        val clientSecret = BuildConfig.USPS_CLIENT_SECRET
        if (clientId.isBlank() || clientSecret.isBlank()) {
            Log.e("TrackingRepository", "Missing USPS_CLIENT_KEY/USPS_CLIENT_SECRET in BuildConfig")
            return@withContext null
        }

        val tokenUrl = BuildConfig.USPS_OAUTH_TOKEN_URL.ifBlank { "https://apis.usps.com/oauth2/v3/token" }

        val jsonPayload = gson.toJson(
            mapOf(
                "grant_type" to "client_credentials",
                "client_id" to clientId,
                "client_secret" to clientSecret
            )
        )

        val requestBody = jsonPayload.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(tokenUrl)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        var response = client.newCall(request).execute()
        var responseBody = response.body?.string() ?: ""
        if (!response.isSuccessful) {
            Log.w("TrackingRepository", "USPS token request (JSON) failed: ${response.code} - ${responseBody}")
            val legacyRequestBody = "grant_type=client_credentials".toRequestBody("application/x-www-form-urlencoded".toMediaType())
            val credentials = Base64.encodeToString("$clientId:$clientSecret".toByteArray(), Base64.NO_WRAP)
            response = client.newCall(
                Request.Builder()
                    .url(tokenUrl)
                    .post(legacyRequestBody)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("Authorization", "Basic $credentials")
                    .build()
            ).execute()
            responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e("TrackingRepository", "Failed USPS token request (legacy): ${response.code} - ${responseBody}")
                return@withContext null
            }
        }

        val tokenResponse = try {
            gson.fromJson(responseBody, UspsOAuthTokenResponse::class.java)
        } catch (t: Throwable) {
            Log.e("TrackingRepository", "USPS token JSON parse failure", t)
            return@withContext null
        }

        val token = tokenResponse.access_token
        if (token.isNullOrBlank()) {
            Log.e("TrackingRepository", "USPS token response missing access_token: $responseBody")
            return@withContext null
        }

        val expiresIn = tokenResponse.expires_in ?: 3600L
        val expiryMillis = System.currentTimeMillis() + (expiresIn * 1000L)
        userPrefs.saveUspsAccessToken(token, expiryMillis)

        Log.d("TrackingRepository", "Got USPS token (expires in ${expiresIn}s)")
        return@withContext token
    }

    fun isCarrierSupported(code: String): Boolean {
        return TrackingCarrier.fromCode(code) != TrackingCarrier.UNKNOWN
    }

    suspend fun trackPackage(code: String, forceRefresh: Boolean = false, carrier: String = "Auto"): TrackingResponse? {
        val cleanCode = code.replace(" ", "").trim().uppercase()

        if (!forceRefresh) {
            val cached = TrackingCache.get(cleanCode)
            if (cached != null) {
                return cached
            }
        }

        val result = when (carrier) {
            context.getString(R.string.carrier_usps) -> trackViaUsps(cleanCode)
            else -> {
                val detectedCarrier = TrackingCarrier.fromCode(cleanCode)
                return when (detectedCarrier) {
                    TrackingCarrier.USPS -> trackViaUsps(cleanCode)
                    TrackingCarrier.UNKNOWN -> null
                }
            }
        }

        if (result != null && !result.events.isNullOrEmpty()) {
            TrackingCache.save(cleanCode, result)
        }

        return result
    }

    private suspend fun trackViaUsps(code: String): TrackingResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://apis.usps.com/tracking/v3r2/tracking"
                val token = getValidUspsToken().takeIf { it?.isNotBlank() == true } ?: BuildConfig.USPS_API_KEY.takeIf { it.isNotBlank() }
                if (token.isNullOrBlank()) {
                    Log.e("TrackingRepository", "USPS lookup failed: no token and no USPS_API_KEY fallback")
                    return@withContext null
                }

                val payloads = listOf(
                    """[{"trackingNumber":"$code"}]""",
                    """{"trackingNumbers":["$code"]}""",
                    """{"trackingNumber":"$code"}""" // keep last as fallback
                )

                var finalData: UspsTrackingResponse? = null

                for (payload in payloads) {
                    Log.d("TrackingRepository", "USPS tracking request code=$code url=$url payload=$payload")
                    val request = Request.Builder()
                        .url(url)
                        .post(payload.toRequestBody("application/json".toMediaType()))
                        .addHeader("User-Agent", "Mozilla/5.0")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Authorization", "Bearer $token")
                        .build()

                    val response = client.newCall(request).execute()
                    val body = response.body?.string() ?: ""
                    Log.d("TrackingRepository", "USPS tracking response status=${response.code} body=$body")

                    if (response.isSuccessful) {
                        finalData = try {
                            gson.fromJson(body, UspsTrackingResponse::class.java)
                        } catch (e: Exception) {
                            gson.fromJson(body, Array<UspsTrackingResponse>::class.java).firstOrNull()
                        }
                        break
                    }

                    if (response.code != 400 || !body.contains("object", true) || !body.contains("array", true)) {
                        // if it's not the specific schema mismatch case, stop retrying
                        return@withContext null
                    }

                    // schema mismatch: try next payload variant
                }

                val rawData = finalData ?: return@withContext null
                val sourceEvents = rawData.trackingEvents ?: rawData.events
                val events = sourceEvents?.mapNotNull { event ->
                    val parsedDate = event.eventDate ?: event.eventTimestamp?.split("T")?.getOrNull(0) ?: ""
                    val parsedTime = event.eventTime ?: event.eventTimestamp?.split("T")?.getOrNull(1)?.substringBefore("Z") ?: ""
                    val statusLabel = event.eventType ?: event.eventDescription ?: event.description ?: rawData.status ?: ""
                    val locationLabel = listOfNotNull(
                        event.eventLocation,
                        event.eventCity,
                        event.eventState,
                        event.eventZIPCode
                    ).filter { it.isNotBlank() }.joinToString(", ").takeIf { it.isNotBlank() } ?: "USPS"

                    if (statusLabel.isBlank() && parsedDate.isBlank()) return@mapNotNull null

                    TrackingEvent(status = statusLabel, date = parsedDate, time = parsedTime, location = locationLabel, subStatus = null)
                } ?: emptyList()

                val finalEvents = if (events.isEmpty() && !rawData.status.isNullOrBlank()) {
                    listOf(TrackingEvent(status = rawData.status, date = "", time = "", location = "USPS", subStatus = null))
                } else events

                if (finalEvents.isEmpty()) return@withContext null
                val deliveryExpectation = rawData.deliveryDateExpectation
                val estimatedDeliveryDate = when {
                    deliveryExpectation?.predictedDeliveryDate.isNullOrBlank() -> null
                    deliveryExpectation.endOfDay.isNullOrBlank() -> deliveryExpectation.predictedDeliveryDate
                    else -> "${deliveryExpectation.predictedDeliveryDate} ${deliveryExpectation.endOfDay}"
                }
                return@withContext TrackingResponse(
                    tracking_code = code,
                    events = finalEvents,
                    estimatedDeliveryDate = estimatedDeliveryDate
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}

fun String.md5(): String {
    val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
    return bytes.joinToString("") { "%02X".format(it) }
}
