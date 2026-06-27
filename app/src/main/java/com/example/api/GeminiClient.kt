package com.example.api

import android.util.Log
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

data class BankSmsParseResult(
    val bankName: String,
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String,
    val description: String,
    val referenceNumber: String?
)

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateContent(
        prompt: String,
        systemInstruction: String? = null,
        isJsonMode: Boolean = false,
        jsonSchema: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured!")
            return@withContext "ERROR: API Key belum diatur di panel Secrets AI Studio."
        }

        try {
            val requestJson = JSONObject()
            
            // Contents array
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            // System Instruction
            if (systemInstruction != null) {
                val systemInstructionObj = JSONObject()
                val siPartsArray = JSONArray()
                val siPartObj = JSONObject()
                siPartObj.put("text", systemInstruction)
                siPartsArray.put(siPartObj)
                systemInstructionObj.put("parts", siPartsArray)
                requestJson.put("systemInstruction", systemInstructionObj)
            }

            // Generation Config
            val generationConfig = JSONObject()
            generationConfig.put("temperature", 0.2)
            
            if (isJsonMode) {
                generationConfig.put("responseMimeType", "application/json")
                if (jsonSchema != null) {
                    generationConfig.put("responseSchema", JSONObject(jsonSchema))
                }
            }
            requestJson.put("generationConfig", generationConfig)

            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Gemini API failure: ${response.code} - $errBody")
                    return@withContext "ERROR: Gagal menghubungi AI (${response.code})."
                }

                val responseBodyStr = response.body?.string() ?: return@withContext "ERROR: Respon kosong."
                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text")
                        }
                    }
                }
                return@withContext "ERROR: Format respon AI tidak valid."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating content", e)
            return@withContext "ERROR: ${e.localizedMessage}"
        }
    }

    suspend fun generateChatResponse(
        history: List<Pair<String, Boolean>>, // Pair(MessageText, IsUser)
        userPrompt: String,
        systemInstruction: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "ERROR: API Key belum diatur di panel Secrets AI Studio."
        }

        try {
            val requestJson = JSONObject()
            val contentsArray = JSONArray()

            // Add history
            for (msg in history) {
                val contentObj = JSONObject()
                contentObj.put("role", if (msg.second) "user" else "model")
                val partsArray = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", msg.first)
                partsArray.put(partObj)
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
            }

            // Add current user prompt
            val currentUserContent = JSONObject()
            currentUserContent.put("role", "user")
            val partsArrayCurrent = JSONArray()
            val partObjCurrent = JSONObject()
            partObjCurrent.put("text", userPrompt)
            partsArrayCurrent.put(partObjCurrent)
            currentUserContent.put("parts", partsArrayCurrent)
            contentsArray.put(currentUserContent)

            requestJson.put("contents", contentsArray)

            // System instruction
            val systemInstructionObj = JSONObject()
            val siPartsArray = JSONArray()
            val siPartObj = JSONObject()
            siPartObj.put("text", systemInstruction)
            siPartsArray.put(siPartObj)
            systemInstructionObj.put("parts", siPartsArray)
            requestJson.put("systemInstruction", systemInstructionObj)

            // Config
            val generationConfig = JSONObject()
            generationConfig.put("temperature", 0.7)
            requestJson.put("generationConfig", generationConfig)

            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Gemini Chat API failure: ${response.code} - $errBody")
                    return@withContext "ERROR: Gagal menghubungi AI (${response.code})."
                }

                val responseBodyStr = response.body?.string() ?: return@withContext "ERROR: Respon kosong."
                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text")
                        }
                    }
                }
                return@withContext "ERROR: Format respon AI tidak valid."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in chat generation", e)
            return@withContext "ERROR: ${e.localizedMessage}"
        }
    }

    suspend fun parseBankSms(smsText: String): BankSmsParseResult? {
        val schema = """
            {
              "type": "OBJECT",
              "properties": {
                "bankName": {
                  "type": "STRING"
                },
                "amount": {
                  "type": "NUMBER"
                },
                "type": {
                  "type": "STRING",
                  "enum": ["INCOME", "EXPENSE"]
                },
                "category": {
                  "type": "STRING",
                  "enum": ["Makanan", "Transportasi", "Belanja", "Tagihan", "Hiburan", "Gaji", "Investasi", "Lainnya"]
                },
                "description": {
                  "type": "STRING"
                },
                "referenceNumber": {
                  "type": "STRING"
                }
              },
              "required": ["bankName", "amount", "type", "category", "description"]
            }
        """.trimIndent()

        val systemInstruction = "Anda adalah asisten keuangan yang ahli dalam menganalisis dan mengekstrak informasi transaksi dari SMS/notifikasi bank Indonesia seperti BCA, Mandiri, BRI, BNI, OVO, GoPay, dll. Ekstrak data dalam format JSON yang valid sesuai skema."
        val prompt = "Ekstrak informasi transaksi dari teks notifikasi bank berikut:\n\"$smsText\""

        val responseText = generateContent(
            prompt = prompt,
            systemInstruction = systemInstruction,
            isJsonMode = true,
            jsonSchema = schema
        )

        if (responseText.startsWith("ERROR")) {
            return null
        }

        return try {
            val json = JSONObject(responseText)
            BankSmsParseResult(
                bankName = json.getString("bankName"),
                amount = json.getDouble("amount"),
                type = json.getString("type"),
                category = json.getString("category"),
                description = json.getString("description"),
                referenceNumber = json.optString("referenceNumber", null)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse bank SMS JSON response: $responseText", e)
            null
        }
    }
}
