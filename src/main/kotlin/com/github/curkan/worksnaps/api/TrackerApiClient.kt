package com.github.curkan.worksnaps.api

import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/**
 * Client for interacting with Tracker (Redmine) API to fetch hours data
 */
class TrackerApiClient(
    private val apiKey: String,
    private val endpointUrl: String
) {
    companion object {
        private const val TIMEOUT_MS = 30000
        private val LOG = Logger.getInstance(TrackerApiClient::class.java)
    }

    private val gson = Gson()

    /**
     * Fetch today's hours data from tracker API
     */
    fun getMyHours(): TrackerHoursData? {
        LOG.info("Fetching my hours from tracker API...")

        try {
            val url = URL(endpointUrl)
            LOG.info("Requesting: $url")

            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("X-Redmine-API-Key", apiKey)
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS

            val responseCode = connection.responseCode
            LOG.info("Response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                LOG.info("Response received: $response")
                connection.disconnect()
                val data = gson.fromJson(response, TrackerHoursData::class.java)
                LOG.info("Parsed tracker data: userMinutes=${data.userMinutes}, hoursOnNow=${data.hoursOnNow}")
                return data
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
                LOG.error("Failed to get hours. Response code: $responseCode, Error: $errorResponse")
                connection.disconnect()
            }
        } catch (e: SocketTimeoutException) {
            LOG.warn("Timeout while fetching hours from tracker API (${TIMEOUT_MS}ms).")
        } catch (e: UnknownHostException) {
            LOG.warn("Cannot resolve host: ${e.message}. Check your internet connection.")
        } catch (e: Exception) {
            LOG.warn("Exception while fetching tracker hours: ${e.message}")
        }

        return null
    }
}

/**
 * Data class representing tracker hours response
 */
data class TrackerHoursData(
    val month_hours: Int = 0,
    val month_hours_user: Int = 0,
    val hours_on_now: Int = 0,
    val user_minutes: Int = 0
) {
    /** Worked minutes today (from user_minutes field) */
    val userMinutes: Int get() = user_minutes

    /** Required hours for the month */
    val monthHours: Int get() = month_hours

    /** Hours worked so far in the month */
    val monthHoursUser: Int get() = month_hours_user

    /** Hours the user is currently tracked on */
    val hoursOnNow: Int get() = hours_on_now
}
