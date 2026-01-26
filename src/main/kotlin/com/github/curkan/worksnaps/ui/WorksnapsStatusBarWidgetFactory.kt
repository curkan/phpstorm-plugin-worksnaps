package com.github.curkan.worksnaps.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory

/**
 * Factory for creating Worksnaps status bar widget
 */
class WorksnapsStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = WorksnapsStatusBarWidget.ID

    override fun getDisplayName(): String = "Worksnaps"

    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget {
        return WorksnapsStatusBarWidget(project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {
        Disposer.dispose(widget)
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}
