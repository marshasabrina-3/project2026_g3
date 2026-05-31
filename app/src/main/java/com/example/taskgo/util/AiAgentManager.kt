package com.example.taskgo.util

import com.example.taskgo.data.model.TaskCategory
import com.example.taskgo.data.model.TaskType
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * A simple AI Agent manager using Google's Gemini (Free tier).
 * This agent acts as a "Digital Employee" with the Skill: Smart Search.
 */
object AiAgentManager {
    // You'll need to get a free API key from https://aistudio.google.com/
    // For now, this is a placeholder. You can set it via an Environment variable or local.properties
    private const val API_KEY = "YOUR_FREE_GEMINI_API_KEY"
    
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = API_KEY
    )

    data class AiFilterResult(
        val category: TaskCategory? = null,
        val campus: String? = null,
        val type: TaskType? = null,
        val query: String = ""
    )

    data class TaskSuggestion(
        val category: TaskCategory? = null,
        val campus: String? = null,
        val suggestedTags: List<String> = emptyList()
    )

    suspend fun suggestMetadata(title: String, description: String): TaskSuggestion = withContext(Dispatchers.IO) {
        val combinedInput = "$title $description"
        
        if (API_KEY == "YOUR_FREE_GEMINI_API_KEY") {
            val heuristic = basicHeuristicSearch(combinedInput)
            return@withContext TaskSuggestion(heuristic.category, heuristic.campus)
        }

        val prompt = """
            Analyze this task posting for a university marketplace:
            Title: "$title"
            Desc: "$description"
            
            Categories: ${TaskCategory.entries.joinToString()}
            Campuses: UTMKL, UTMJB
            
            Rules:
            1. If it mentions "JB" or "Johor", select UTMJB.
            2. If it mentions "KL" or "Kuala Lumpur", select UTMKL.
            3. If they need "food", "eat", "buy", select FOOD category.
            4. Be smart about context.
            
            Return ONLY JSON:
            {
              "category": "MATCHING_CATEGORY_NAME",
              "campus": "MATCHING_CAMPUS_NAME",
              "tags": ["relevant", "keywords"]
            }
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            val jsonStr = response.text?.substringAfter("{")?.substringBeforeLast("}")?.let { "{$it}" }
            if (jsonStr != null) {
                val json = JSONObject(jsonStr)
                TaskSuggestion(
                    category = try { TaskCategory.valueOf(json.getString("category")) } catch (e: Exception) { null },
                    campus = if (json.optString("campus") != "NULL" && json.optString("campus") != "") json.optString("campus") else null,
                    suggestedTags = json.optJSONArray("tags")?.let { arr -> List(arr.length()) { i -> arr.getString(i) } } ?: emptyList()
                )
            } else {
                val heuristic = basicHeuristicSearch(combinedInput)
                TaskSuggestion(heuristic.category, heuristic.campus)
            }
        } catch (e: Exception) {
            val heuristic = basicHeuristicSearch(combinedInput)
            TaskSuggestion(heuristic.category, heuristic.campus)
        }
    }

    suspend fun generateDisputeArbitration(
        taskTitle: String,
        taskDescription: String,
        chatTranscript: String
    ): String = withContext(Dispatchers.IO) {
        if (API_KEY == "YOUR_FREE_GEMINI_API_KEY") {
            return@withContext "Agentic Arbitrator: Insufficient data for analysis (Offline/No API Key)."
        }

        val prompt = """
            You are the TaskGO Dispute Arbitrator. 
            Review this task and the chat history to provide a summary and recommendation for the Administrator.
            
            Task: "$taskTitle"
            Goal: "$taskDescription"
            
            Chat History:
            $chatTranscript
            
            Analyze:
            1. Did the runner provide evidence of completion?
            2. Is the requester being unreasonable?
            3. Who is likely at fault?
            
            Provide a short "Arbitration Summary" (max 100 words).
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            response.text ?: "No analysis generated."
        } catch (e: Exception) {
            "Arbitrator Error: ${e.message}"
        }
    }

    suspend fun processSmartSearch(userInput: String): AiFilterResult = withContext(Dispatchers.IO) {
        if (API_KEY == "YOUR_FREE_GEMINI_API_KEY") {
            // Fallback for demo if no key is provided
            return@withContext basicHeuristicSearch(userInput)
        }

        val prompt = """
            You are TaskGO Assistant, a digital employee for a student marketplace.
            Your task is to convert user natural language search into structured filters.
            
            Categories: ${TaskCategory.entries.joinToString { it.name }}
            Campuses: UTMKL, UTMJB
            Types: REQUEST, SERVICE
            
            User Input: "$userInput"
            
            Return ONLY a JSON object with:
            {
              "category": "CATEGORY_NAME_OR_NULL",
              "campus": "CAMPUS_NAME_OR_NULL",
              "type": "TYPE_NAME_OR_NULL",
              "query": "EXTRACTED_KEYWORD_OR_EMPTY"
            }
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            val jsonStr = response.text?.substringAfter("{")?.substringBeforeLast("}")?.let { "{$it}" }
            if (jsonStr != null) {
                val json = JSONObject(jsonStr)
                AiFilterResult(
                    category = try { TaskCategory.valueOf(json.getString("category")) } catch (e: Exception) { null },
                    campus = if (json.optString("campus") != "NULL") json.optString("campus") else null,
                    type = try { TaskType.valueOf(json.getString("type")) } catch (e: Exception) { null },
                    query = json.optString("query", "")
                )
            } else {
                basicHeuristicSearch(userInput)
            }
        } catch (e: Exception) {
            basicHeuristicSearch(userInput)
        }
    }

    private fun basicHeuristicSearch(input: String): AiFilterResult {
        val lower = input.lowercase()
        val cat = TaskCategory.entries.find { it.name.lowercase().replace("_", " ") in lower }
        val campus = if ("utmkl" in lower || "kl" in lower) "UTMKL" else if ("utmjb" in lower || "jb" in lower) "UTMJB" else null
        val type = if ("service" in lower || "offer" in lower) TaskType.SERVICE else if ("request" in lower || "need" in lower || "help" in lower) TaskType.REQUEST else null
        
        return AiFilterResult(cat, campus, type, input)
    }
}
