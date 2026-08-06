package me.devnatan.inventoryframework.intellij

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JLabel

class MinecraftIconSettingsConfigurable : Configurable {

    private var homeField: TextFieldWithBrowseButton? = null

    override fun getDisplayName(): String = "Inventory Framework"

    override fun createComponent(): JComponent {
        val field = TextFieldWithBrowseButton()
        field.addActionListener {
            val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                .withTitle("Minecraft Home Directory")
                .withDescription("Folder containing \"versions\" - usually named .minecraft.")
            val chosen = FileChooser.chooseFile(descriptor, null, null) ?: return@addActionListener
            field.text = chosen.path
        }
        homeField = field

        val comment = JLabel(
            "<html>Used to render real item icons in the inventory preview.<br/>Leave empty to auto-detect the platform default.</html>",
        )

        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Minecraft home:", field)
            .addComponentToRightColumn(comment)
            .addComponentFillVertically(JLabel(), 0)
            .panel
    }

    override fun isModified(): Boolean =
        homeField?.text?.trim().orEmpty() != MinecraftIconSettings.getInstance().minecraftHome

    override fun apply() {
        MinecraftIconSettings.getInstance().minecraftHome = homeField?.text.orEmpty()
    }

    override fun reset() {
        homeField?.text = MinecraftIconSettings.getInstance().minecraftHome
    }

    override fun disposeUIResources() {
        homeField = null
    }
}
