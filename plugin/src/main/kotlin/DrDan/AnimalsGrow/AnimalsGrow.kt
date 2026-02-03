package DrDan.AnimalsGrow

import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.util.Config
import org.slf4j.LoggerFactory
import DrDan.AnimalsGrow.command.AnimalsGrowCommand
import DrDan.AnimalsGrow.config.AnimalsGrowConfig
import DrDan.AnimalsGrow.event.AnimalsGrowEvent
import DrDan.AnimalsGrow.grow_ecs.AnimalsGrowComponent
import DrDan.AnimalsGrow.grow_ecs.AnimalsGrowSystem

private const val PLUGIN_NAME = "AnimalsGrow"

class AnimalsGrow(init: JavaPluginInit) : JavaPlugin(init) {
    private val logger = LoggerFactory.getLogger(AnimalsGrow::class.java)
    private val config: Config<AnimalsGrowConfig> = this.withConfig(PLUGIN_NAME, AnimalsGrowConfig.CODEC)

    companion object {
        /** Global debug flag for verbose logging */
        @JvmStatic
        var DEBUG = false
        
        @Volatile
        private var componentType: ComponentType<EntityStore, AnimalsGrowComponent>? = null

        @JvmStatic
        fun getComponentType(): ComponentType<EntityStore, AnimalsGrowComponent> {
            return componentType ?: throw IllegalStateException(
                "AnimalsGrowComponent not registered. Plugin not started yet."
            )
        }
    }

    override fun setup() {
        logger.info("Registering $PLUGIN_NAME!")
        config.save()
    }

    override fun start() {
        logger.info("Starting $PLUGIN_NAME!")
        
        val growthConfig = config.get().growsUpInto
        AnimalsGrowAction.initialize(growthConfig)
        
        // Register AnimalsGrowComponent
        componentType = entityStoreRegistry.registerComponent(
            AnimalsGrowComponent::class.java
        ) { AnimalsGrowComponent() }
        
        // Register systems - they use AnimalsGrow.getComponentType()
        entityStoreRegistry.registerSystem(AnimalsGrowEvent(growthConfig))
        entityStoreRegistry.registerSystem(AnimalsGrowSystem())

        // Register commands
        commandRegistry.registerCommand(AnimalsGrowCommand())
    }
}
