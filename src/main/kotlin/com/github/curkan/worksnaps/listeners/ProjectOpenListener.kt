package com.github.curkan.worksnaps.listeners

import com.github.curkan.worksnaps.service.WorksnapsService
import com.github.curkan.worksnaps.settings.WorksnapsSettings
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

/**
 * Listener that starts auto-refresh when project is opened
 */
class ProjectOpenListener : ProjectManagerListener {
    companion object {
        private val LOG = Logger.getInstance(ProjectOpenListener::class.java)
    }

    override fun projectOpened(project: Project) {
        LOG.info("Project opened: ${project.name}")
        val settings = WorksnapsSettings.getInstance()
        val service = WorksnapsService.getInstance()

        // Start auto-refresh with configured interval
        LOG.info("Starting auto-refresh with interval: ${settings.updateInterval} seconds")
        service.startAutoRefresh(settings.updateInterval)

        // Trigger initial refresh
        LOG.info("Triggering initial refresh")
        service.refreshData()
    }

    override fun projectClosed(project: Project) {
        // Optionally stop refresh when all projects are closed
        // For now, keep it running as it's an application-level service
    }
}
