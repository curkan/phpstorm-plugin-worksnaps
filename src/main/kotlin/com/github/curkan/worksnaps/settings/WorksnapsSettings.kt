package com.github.curkan.worksnaps.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Persistent settings for Worksnaps plugin
 */
@State(
    name = "com.github.curkan.worksnaps.settings.WorksnapsSettings",
    storages = [Storage("WorksnapsSettings.xml")]
)
class WorksnapsSettings : PersistentStateComponent<WorksnapsSettings> {
    var apiToken: String = ""
    var projectId: String = ""
    var userId: String = ""
    var updateInterval: Int = 60
    var targetHours: Double = 8.0
    var showTime: Boolean = true
    var showActivity: Boolean = true
    var showRemaining: Boolean = true
    var prefix: String = "WS:"

    companion object {
        fun getInstance(): WorksnapsSettings {
            return ApplicationManager.getApplication().getService(WorksnapsSettings::class.java)
        }
    }

    override fun getState(): WorksnapsSettings = this

    override fun loadState(state: WorksnapsSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
