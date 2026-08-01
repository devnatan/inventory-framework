package me.devnatan.inventoryframework.runtime.command

import me.devnatan.inventoryframework.minestom.ViewFrame
import me.devnatan.inventoryframework.runtime.view.Failing
import me.devnatan.inventoryframework.runtime.view.ScheduledView
import me.devnatan.inventoryframework.runtime.view.SimplePagination
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.arguments.Argument
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.command.builder.suggestion.SuggestionEntry
import net.minestom.server.entity.Player

class IFExampleCommand(
    private val viewFrame: ViewFrame,
) : Command("ifexample") {
    private val availableViews =
        mapOf(
            "failing" to Failing::class.java,
            "simple-pagination" to SimplePagination::class.java,
            "scheduled" to ScheduledView::class.java,
        )

    private val arg: Argument<String> =
        ArgumentType.String("view").setSuggestionCallback { _, _, suggestion ->
            availableViews.keys.forEach { suggestion.addEntry(SuggestionEntry(it)) }
        }

    init {
        addSyntax({ sender, ctx -> onCommand(sender, ctx) }, arg)
    }

    private fun onCommand(
        sender: CommandSender,
        ctx: CommandContext,
    ) {
        if (sender !is Player) {
            sender.sendMessage("This command can only be executed by players.")
            return
        }

        val view = availableViews[ctx.get(arg)]
        if (view != null) {
            sender.sendMessage("Opened view: ${ctx.get(arg)}")
            try {
                viewFrame.open(view, sender)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            sender.sendMessage("Unknown view: ${ctx.get(arg)}")
            sender.sendMessage("Available views: ${availableViews.keys.joinToString(", ")}")
        }
    }
}
