package com.github.curkan.worksnaps.ui

import com.github.curkan.worksnaps.api.WorksnapsData
import com.github.curkan.worksnaps.service.WorksnapsService
import com.github.curkan.worksnaps.settings.WorksnapsSettings
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.Consumer
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import kotlin.math.abs

/**
 * Status bar widget that displays Worksnaps statistics with colored text
 */
class WorksnapsStatusBarWidget(private val project: Project) : CustomStatusBarWidget {
    companion object {
        const val ID = "WorksnapsStatusBarWidget"
        private val LOG = Logger.getInstance(WorksnapsStatusBarWidget::class.java)

        // Colors
        private val COLOR_GREEN = Color(0, 170, 0)
        private val COLOR_YELLOW = Color(255, 170, 0)
        private val COLOR_RED = Color(255, 0, 0)
        private val COLOR_ORANGE = Color(255, 165, 0)
    }

    private val service = WorksnapsService.getInstance()
    private val settings = WorksnapsSettings.getInstance()
    private var statusBar: StatusBar? = null
    private val component = SimpleColoredComponent()

    // Use status bar's default text color (grayed/inactive)
    private val defaultTextAttributes: SimpleTextAttributes
        get() = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, UIUtil.getInactiveTextColor())

    init {
        LOG.info("WorksnapsStatusBarWidget initialized for project: ${project.name}")

        // Configure component appearance to match status bar
        component.isOpaque = false
        component.border = null

        // Add mouse click handler
        component.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                LOG.info("Widget clicked! Triggering manual refresh...")
                service.refreshData()
                updateComponent()
            }
        })

        // Start auto-refresh when widget is created
        service.startAutoRefresh(settings.updateInterval)
        updateComponent()
    }

    override fun ID(): String = ID

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
        LOG.info("Widget installed to status bar")
    }

    override fun dispose() {
        LOG.info("Widget disposed")
    }

    override fun getComponent(): JComponent {
        return component
    }

    private fun getTooltipText(): String {
        val error = service.getLastError()
        return when {
            error != null -> "Worksnaps Error: $error\nClick to retry"
            service.isUsingCachedData() -> "Worksnaps (using cached data)\nClick to refresh"
            settings.apiToken.isEmpty() || settings.projectId.isEmpty() -> "Worksnaps: Not configured\nGo to Settings → Tools → Worksnaps"
            else -> "Worksnaps Time Tracker\nClick to refresh"
        }
    }

    fun updateComponent() {
        component.clear()
        component.toolTipText = getTooltipText()
        formatOutput()
    }

    /**
     * Format output using SimpleColoredComponent
     */
    private fun formatOutput() {
        // Check if settings are configured
        if (settings.apiToken.isEmpty() || settings.projectId.isEmpty()) {
            component.append("${settings.prefix} N/A", defaultTextAttributes)
            return
        }

        // Get data from service
        val data = service.getData()

        if (data == null) {
            val errorText = if (service.getLastError() != null) {
                "${settings.prefix} ⚠ Error"
            } else {
                "${settings.prefix} Loading..."
            }
            component.append(errorText, defaultTextAttributes)
            return
        }

        buildOutputComponents(data, service.isUsingCachedData())
    }

    /**
     * Build formatted output with colored components
     */
    private fun buildOutputComponents(data: WorksnapsData, isUsingCache: Boolean) {
        // Add prefix
        component.append(settings.prefix, defaultTextAttributes)

        // Add time if enabled
        if (settings.showTime) {
            component.append(" ", defaultTextAttributes)
            val timeString = formatTime(data.hours)
            component.append(timeString, defaultTextAttributes)

            // Add remaining time with color if enabled
            if (settings.showRemaining) {
                component.append(" ", defaultTextAttributes)
                formatRemainingTimeWithColor(data.hours)
            }
        }

        // Add activity with color if enabled
        if (settings.showActivity) {
            component.append(" | ", defaultTextAttributes)
            formatActivityWithColor(data.activity)
        }

        // Add error indicator if using cached data
        if (isUsingCache) {
            component.append(" ⚠", SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, COLOR_ORANGE))
        }
    }

    /**
     * Format activity with color based on percentage
     */
    private fun formatActivityWithColor(activity: Int) {
        val color = when {
            activity >= 80 -> COLOR_GREEN // Green for good
            activity >= 60 -> COLOR_YELLOW // Yellow for medium
            else -> COLOR_RED // Red for low
        }
        component.append("${activity}%", SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, color))
    }

    /**
     * Format remaining time with color (green for overtime, red for remaining)
     */
    private fun formatRemainingTimeWithColor(workedHours: Double) {
        val targetMinutes = (settings.targetHours * 60).toInt()
        val workedMinutes = (workedHours * 60).toInt()
        val roundedWorkedMinutes = ((workedMinutes + 5) / 10) * 10

        val remainingMinutes = targetMinutes - roundedWorkedMinutes

        // Round to nearest 10 minutes, correctly handling negative numbers
        val roundedRemaining = if (remainingMinutes >= 0) {
            ((remainingMinutes + 5) / 10) * 10  // Round up for positive
        } else {
            ((remainingMinutes - 5) / 10) * 10  // Round down for negative
        }

        val absMinutes = abs(roundedRemaining)
        val hours = absMinutes / 60
        val mins = absMinutes % 60

        val timeStr = if (roundedRemaining < 0) {
            String.format("(+%d:%02d)", hours, mins)
        } else {
            String.format("(-%d:%02d)", hours, mins)
        }

        val color = if (roundedRemaining < 0) COLOR_GREEN else COLOR_RED // Green for overtime, Red for remaining

        component.append(timeStr, SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, color))
    }

    /**
     * Format hours to HH:MM format (rounded to 10 minutes)
     */
    private fun formatTime(hours: Double): String {
        val totalMinutes = (hours * 60).toInt()
        val roundedMinutes = ((totalMinutes + 5) / 10) * 10

        val displayHours = roundedMinutes / 60
        val displayMins = roundedMinutes % 60

        return String.format("%d:%02d", displayHours, displayMins)
    }

}
