package me.devnatan.inventoryframework.runtime

import me.devnatan.inventoryframework.minestom.ViewFrame
import me.devnatan.inventoryframework.runtime.command.GamemodeCommand
import me.devnatan.inventoryframework.runtime.command.IFExampleCommand
import me.devnatan.inventoryframework.runtime.view.Failing
import me.devnatan.inventoryframework.runtime.view.ScheduledView
import me.devnatan.inventoryframework.runtime.view.SimplePagination
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.PlayerSkin
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerSkinInitEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.block.Block
import net.minestom.server.inventory.TransactionOption
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

class SampleServer {
    init {
        val server = MinecraftServer.init()
        val instanceManager = MinecraftServer.getInstanceManager()

        // Create word filled with quartz blocks up to height 50
        val instance = instanceManager.createInstanceContainer()
        instance.setGenerator {
            it.modifier().fillHeight(it.absoluteStart().blockY(), 50, Block.QUARTZ_BLOCK)
        }
        instance.setChunkSupplier(::LightingChunk)

        val handler = MinecraftServer.getGlobalEventHandler()

        handler.addListener(AsyncPlayerConfigurationEvent::class.java) { event ->
            event.spawningInstance = instance
            event.player.respawnPoint = Pos(0.0, 53.0, 0.0)
        }
        handler.addListener(PlayerSkinInitEvent::class.java) { event ->
            event.skin = PlayerSkin.fromUsername(event.player.username)
        }
        handler.addListener(PlayerSpawnEvent::class.java) { event ->
            event.player.inventory.addItemStacks(
                ExampleUtil.getRandomItems(20),
                TransactionOption.ALL,
            )
            event.player.inventory.addItemStack(
                ItemStack.of(Material.OAK_PLANKS, 64),
                TransactionOption.ALL,
            )
        }

        val viewFrame =
            ViewFrame
                .create(handler)
                .with(Failing(), SimplePagination(), ScheduledView())
                .register()

        MinecraftServer
            .getCommandManager()
            .register(IFExampleCommand(viewFrame), GamemodeCommand(viewFrame))

        server.start("0.0.0.0", 25565)
    }
}

fun main() {
    SampleServer()
}
