package DrDan.AnimalsBreed

import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.util.Config
import com.hypixel.hytale.server.core.HytaleServer
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.component.ComponentRegistryProxy
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.command.system.CommandRegistry
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction
import com.hypixel.hytale.assetstore.AssetPack
import com.hypixel.hytale.common.util.FormatUtil
import com.hypixel.hytale.logger.sentry.SkipSentryException
import com.hypixel.hytale.server.core.asset.AssetModule
import com.hypixel.hytale.server.core.asset.LoadAssetEvent

import java.util.logging.Level
import java.nio.file.FileSystem
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.FileSystems

import DrDan.AnimalsBreed.registry.AnimalsBreedRegistry
import DrDan.AnimalsBreed.registry.AnimalsBreedRegistrySystem
import DrDan.AnimalsBreed.command.AnimalsBreedCommand
import DrDan.AnimalsBreed.config.AnimalsBreedConfig
import DrDan.AnimalsBreed.event.AnimalsBreedEvent
import DrDan.AnimalsBreed.breed_ecs.AnimalsBreedComponent
import DrDan.AnimalsBreed.breed_ecs.AnimalsBreedSystem
import DrDan.AnimalsBreed.resource_creator.ResourceCreator
// import DrDan.AnimalsBreed.interaction.ExampleInteraction

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
        
        for (npcPath in tamedNPCs) {
            // logger.info("Tamed NPC JSON found in ZIP: $npcPath")
            rc.mergePatchArrayMove(npcPath, """{"Modify":{"AttractiveItemSet":["Tool_Feedbag"]}}""")
        }


        // val assetPacks: List<AssetPack> = AssetModule.get().getAssetPacks()

        // logger.info("Asset found at 1 ${assetPacks[2].getRoot()}")

        // val rc = ResourceCreator()
        // rc.mergePatch(Path.of("Server/NPC/Roles/Creature/Livestock/Tamed/Tamed_Bison.json"), "")

        // TODO: See if this can be moved to start()
        // this.getCodecRegistry(Interaction.CODEC).register("Example", ExampleInteraction::class.java, ExampleInteraction.CODEC)
        // NPCPlugin.get().registerCoreComponentType("Breed", com.hypixel.hytale.builtin.adventure.npcshop.npc.builders.BuilderActionOpenShop::new)
    }

    override fun start() { start(entityStoreRegistry, commandRegistry) }
    fun start(entityStoreRegistry: ComponentRegistryProxy<EntityStore>, commandRegistry: CommandRegistry) {
        logger.info("Starting $PLUGIN_NAME!")

        // val zipFs = FileSystems.newFileSystem(assetPath, emptyMap<String, Any>())
        // try {
        //     val target: Path = zipFs.getPath(jsonFileToGet.toString())
        //     if (Files.exists(target)) {
        //         try {
        //             val bytes = Files.readAllBytes(target)
        //             val content = String(bytes)
        //             logger.info("Contents of $jsonFileToGet:\n$content")
        //         } catch (e: Exception) {
        //             logger.warn("Failed reading $jsonFileToGet from ZIP", e)
        //         }
        //     } else {
        //         logger.warn("JSON file not found in asset ZIP: $jsonFileToGet")
        //     }
        // } finally {
        //     zipFs.close()
        // }


        // logger.info("Asset found 1: getName = ${assetPacks[1].getName()}")
        // logger.info("Asset found 1: getPackLocation = ${assetPacks[1].getPackLocation()}")
        // logger.info("Asset found 1: getRoot = ${assetPacks[1].getRoot()}")

        // this.getEventRegistry().register(128.toShort(), LoadAssetEvent::class.java) { event ->
        //     this.getLogger().at(Level.INFO).log("Loading Hytalor Patch assets phase...")
        //     val start = System.nanoTime()
        //     val assetPacks: List<AssetPack> = AssetModule.get().getAssetPacks()

        //     for (assetPack in assetPacks) {
        //         try {
        //             val pmClass = Class.forName("com.hypixel.hytale.server.core.asset.PatchManager")
        //             val getMethod = pmClass.getMethod("get")
        //             val pm = getMethod.invoke(null)
        //             val loadMethod = pmClass.getMethod("loadPatchAssets", assetPack.javaClass)
        //             loadMethod.invoke(pm, assetPack)
        //         } catch (e: Exception) {
        //             logger.warn("Failed to load patch assets reflectively", e)
        //         }
        //     }

        //     this.getLogger()
        //         .at(Level.INFO)
        //         .log(
        //             "Loading Hytalor Patch assets phase completed! Boot time %s, Took %s",
        //             FormatUtil.nanosToString(System.nanoTime() - event.getBootStart()),
        //             FormatUtil.nanosToString(System.nanoTime() - start)
        //         )
        // }
        
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
    }
}