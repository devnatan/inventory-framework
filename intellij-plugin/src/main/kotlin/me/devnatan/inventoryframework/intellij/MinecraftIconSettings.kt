package me.devnatan.inventoryframework.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.APP)
@State(name = "InventoryFrameworkMinecraftSettings", storages = [Storage("inventoryframework-minecraft.xml")])
class MinecraftIconSettings : PersistentStateComponent<MinecraftIconSettings.State> {

    class State {
        var minecraftHome: String = ""
    }

    private var state = State()

    var minecraftHome: String
        get() = state.minecraftHome
        set(value) {
            state.minecraftHome = value.trim()
        }

    override fun getState(): State = state

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    companion object {
        fun getInstance(): MinecraftIconSettings =
            ApplicationManager.getApplication().getService(MinecraftIconSettings::class.java)
    }
}
