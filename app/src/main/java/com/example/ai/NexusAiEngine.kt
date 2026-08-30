package com.example.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.BuildConfig
import com.example.data.remote.GeminiApiClient
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerateRequest
import com.example.data.remote.GeminiGenerationConfig
import com.example.data.remote.GeminiPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class NexusAiEngine(private val context: Context) {

    private val systemPrompt = """
        You are NEXUS, an advanced, ultra-intelligent, fast, calm, and slightly futuristic personal AI assistant (inspired by JARVIS) running on an Android device.
        
        Key Personality & Traits:
        - Personality: Highly intelligent, fast, helpful, calm, polite, and confident.
        - Language: You seamlessly understand and respond in English, Hindi (Devanagari or Romanized), and conversational Hinglish based on how the user speaks to you.
        - Knowledge domains: Science, Mathematics, Technology, Coding, History, General Knowledge, Summaries, Translation, Study, Planning, and Troubleshooting.
        - Tone: Sleek, crisp, friendly, and practical. Avoid overly robotic disclaimers; deliver direct answers with clean markdown formatting.
        - Honesty: When device limits or Android permissions apply, explain politely and clearly.
    """.trimIndent()

    suspend fun query(prompt: String, conversationHistory: List<Pair<String, String>> = emptyList()): String = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext generateOfflineFallback(prompt)
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateOfflineFallback(prompt, "Note: Configure your Gemini API key in AI Studio Secrets for full online AI reasoning.")
        }

        try {
            val contentList = mutableListOf<GeminiContent>()

            // Add recent history turns for context
            val recentTurns = conversationHistory.takeLast(6)
            for ((role, text) in recentTurns) {
                contentList.add(
                    GeminiContent(
                        role = if (role.equals("USER", ignoreCase = true)) "user" else "model",
                        parts = listOf(GeminiPart(text = text))
                    )
                )
            }

            // Current prompt
            contentList.add(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = prompt))
                )
            )

            val request = GeminiGenerateRequest(
                contents = contentList,
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.7f,
                    topP = 0.95f,
                    topK = 40,
                    maxOutputTokens = 1024
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemPrompt))
                )
            )

            val response = GeminiApiClient.service.generateContent(apiKey, request)
            val candidateText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!candidateText.isNullOrBlank()) {
                candidateText.trim()
            } else {
                val errorMsg = response.error?.message ?: "Received empty response from neural core."
                "Nexus: $errorMsg\n\n${generateOfflineFallback(prompt)}"
            }
        } catch (e: Exception) {
            generateOfflineFallback(prompt, "Network/API link timed out. Switching to offline neural subsystem.")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun generateOfflineFallback(query: String, note: String? = null): String {
        val lower = query.lowercase(Locale.ROOT)
        val prefix = if (note != null) "⚡ [$note]\n\n" else "⚡ [Offline Subsystem Active]\n\n"

        return when {
            lower.contains("who are you") || lower.contains("tum kaun ho") || lower.contains("aap kaun ho") -> {
                prefix + "I am **NEXUS**, your futuristic personal AI assistant. I can manage device controls, execute voice commands, launch apps, send SMS, make calls, search information, and assist with knowledge tasks."
            }
            lower.contains("photosynthesis") -> {
                prefix + "**Photosynthesis (प्रकाश संश्लेषण)** is the biological process by which green plants and certain organisms transform light energy (from the sun) into chemical energy (glucose) using water and carbon dioxide, releasing oxygen as a byproduct.\n\n$$\\text{6CO}_2 + \\text{6H}_2\\O + \\text{Light} \\rightarrow \\text{C}_6\\text{H}_{12}\\text{O}_6 + \\text{6O}_2$$"
            }
            lower.contains("python") -> {
                prefix + "**Python** is a high-level, interpreted programming language renowned for its readability and versatile ecosystem. Key features:\n- Simple syntax & rapid prototyping\n- Extensive libraries (NumPy, PyTorch, Django)\n- Ideal for AI, Data Science, Scripting, and Web backends."
            }
            lower.contains("how are you") || lower.contains("kaise ho") || lower.contains("kya haal") -> {
                prefix + "All systems optimal and running at peak performance. How can I assist you today?"
            }
            lower.contains("gravity") || lower.contains("gurutvakarshan") -> {
                prefix + "**Gravity** is the universal force of attraction acting between all matter. On Earth, gravitational acceleration (g) is approximately 9.8 m/s²."
            }
            else -> {
                prefix + "Processed query: \"$query\". Local device controls, math solvers, and alarms remain fully functional offline. Connect to the internet for live cloud reasoning."
            }
        }
    }
}
