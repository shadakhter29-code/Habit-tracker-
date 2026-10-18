package com.example.data.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateContent(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateFallbackResponse(prompt)
        }

        try {
            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                if (!systemInstruction.isNullOrBlank()) {
                    val sysObj = JSONObject().apply {
                        val sysParts = JSONArray().apply {
                            put(JSONObject().apply { put("text", systemInstruction) })
                        }
                        put("parts", sysParts)
                    }
                    put("systemInstruction", sysObj)
                }
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext generateFallbackResponse(prompt)
                }
                val responseString = response.body?.string() ?: return@withContext generateFallbackResponse(prompt)
                val jsonResponse = JSONObject(responseString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        if (text.isNotBlank()) return@withContext text
                    }
                }
                return@withContext generateFallbackResponse(prompt)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext generateFallbackResponse(prompt)
        }
    }

    private fun generateFallbackResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("analyze") || lower.contains("progress") -> {
                "📊 **Coach Analysis**: You're doing great keeping your momentum! Your consistency in Health & Mind habits shows strong commitment. Consider focusing on completing 1 primary habit before noon each day to build an impenetrable morning routine."
            }
            lower.contains("suggest") || lower.contains("easier") || lower.contains("missed") -> {
                "💡 **Smart Habit Adjustment**: When missing habits repeatedly, lower the entry barrier! Instead of 'Read 30 mins', try 'Read 5 mins' or 'Read 2 pages'. The key is protecting the daily streak rather than perfect volume."
            }
            lower.contains("motivation") || lower.contains("streak") -> {
                "🔥 **Streak Boost**: 'Success isn't always about greatness. It's about consistency. Consistent hard work leads to success.' Keep your streak alive today, Hero!"
            }
            lower.contains("recommend") || lower.contains("new habit") || lower.contains("goals") -> {
                "🌟 **Recommended Habits**:\n1. 🧘 **5-Minute Deep Breathing** (Mindfulness)\n2. 📝 **Evening Gratitude Journal** (Positivity)\n3. 🚶 **Post-Meal 10-Min Walk** (Energy Boost)\n4. 📱 **Screen-Free Hour Before Bed** (Better Sleep)"
            }
            else -> {
                "⚡ **Hero Coach**: Small, deliberate daily actions compound into extraordinary transformation. Keep tracking your habits daily to level up!"
            }
        }
    }
}
