package DrDan.AnimalsGrow

import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.component.ComponentRegistryProxy
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.util.Config
import com.hypixel.hytale.server.core.command.system.CommandRegistry

import java.nio.file.Path;
import org.slf4j.LoggerFactory

import DrDan.AnimalsGrow.command.AnimalsGrowCommand
import DrDan.AnimalsGrow.command.AnimalsGrowTestCommand
import DrDan.AnimalsGrow.config.AnimalsGrowConfig
import DrDan.AnimalsGrow.event.AnimalsGrowEvent
import DrDan.AnimalsGrow.grow_ecs.AnimalsGrowComponent
import DrDan.AnimalsGrow.grow_ecs.AnimalsGrowSystem

private const val PLUGIN_NAME = "AnimalsGrow"
private const val CONFIG_VERSION = "2"

class AnimalsGrow(init: JavaPluginInit) : JavaPlugin(init) {
    private var logger = LoggerFactory.getLogger(AnimalsGrow::class.java)
    private var config: Config<AnimalsGrowConfig> = Config<AnimalsGrowConfig>(getDataDirectory(), PLUGIN_NAME, AnimalsGrowConfig.CODEC)

    companion object {
        @Volatile
        private var componentType: ComponentType<EntityStore, AnimalsGrowComponent>? = null

        @JvmStatic
        fun getComponentType(): ComponentType<EntityStore, AnimalsGrowComponent> {
            return componentType ?: throw IllegalStateException(
                "AnimalsGrowComponent not registered. Plugin not started yet."
            )
        }
    }

    fun callSetup() { setup() }
    override fun setup() {
        logger.info("Registering $PLUGIN_NAME!")

        config.load().join()
        config.save()
    }

    override fun start() { start(entityStoreRegistry, commandRegistry) }
    fun start(entityStoreRegistry: ComponentRegistryProxy<EntityStore>, commandRegistry: CommandRegistry) {
        logger.info("Starting $PLUGIN_NAME!")

        val growthConfig = config.get().growsUpInto
        AnimalsGrowAction.initialize(growthConfig)
        
        // Register components
        componentType = entityStoreRegistry.registerComponent(
            AnimalsGrowComponent::class.java
        ) { AnimalsGrowComponent() }
        
        // Register systems
        entityStoreRegistry.registerSystem(AnimalsGrowEvent(growthConfig))
        entityStoreRegistry.registerSystem(AnimalsGrowSystem())

        // Register commands
        commandRegistry.registerCommand(AnimalsGrowCommand())
        commandRegistry.registerCommand(AnimalsGrowTestCommand())
    }
}
