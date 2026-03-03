package com.github.curkan.worksnaps.service

import com.github.curkan.worksnaps.api.TrackerApiClient
import com.github.curkan.worksnaps.api.TrackerHoursData
import com.github.curkan.worksnaps.api.WorksnapsApiClient
import com.github.curkan.worksnaps.api.WorksnapsData
import com.github.curkan.worksnaps.settings.WorksnapsSettings
import com.github.curkan.worksnaps.ui.WorksnapsStatusBarWidget
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.WindowManager
import kotlinx.coroutines.*
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Instant

/**
 * Service for managing Worksnaps data fetching and caching
 */
@Service
class WorksnapsService {
    private var cachedData: WorksnapsData? = null
    private var cacheTimestamp: Long = 0
    private var lastError: String? = null
    private var isRefreshing = false

    private var cachedTrackerData: TrackerHoursData? = null
    private var trackerCacheTimestamp: Long = 0
    private var trackerLastError: String? = null
    private var isTrackerRefreshing = false

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var refreshJob: Job? = null

    companion object {
        fun getInstance(): WorksnapsService {
            return ApplicationManager.getApplication().getService(WorksnapsService::class.java)
        }

        private const val CACHE_TTL_SECONDS = 60
        private val LOG = Logger.getInstance(WorksnapsService::class.java)
    }

    /**
     * Get current Worksnaps data (from cache or fresh)
     */
    fun getData(): WorksnapsData? {
        val settings = WorksnapsSettings.getInstance()

        // Check if settings are configured
        if (settings.apiToken.isEmpty() || settings.projectId.isEmpty()) {
            return null
        }

        // Return cached data if still valid
        if (isCacheValid()) {
            return cachedData
        }

        // Trigger refresh if not already in progress
        if (!isRefreshing) {
            refreshData()
        }

        return cachedData
    }

    /**
     * Get current tracker hours data (from cache or fresh)
     */
    fun getTrackerData(): TrackerHoursData? {
        val settings = WorksnapsSettings.getInstance()

        if (!settings.showTrackerRemaining || settings.redmineApiToken.isEmpty() || settings.trackerEndpointUrl.isEmpty()) {
            return null
        }

        if (isTrackerCacheValid()) {
            return cachedTrackerData
        }

        if (!isTrackerRefreshing) {
            refreshTrackerData()
        }

        return cachedTrackerData
    }

    /**
     * Get last tracker error message
     */
    fun getTrackerLastError(): String? = trackerLastError

    /**
     * Get last error message
     */
    fun getLastError(): String? = lastError

    /**
     * Check if using cached data due to error
     */
    fun isUsingCachedData(): Boolean {
        return lastError != null && cachedData != null
    }

    /**
     * Refresh data from API
     */
    fun refreshData() {
        val settings = WorksnapsSettings.getInstance()

        LOG.info("Refresh data requested")

        if (settings.apiToken.isEmpty() || settings.projectId.isEmpty()) {
            LOG.warn("Refresh aborted: API token or project ID is empty")
            lastError = "API Token or Project ID not configured"
            return
        }

        LOG.info("Starting refresh with token: ${settings.apiToken.take(10)}..., projectId: ${settings.projectId}")
        isRefreshing = true
        lastError = null

        coroutineScope.launch {
            try {
                val client = WorksnapsApiClient(
                    settings.apiToken,
                    settings.projectId,
                    settings.userId.ifEmpty { null }
                )

                LOG.info("Calling API client...")
                val data = client.getTodayTimeEntries()

                if (data != null) {
                    LOG.info("Data received successfully: hours=${data.hours}, activity=${data.activity}")
                    cachedData = data
                    cacheTimestamp = Instant.now().epochSecond
                    lastError = null
                } else {
                    LOG.warn("API returned null data - server might be unavailable or request timed out")
                    lastError = "Failed to fetch data from API"
                }

                // Also refresh tracker data if configured
                if (settings.showTrackerRemaining && settings.redmineApiToken.isNotEmpty() && settings.trackerEndpointUrl.isNotEmpty()) {
                    refreshTrackerData()
                }
            } catch (e: SocketTimeoutException) {
                lastError = "API request timed out. The server might be slow or unavailable."
                LOG.warn("Timeout during refresh: ${e.message}")
            } catch (e: UnknownHostException) {
                lastError = "No internet connection or server unavailable."
                LOG.warn("Network error during refresh: ${e.message}")
            } catch (e: Exception) {
                lastError = e.message ?: "Unknown error"
                LOG.warn("Exception during refresh: ${e.message}")
            } finally {
                isRefreshing = false
                LOG.info("Refresh completed. Error: $lastError")

                // Update status bar widget
                updateStatusBar()
            }
        }
    }

    /**
     * Refresh tracker hours data from Redmine API
     */
    fun refreshTrackerData() {
        val settings = WorksnapsSettings.getInstance()

        if (settings.redmineApiToken.isEmpty() || settings.trackerEndpointUrl.isEmpty()) {
            trackerLastError = "Redmine API key or Tracker endpoint URL not configured"
            return
        }

        LOG.info("Starting tracker data refresh...")
        isTrackerRefreshing = true
        trackerLastError = null

        coroutineScope.launch {
            try {
                val client = TrackerApiClient(settings.redmineApiToken, settings.trackerEndpointUrl)
                val data = client.getMyHours()

                if (data != null) {
                    LOG.info("Tracker data received: userMinutes=${data.userMinutes}")
                    cachedTrackerData = data
                    trackerCacheTimestamp = Instant.now().epochSecond
                    trackerLastError = null
                } else {
                    LOG.warn("Tracker API returned null data")
                    trackerLastError = "Failed to fetch data from Tracker API"
                }
            } catch (e: Exception) {
                trackerLastError = e.message ?: "Unknown error"
                LOG.warn("Exception during tracker refresh: ${e.message}")
            } finally {
                isTrackerRefreshing = false
                updateStatusBar()
            }
        }
    }

    /**
     * Update status bar widget for all open projects
     */
    private fun updateStatusBar() {
        ApplicationManager.getApplication().invokeLater {
            try {
                val projects = ProjectManager.getInstance().openProjects
                for (project in projects) {
                    val statusBar = WindowManager.getInstance()?.getStatusBar(project)
                    val widget = statusBar?.getWidget(WorksnapsStatusBarWidget.ID) as? WorksnapsStatusBarWidget
                    widget?.updateComponent()
                    LOG.info("Status bar updated for project: ${project.name}")
                }
            } catch (e: Exception) {
                LOG.error("Failed to update status bar", e)
            }
        }
    }

    /**
     * Start auto-refresh with specified interval
     */
    fun startAutoRefresh(intervalSeconds: Int = 60) {
        LOG.info("startAutoRefresh called with interval: $intervalSeconds seconds")
        stopAutoRefresh()

        LOG.info("Launching auto-refresh coroutine...")
        refreshJob = coroutineScope.launch {
            LOG.info("Auto-refresh coroutine started")
            while (isActive) {
                LOG.info("Auto-refresh cycle: waiting ${intervalSeconds}s before next refresh")
                delay(intervalSeconds * 1000L)
                LOG.info("Auto-refresh cycle: triggering refresh now")
                refreshData()
            }
            LOG.info("Auto-refresh coroutine stopped")
        }
        LOG.info("Auto-refresh job launched: ${refreshJob != null}")
    }

    /**
     * Stop auto-refresh
     */
    fun stopAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    /**
     * Check if cache is still valid
     */
    private fun isCacheValid(): Boolean {
        if (cachedData == null) return false

        val now = Instant.now().epochSecond
        val age = now - cacheTimestamp

        return age < CACHE_TTL_SECONDS
    }

    /**
     * Check if tracker cache is still valid
     */
    private fun isTrackerCacheValid(): Boolean {
        if (cachedTrackerData == null) return false

        val now = Instant.now().epochSecond
        val age = now - trackerCacheTimestamp

        return age < CACHE_TTL_SECONDS
    }

    /**
     * Clear cache
     */
    fun clearCache() {
        cachedData = null
        cacheTimestamp = 0
        lastError = null
        cachedTrackerData = null
        trackerCacheTimestamp = 0
        trackerLastError = null
    }
}
