package com.github.curkan.worksnaps.settings

import com.github.curkan.worksnaps.service.WorksnapsService
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent

/**
 * Configurable settings UI for Worksnaps plugin
 */
class WorksnapsConfigurable : Configurable {
    private var settingsPanel: DialogPanel? = null
    private val settings = WorksnapsSettings.getInstance()

    // UI Components
    private val apiTokenField = JBPasswordField()
    private val projectIdField = JBTextField()
    private val userIdField = JBTextField()
    private val updateIntervalField = JBTextField()
    private val targetHoursField = JBTextField()
    private val prefixField = JBTextField()
    private val showTimeCheckbox = JBCheckBox("Show worked time")
    private val showActivityCheckbox = JBCheckBox("Show activity percentage")
    private val showRemainingCheckbox = JBCheckBox("Show remaining time")

    override fun getDisplayName(): String = "Worksnaps"

    override fun createComponent(): JComponent {
        settingsPanel = panel {
            group("API Configuration") {
                row {
                    label("API Token:")
                        .widthGroup("labels")
                    cell(apiTokenField)
                        .align(AlignX.FILL)
                        .comment("Get your API token from Worksnaps Profile & Settings → Web Service API")
                }
                row {
                    label("Project ID:")
                        .widthGroup("labels")
                    cell(projectIdField)
                        .align(AlignX.FILL)
                        .comment("Find project ID in URL or via API")
                }
                row {
                    label("User ID:")
                        .widthGroup("labels")
                    cell(userIdField)
                        .align(AlignX.FILL)
                        .comment("Optional: Leave empty to auto-detect from API")
                }
            }

            group("Display Settings") {
                row {
                    label("Prefix:")
                        .widthGroup("labels")
                    cell(prefixField)
                        .align(AlignX.FILL)
                        .comment("Text shown before the statistics (e.g., 'WS:')")
                }
                row {
                    label("Target Hours:")
                        .widthGroup("labels")
                    cell(targetHoursField)
                        .align(AlignX.FILL)
                        .comment("Target working hours per day (default: 8)")
                }
                row {
                    cell(showTimeCheckbox)
                }
                row {
                    cell(showActivityCheckbox)
                }
                row {
                    cell(showRemainingCheckbox)
                }
            }

            group("Update Settings") {
                row {
                    label("Update Interval (seconds):")
                        .widthGroup("labels")
                    cell(updateIntervalField)
                        .align(AlignX.FILL)
                        .comment("How often to refresh data from API (default: 60)")
                }
            }

            group("Help") {
                row {
                    text("""
                        <h3>How to get API Token:</h3>
                        <ol>
                            <li>Log in to your Worksnaps account</li>
                            <li>Go to <b>Profile & Settings → Web Service API</b></li>
                            <li>Click "Show my API Token"</li>
                            <li>Copy and paste it above</li>
                        </ol>
                        <br>
                        <h3>How to get Project ID:</h3>
                        <p>You can find the project ID in the URL when viewing a project in Worksnaps web interface,<br>
                        or by making an API request to: <code>https://api.worksnaps.com/api/projects.xml</code></p>
                        <br>
                        <h3>Activity Colors:</h3>
                        <ul>
                            <li><font color="green">Green</font>: Activity ≥ 80%</li>
                            <li><font color="#FFA500">Yellow</font>: Activity 60-79%</li>
                            <li><font color="red">Red</font>: Activity < 60%</li>
                        </ul>
                    """.trimIndent())
                }
            }
        }

        reset()
        return settingsPanel!!
    }

    override fun isModified(): Boolean {
        return String(apiTokenField.password) != settings.apiToken ||
                projectIdField.text != settings.projectId ||
                userIdField.text != settings.userId ||
                updateIntervalField.text.toIntOrNull() != settings.updateInterval ||
                targetHoursField.text.toDoubleOrNull() != settings.targetHours ||
                prefixField.text != settings.prefix ||
                showTimeCheckbox.isSelected != settings.showTime ||
                showActivityCheckbox.isSelected != settings.showActivity ||
                showRemainingCheckbox.isSelected != settings.showRemaining
    }

    override fun apply() {
        settings.apiToken = String(apiTokenField.password)
        settings.projectId = projectIdField.text
        settings.userId = userIdField.text
        settings.updateInterval = updateIntervalField.text.toIntOrNull() ?: 60
        settings.targetHours = targetHoursField.text.toDoubleOrNull() ?: 8.0
        settings.prefix = prefixField.text
        settings.showTime = showTimeCheckbox.isSelected
        settings.showActivity = showActivityCheckbox.isSelected
        settings.showRemaining = showRemainingCheckbox.isSelected

        // Clear cache and restart auto-refresh with new settings
        val service = WorksnapsService.getInstance()
        service.clearCache()
        service.stopAutoRefresh()
        service.startAutoRefresh(settings.updateInterval)
    }

    override fun reset() {
        apiTokenField.text = settings.apiToken
        projectIdField.text = settings.projectId
        userIdField.text = settings.userId
        updateIntervalField.text = settings.updateInterval.toString()
        targetHoursField.text = settings.targetHours.toString()
        prefixField.text = settings.prefix
        showTimeCheckbox.isSelected = settings.showTime
        showActivityCheckbox.isSelected = settings.showActivity
        showRemainingCheckbox.isSelected = settings.showRemaining
    }
}
