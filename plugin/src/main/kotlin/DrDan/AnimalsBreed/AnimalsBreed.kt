package DrDan.AnimalsBreed

import com.hypixel.hytale.assetstore.AssetPack
import com.hypixel.hytale.server.npc.NPCPlugin
import com.hypixel.hytale.common.util.FormatUtil
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.util.Config
import com.hypixel.hytale.server.core.HytaleServer
import com.hypixel.hytale.server.core.asset.AssetModule
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.component.ComponentRegistryProxy
import com.hypixel.hytale.server.core.asset.LoadAssetEvent
import com.hypixel.hytale.logger.sentry.SkipSentryException
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.command.system.CommandRegistry
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction

import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.Paths
import java.util.logging.Level
import org.slf4j.LoggerFactory
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.util.concurrent.TimeUnit

import DrDan.AnimalsBreed.event.AnimalsBreedEvent
import DrDan.AnimalsBreed.config.AnimalsBreedConfig
import DrDan.AnimalsBreed.command.AnimalsBreedCommand
import DrDan.AnimalsBreed.breed_ecs.AnimalsBreedSystem
import DrDan.AnimalsBreed.registry.AnimalsBreedRegistry
import DrDan.AnimalsBreed.breed_ecs.AnimalsBreedComponent
import DrDan.AnimalsBreed.resource_creator.ResourceCreator
import DrDan.AnimalsBreed.registry.AnimalsBreedRegistrySystem
import DrDan.AnimalsBreed.builders.BuilderActionBreed

private const val PLUGIN_NAME = "AnimalsBreed"

class AnimalsBreed(init: JavaPluginInit) : JavaPlugin(init) {
    private var logger = LoggerFactory.getLogger(AnimalsBreed::class.java)
    private var config: Config<AnimalsBreedConfig> = Config<AnimalsBreedConfig>(getDataDirectory(), PLUGIN_NAME, AnimalsBreedConfig.CODEC)
    
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

    fun callSetup() { setup() }
    override fun setup() {
        logger.info("Registering $PLUGIN_NAME!")

        config.load().join()
        config.save()


        val assetPath = AssetModule.get().getBaseAssetPack().packLocation
        val rc = ResourceCreator(assetPath)
        // rc.extractAsset(jsonFileToGet)
        // rc.mergePatch(jsonFileToGet, """{"Modify":{"AttractiveItemSet":["Ingredient_Fibre"]}}""")
        val tamedNPCs = rc.listFilesInZipPath("Server/NPC/Roles/Creature/Livestock/Tamed")

        // for (npcPath in tamedNPCs) {
        //     // logger.info("Tamed NPC JSON found in ZIP: $npcPath")
        //     rc.mergePatchArrayRemove(npcPath, """{"Modify":{"AttractiveItemSet":["Tool_Feedbag"]}}""")
        // }

        // Override C:\Users\YouMi\Downloads\New folder\Assets\Server\Item\Items\Tool\Feedbag\Tool_Feedbag.json
        // Override with: /workspace/plugin/src/main/kotlin/DrDan/overrides
        // rc.extractAsset(Paths.get("Server/Item/Items/Tool/Feedbag/Tool_Feedbag.json"), Paths.get("overrides/feedbagBreed.json"))

        // Print contents of the folder structure inside getDataDirectory()
        // Navigate up one directory from the plugin data directory (safe fallback if parent is null)
        val dataDir = getDataDirectory().toAbsolutePath().parent ?: getDataDirectory()
        logger.info("getDataDirectory path: $dataDir")
        logger.info("Data directory contents:")
        var lifecycleJar: Path? = null
        try {
            Files.walk(dataDir).use { paths ->
                paths.filter { Files.isRegularFile(it) }.forEach { path ->
                    // logger.info("- ${dataDir.relativize(path)}")
                    if (lifecycleJar == null && path.fileName.toString().matches(Regex("LifeCycle-.*\\.jar"))) {
                        lifecycleJar = path
                    }
                }
            }
            if (lifecycleJar != null) {
                logger.info("Found LifeCycle jar: $lifecycleJar")
            } else {
                logger.info("No LifeCycle-*.jar found under $dataDir")
            }
        } catch (e: Exception) {
            logger.error("Failed to list data directory contents", e)
        }

        if (lifecycleJar == null) {
            logger.info("No LifeCycle-*.jar found; skipping Tool_Feedbag extraction.")
            return
        }

        val myToolbag = rc.extractAsset(Paths.get("Server/overrides/feedbagBreed.json"), lifecycleJar) ?: return

        // rc.mergePatch(Paths.get("Server/Item/Items/Tool/Feedbag/Tool_Feedbag.json"), myToolbag)

        logger.info("Extracted modified Tool_Feedbag.json content:\n$myToolbag")

        for (npcPath in tamedNPCs) {
            // logger.info("Tamed NPC JSON found in ZIP: $npcPath")
            rc.mergePatchArrayRemove(npcPath, """{"Modify":{"AttractiveItemSet":["Tool_Feedbag"]}}""")
            rc.mergePatch(npcPath, myToolbag)
        }


    }

    override fun start() { start(entityStoreRegistry, commandRegistry) }
    fun start(entityStoreRegistry: ComponentRegistryProxy<EntityStore>, commandRegistry: CommandRegistry) {
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
        entityStoreRegistry.registerSystem(AnimalsBreedRegistrySystem())
        
        // Register commands
        commandRegistry.registerCommand(AnimalsBreedCommand())

        // Register interactions (commented out for now — needs proper functional ref)
        try {
            NPCPlugin.get().registerCoreComponentType("Breed", java.util.function.Supplier { BuilderActionBreed() })
        } catch (e: Exception) {
            logger.info("Breed action already registered (Maybe plugin reloaded?)")
        }
    }
}