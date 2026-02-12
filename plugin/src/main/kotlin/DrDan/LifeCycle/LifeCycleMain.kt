package DrDan.LifeCycle

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.CompletableFuture

import DrDan.AnimalsGrow.*
import DrDan.AnimalsGrow.command.*
import DrDan.AnimalsGrow.config.GrowthEntry
import DrDan.AnimalsGrow.event.AnimalsGrowEvent
import DrDan.AnimalsGrow.grow_ecs.*

import DrDan.AnimalsBreed.*
import DrDan.AnimalsBreed.command.*
import DrDan.AnimalsBreed.config.*
import DrDan.AnimalsBreed.event.AnimalsBreedEvent
import DrDan.AnimalsBreed.breed_ecs.*

class LifeCycleMain(init: JavaPluginInit) : JavaPlugin(init) {
    private val logger = LoggerFactory.getLogger(LifeCycleMain::class.java)

    val AnimalsGrowPlugin: AnimalsGrow = AnimalsGrow(init)
    // val AnimalsBreedPlugin: AnimalsBreed = AnimalsBreed(init)

    override fun setup() {
        logger.info("Registering LifeCycleMain!")
        AnimalsGrowPlugin.callSetup()
        // AnimalsBreedPlugin.setup()
    }

    override fun start() {
        logger.info("Starting LifeCycleMain!")
        AnimalsGrowPlugin.start(entityStoreRegistry, commandRegistry)
        // AnimalsBreedPlugin.start()
    }
}