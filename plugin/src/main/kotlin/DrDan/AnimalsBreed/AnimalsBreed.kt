package DrDan.AnimalsBreed

import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.util.Config

import DrDan.AnimalsBreed.command.AnimalsBreedCommand
import DrDan.AnimalsBreed.config.AnimalsBreedConfig
import DrDan.AnimalsBreed.event.AnimalsBreedEvent
import DrDan.AnimalsBreed.breed_ecs.AnimalsBreedComponent
import DrDan.AnimalsBreed.breed_ecs.AnimalsBreedSystem

import org.slf4j.LoggerFactory

private const val PLUGIN_NAME = "AnimalsBreed"

class AnimalsBreed(init: JavaPluginInit) : JavaPlugin(init) {
    private val logger = LoggerFactory.getLogger(AnimalsBreed::class.java)
    private val config: Config<AnimalsBreedConfig> = this.withConfig(PLUGIN_NAME, AnimalsBreedConfig.CODEC)

    companion object {
        @Volatile
        private var componentType: ComponentType<EntityStore, AnimalsBreedComponent>? = null
        @JvmStatic
        fun getComponentType(): ComponentType<EntityStore, AnimalsBreedComponent> {
            return componentType ?: throw IllegalStateException(
                "AnimalsBreedComponent not registered. Plugin not started yet."
            )
        }
    }

    override fun setup() {
        logger.info("Registering $PLUGIN_NAME!")
        config.save()
    }

    override fun start() {
        logger.info("Starting $PLUGIN_NAME!")
        
        val fullConfig = config.get()
        AnimalsBreedAction.initialize(fullConfig)
        
        // Register components
        componentType = entityStoreRegistry.registerComponent(
            AnimalsBreedComponent::class.java
        ) { AnimalsBreedComponent() }
        
        // Register systems
        entityStoreRegistry.registerSystem(AnimalsBreedEvent(fullConfig.breedGroup))
        entityStoreRegistry.registerSystem(AnimalsBreedSystem())
        
        // Register commands
        commandRegistry.registerCommand(AnimalsBreedCommand())
    }
}
