package com.github.curkan.worksnaps.api

import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Base64

/**
 * Client for interacting with Worksnaps API
 */
class WorksnapsApiClient(
    private val apiToken: String,
    private val projectId: String,
    private var userId: String? = null
) {
    companion object {
        private const val API_BASE_URL = "https://api.worksnaps.com/api"
        private const val TIMEOUT_MS = 30000
        private val LOG = Logger.getInstance(WorksnapsApiClient::class.java)
    }

    private val gson = Gson()

    /**
     * Get user ID from API if not provided
     */
    fun getUserId(): String? {
        if (userId != null) {
            LOG.info("Using cached user ID: $userId")
            return userId
        }

        LOG.info("Fetching user ID from API...")
        try {
            val url = URL("$API_BASE_URL/me.xml")
            LOG.info("Requesting: ${url}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", getBasicAuthHeader())
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS

            val responseCode = connection.responseCode
            LOG.info("Response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                LOG.info("Response received (${response.length} chars)")
                // Extract user ID from XML: <id>12345</id>
                val idRegex = """<id>(\d+)</id>""".toRegex()
                val match = idRegex.find(response)
                userId = match?.groupValues?.get(1)
                LOG.info("User ID extracted: $userId")
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
                LOG.error("Failed to get user ID. Response code: $responseCode, Error: $errorResponse")
            }

            connection.disconnect()
        } catch (e: Exception) {
            LOG.error("Exception while getting user ID", e)
            e.printStackTrace()
        }

        return userId
    }

    /**
     * Get time entries for today
     */
    fun getTodayTimeEntries(): WorksnapsData? {
        LOG.info("Getting today's time entries...")
        val currentUserId = getUserId()
        if (currentUserId == null) {
            LOG.error("Cannot get time entries: User ID is null")
            return null
        }

        try {
            // Calculate today's timestamp range
            val todayStart = LocalDate.now()
                .atStartOfDay(ZoneId.of("UTC"))
                .toEpochSecond()
            val now = Instant.now().epochSecond

            // Build URL with parameters
            val urlString = "$API_BASE_URL/projects/$projectId/time_entries.xml" +
                    "?from_timestamp=$todayStart&to_timestamp=$now&user_ids=$currentUserId"

            LOG.info("Requesting time entries: $urlString")
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", getBasicAuthHeader())
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS

            val responseCode = connection.responseCode
            LOG.info("Time entries response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                LOG.info("Time entries response received (${response.length} chars)")
                connection.disconnect()
                val data = parseTimeEntries(response)
                LOG.info("Parsed data: hours=${data.hours}, activity=${data.activity}")
                return data
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
                LOG.error("Failed to get time entries. Response code: $responseCode, Error: $errorResponse")
            }

            connection.disconnect()
        } catch (e: Exception) {
            LOG.error("Exception while getting time entries", e)
            e.printStackTrace()
        }

        return null
    }

    /**
     * Parse XML response and calculate statistics
     */
    private fun parseTimeEntries(xmlData: String): WorksnapsData {
        // Check for errors
        if (xmlData.contains("<error>")) {
            return WorksnapsData(0.0, 0)
        }

        // Check if there are any time entries
        if (!xmlData.contains("<time_entry>")) {
            return WorksnapsData(0.0, 0)
        }

        // Extract durations (in minutes)
        val durationRegex = """<duration_in_minutes>(\d+)</duration_in_minutes>""".toRegex()
        val durations = durationRegex.findAll(xmlData)
            .map { it.groupValues[1].toInt() }
            .toList()

        val totalMinutes = durations.sum()

        // Extract activity levels (0-10 scale)
        val activityRegex = """<activity_level>(\d+)</activity_level>""".toRegex()
        val activities = activityRegex.findAll(xmlData)
            .map { it.groupValues[1].toInt() * 10 } // Convert to percentage
            .toList()

        val avgActivity = if (activities.isNotEmpty()) {
            activities.average().toInt()
        } else {
            0
        }

        // Convert minutes to hours
        val hours = totalMinutes / 60.0

        return WorksnapsData(hours, avgActivity)
    }

    /**
     * Create Basic Auth header
     */
    private fun getBasicAuthHeader(): String {
        val credentials = "$apiToken:"
        val encoded = Base64.getEncoder().encodeToString(credentials.toByteArray())
        return "Basic $encoded"
    }
}

/**
 * Data class representing Worksnaps statistics
 */
data class WorksnapsData(
    val hours: Double,
    val activity: Int
)
