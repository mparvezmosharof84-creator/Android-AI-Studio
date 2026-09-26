package com.parvez.aistudio

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Autonomous AI App Builder Engine for Android AI Studio
 * Creator & Master: Parvez Mosharof
 */
object AIStudioEngine {

    // সুরক্ষার জন্য স্প্লিট করা মাস্টার চাবি
    private val masterApiKey: String by lazy {
        val p1 = "AQ.Ab8RN6JlpsQNSkP"
        val p2 = "nhKkW-cFdpjr3dfdf"
        val p3 = "EWHyJLM7R1OX82s2tQ"
        p1 + p2 + p3
    }

    private val masterSystemInstruction = """
        You are the Master Autonomous App & Game Builder inside Android AI Studio, built exclusively for Parvez Mosharof.
        Your goal is to build real, production-ready, fully functional Android applications, 3D games, and utilities.
        
        RULES:
        1. NO DUMMY OR PLACEHOLDER CODE: Do not write '// TODO' or leave empty methods. Implement complete production logic, layouts, and button click handlers.
        2. ASSET GENERATION: If the app needs drawables or icons, create complete Android Vector Drawable XML files inside res/drawable.
        3. RESPECT CONFIGURATION: Always keep the user's project Package Name and Language (Kotlin/Java) exactly as configured.
        4. STRUCTURED FILE OUTPUT: Format every created or modified file strictly as:
           <<<FILE:relative/path/to/filename.ext>>>
           [Full code here without truncation]
           <<<END_FILE>>>
        5. Once all files are written, output:
           <<<BUILD_READY>>>
    """.trimIndent()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(35, TimeUnit.SECONDS)
        .readTimeout(70, TimeUnit.SECONDS)
        .build()

    suspend fun queryGeminiDirectly(prompt: String): String = withContext(Dispatchers.IO) {
        val models = listOf("gemini-2.5-flash", "gemini-3.8-flash", "gemini-3.7-flash", "gemini-2.0-flash")
        var lastErr: Exception? = null

        for (m in models) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$m:generateContent?key=$masterApiKey"
                val jsonPayload = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", "$masterSystemInstruction\n\nUser Request: $prompt")
                                })
                            })
                        })
                    })
                }

                val body = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val root = JSONObject(responseBody)
                    val candidates = root.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "")
                        }
                    }
                } else {
                    lastErr = Exception("Response code: ${response.code}\n$responseBody")
                }
            } catch (e: Exception) {
                lastErr = e
            }
        }
        throw lastErr ?: Exception("গুগল এআই থেকে কোনো রেসপন্স পাওয়া যায়নি।")
    }

    fun parseAndSaveFiles(projectDir: File, aiResponse: String): Int {
        val pattern = Pattern.compile("<<<FILE:(.*?)>>>(.*?)<<<END_FILE>>>", Pattern.DOTALL)
        val matcher = pattern.matcher(aiResponse)
        var count = 0

        while (matcher.find()) {
            val path = matcher.group(1)?.trim() ?: continue
            val code = matcher.group(2)?.trim() ?: continue
            val target = File(projectDir, path)
            target.parentFile?.mkdirs()
            target.writeText(code)
            count++
        }
        return count
    }

    fun installApk(context: Context, apkFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun shareAab(context: Context, aabFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            aabFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share AAB Bundle"))
    }
}
