package me.devnatan.inventoryframework.runtime.command

import me.devnatan.inventoryframework.minestom.ViewFrame
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.arguments.Argument
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.command.builder.suggestion.SuggestionEntry
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player

class GamemodeCommand(
    private val viewFrame: ViewFrame,
) : Command("gamemode") {
    private val gamemodeArg: Argument<GameMode> =
        ArgumentType.Enum("gameMode", GameMode::class.java).setSuggestionCallback { _, _, suggestion ->
            for (gameMode in GameMode.entries) {
                suggestion.addEntry(SuggestionEntry(gameMode.name.lowercase()))
            }
        }

    init {
        setDefaultExecutor(::onCommand)
        addSyntax(::onCommand, gamemodeArg)
    }

    private fun onCommand(
        sender: CommandSender,
        ctx: CommandContext,
    ) {
        if (sender !is Player) {
            sender.sendMessage("This command can only be executed by players.")
            return
        }

        val gameMode: GameMode = ctx.get(gamemodeArg)
        sender.gameMode = gameMode
    }
}
