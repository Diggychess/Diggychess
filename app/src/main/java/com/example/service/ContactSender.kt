package com.example.service

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ContactSubmission(
    val senderName: String,
    val senderEmail: String,
    val subject: String,
    val message: String
)

object ContactSender {

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun isFormspreeConfigured(): Boolean {
        val ep = BuildConfig.FORMSPREE_ENDPOINT.replace('\u00A0', ' ').trim()
        if (ep.isEmpty() || ep.contains("PLACEHOLDER", ignoreCase = true)) return false
        val afterScheme = when {
            ep.startsWith("https://") -> ep.removePrefix("https://")
            ep.startsWith("http://") -> ep.removePrefix("http://")
            else -> return false
        }
        val path = afterScheme.substringAfter('/', missingDelimiterValue = "").trim()
        return path.any { it.isLetterOrDigit() }
    }

    fun isEmailJsConfigured(): Boolean {
        val sid = BuildConfig.EMAILJS_SERVICE_ID.replace('\u00A0', ' ').trim()
        val tid = BuildConfig.EMAILJS_TEMPLATE_ID.replace('\u00A0', ' ').trim()
        val pk = BuildConfig.EMAILJS_PUBLIC_KEY.replace('\u00A0', ' ').trim()
        return sid.isNotEmpty() && tid.isNotEmpty() && pk.isNotEmpty() &&
            !sid.startsWith("http") &&
            !sid.contains("PLACEHOLDER", ignoreCase = true) &&
            !tid.contains("PLACEHOLDER", ignoreCase = true) &&
            !pk.contains("PLACEHOLDER", ignoreCase = true)
    }

    fun hasAnyProvider(): Boolean = isFormspreeConfigured() || isEmailJsConfigured()

    suspend fun send(submission: ContactSubmission): Result<String> = withContext(Dispatchers.IO) {
        if (!hasAnyProvider()) {
            return@withContext Result.failure(
                IllegalStateException("Contact delivery is unavailable because no optional email service is configured.")
            )
        }

        if (isFormspreeConfigured()) {
            val fsResult = sendViaFormspree(submission)
            if (fsResult.isSuccess || !isEmailJsConfigured()) {
                return@withContext fsResult
            }
        }

        if (isEmailJsConfigured()) {
            return@withContext sendViaEmailJs(submission)
        }

        Result.failure(IllegalStateException("No contact delivery service available."))
    }

    private fun sendViaFormspree(sub: ContactSubmission): Result<String> {
        return try {
            val payload = JSONObject().apply {
                put("name", sub.senderName)
                put("email", sub.senderEmail)
                put("subject", sub.subject)
                put("message", sub.message)
            }
            val request = Request.Builder()
                .url(BuildConfig.FORMSPREE_ENDPOINT.trim())
                .post(payload.toString().toRequestBody(JSON_TYPE))
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val errMsg = runCatching {
                        JSONObject(raw).optString("error")
                    }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Formspree error (${resp.code})"
                    return Result.failure(IllegalStateException(errMsg))
                }
                Result.success("Message sent via Formspree.")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sendViaEmailJs(sub: ContactSubmission): Result<String> {
        return try {
            val payload = JSONObject().apply {
                put("service_id", BuildConfig.EMAILJS_SERVICE_ID.trim())
                put("template_id", BuildConfig.EMAILJS_TEMPLATE_ID.trim())
                put("user_id", BuildConfig.EMAILJS_PUBLIC_KEY.trim())
                put("template_params", JSONObject().apply {
                    put("from_name", sub.senderName)
                    put("from_email", sub.senderEmail)
                    put("subject", sub.subject)
                    put("message", sub.message)
                })
            }
            val request = Request.Builder()
                .url("https://api.emailjs.com/api/v1.0/email/send")
                .post(payload.toString().toRequestBody(JSON_TYPE))
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return Result.failure(IllegalStateException("EmailJS error (${resp.code}): $raw"))
                }
                Result.success("Message sent via EmailJS.")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
