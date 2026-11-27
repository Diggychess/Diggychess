package com.example.service

import com.example.BuildConfig
import com.example.data.KnightScopeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class TinyFishSearchResult(
    val title: String,
    val url: String,
    val snippet: String = ""
)

/**
 * TinyFish Web Search integration (`https://api.search.tinyfish.ai`) mirroring the
 * `tinyfish` SDK's `client.search.query(...)` interface using secrets from AI Studio
 * (`TINYFISH_API_KEY`, optional `TINYFISH_RUNTIME_ATTEMPT_ID`) or optional Settings entry.
 */
class TinyFishSearchService(
    private val repo: KnightScopeRepository,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build(),
    private val endpointUrl: String = DEFAULT_SEARCH_ENDPOINT
) {
    companion object {
        const val DEFAULT_SEARCH_ENDPOINT = "https://api.search.tinyfish.ai"
    }

    suspend fun isConfigured(): Boolean = repo.getEffectiveTinyFishKey().isNotBlank()

    suspend fun query(
        query: String,
        location: String = "US",
        language: String = "en",
        purpose: String? = null,
        maxResults: Int = 4
    ): Result<List<TinyFishSearchResult>> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Search query cannot be empty."))
        }
        val apiKey = repo.getEffectiveTinyFishKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("TinyFish API key is not configured. Add TINYFISH_API_KEY in Secrets or Settings.")
            )
        }

        try {
            val base = endpointUrl.toHttpUrlOrNull()
                ?: return@withContext Result.failure(IllegalStateException("Invalid TinyFish endpoint URL."))

            val urlBuilder = base.newBuilder()
                .addQueryParameter("query", cleanQuery)
                .addQueryParameter("location", location.ifBlank { "US" })
                .addQueryParameter("language", language.ifBlank { "en" })
            if (!purpose.isNullOrBlank()) {
                urlBuilder.addQueryParameter("purpose", purpose)
            }

            val reqBuilder = Request.Builder()
                .url(urlBuilder.build())
                .header("X-API-Key", apiKey)
                .header("Accept", "application/json")
                .get()

            val attemptId = BuildConfig.TINYFISH_RUNTIME_ATTEMPT_ID.trim()
            if (attemptId.isNotBlank()) {
                reqBuilder.header("X-Runtime-Attempt-Id", attemptId)
            }

            withTimeout(14_000L) {
                httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                    val rawBody = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        return@withTimeout Result.failure(
                            IllegalStateException("TinyFish Search HTTP ${resp.code}: ${rawBody.take(120)}")
                        )
                    }
                    Result.success(parseResults(rawBody, maxResults))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shouldEnrichQuestion(question: String): Boolean {
        val q = question.lowercase()
        return q.startsWith("search:") ||
            q.startsWith("web:") ||
            listOf(
                "search", "web", "latest", "news", "today", "theory",
                "opening", "gambit", "grandmaster", "gm", "tournament",
                "world cup", "fide", "world chess", "history", "explain"
            ).any { q.contains(it) }
    }

    suspend fun searchChessContext(
        question: String,
        openingName: String?
    ): List<TinyFishSearchResult> {
        if (!isConfigured() || !shouldEnrichQuestion(question)) return emptyList()
        val stripped = question
            .removePrefix("search:")
            .removePrefix("Search:")
            .removePrefix("web:")
            .removePrefix("Web:")
            .trim()
        val finalQuery = if (
            stripped.contains("chess", ignoreCase = true) ||
            stripped.contains("fifa", ignoreCase = true) ||
            stripped.contains("news", ignoreCase = true) ||
            stripped.contains("opening", ignoreCase = true) ||
            !openingName.isNullOrBlank()
        ) {
            if (stripped.contains("news", ignoreCase = true) || stripped.contains("fifa", ignoreCase = true)) {
                stripped
            } else {
                listOfNotNull(stripped, openingName?.takeIf { !stripped.contains(it, ignoreCase = true) }, "chess")
                    .joinToString(" ")
            }
        } else {
            "$stripped chess strategy"
        }
        return query(
            query = finalQuery,
            location = "US",
            language = "en",
            purpose = "Chess coaching and live web reference",
            maxResults = 3
        ).getOrElse { emptyList() }
    }

    internal fun parseResults(rawJson: String, maxResults: Int = 4): List<TinyFishSearchResult> {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) return emptyList()

        val arr: JSONArray = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            else -> {
                val root = JSONObject(trimmed)
                root.optJSONArray("results")
                    ?: root.optJSONArray("data")
                    ?: root.optJSONArray("items")
                    ?: JSONArray()
            }
        }

        val out = mutableListOf<TinyFishSearchResult>()
        for (i in 0 until minOf(arr.length(), maxResults)) {
            val item = arr.optJSONObject(i) ?: continue
            val title = item.optString("title")
                .ifBlank { item.optString("name") }
                .trim()
            val url = item.optString("url")
                .ifBlank { item.optString("link") }
                .trim()
            val snippet = item.optString("snippet")
                .ifBlank { item.optString("description") }
                .ifBlank { item.optString("content") }
                .trim()
            if (title.isNotBlank() && url.isNotBlank()) {
                out.add(TinyFishSearchResult(title = title, url = url, snippet = snippet.take(240)))
            }
        }
        return out
    }
}
