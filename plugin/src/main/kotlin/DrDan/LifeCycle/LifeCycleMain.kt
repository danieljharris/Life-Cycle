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

class LifeCycleMain(private val pluginInit: JavaPluginInit) : JavaPlugin(pluginInit) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private data class PluginHandle(
        val name: String,
        val instance: Any,
        val setup: Method,
        val start: Method,
        val manualStart: (JavaPlugin, Any) -> Unit
    )

    private val plugins: List<PluginHandle> = listOfNotNull(

        // AnimalsGrow
        load(
            "AnimalsGrow",
            "DrDan.AnimalsGrow.AnimalsGrow"
        ) { plugin, instance ->
            val growthConfig = readConfig<List<GrowthEntry>>(instance, "growsUpInto")
                ?: return@load
            
            manualStartPlugin(
                plugin = plugin,
                instance = instance,
                configFieldName = "growsUpInto",
                initializeAction = { 
                    @Suppress("UNCHECKED_CAST")
                    AnimalsGrowAction.initialize(it as List<GrowthEntry>) 
                },
                components = listOf(AnimalsGrowComponent()),
                events = listOf(AnimalsGrowEvent(growthConfig)),
                systems = listOf(AnimalsGrowSystem()),
                commands = listOf(AnimalsGrowCommand(), AnimalsGrowTestCommand())
            )
        },

        // AnimalsBreed
        load(
            "AnimalsBreed",
            "DrDan.AnimalsBreed.AnimalsBreed"
        ) { plugin, instance ->
            val breedConfig = readConfig<AnimalsBreedConfig>(instance)
                ?: return@load
            
            manualStartPlugin(
                plugin = plugin,
                instance = instance,
                configFieldName = null,
                initializeAction = { 
                    @Suppress("UNCHECKED_CAST")
                    AnimalsBreedAction.initialize(it as AnimalsBreedConfig) 
                },
                components = listOf(AnimalsBreedComponent()),
                events = listOf(AnimalsBreedEvent(breedConfig.breedGroup)),
                systems = listOf(AnimalsBreedSystem()),
                commands = listOf(AnimalsBreedCommand())
            )
        }
    )

    private fun load(
        name: String,
        className: String,
        manualStart: (JavaPlugin, Any) -> Unit
    ): PluginHandle? =
        runCatching {
            val cls = Class.forName(className)
            val instance = cls
                .getConstructor(JavaPluginInit::class.java)
                .newInstance(pluginInit)

            PluginHandle(
                name = name,
                instance = instance,
                setup = cls.getDeclaredMethod("setup").apply { isAccessible = true },
                start = cls.getDeclaredMethod("start").apply { isAccessible = true },
                manualStart = manualStart
            )
        }.onSuccess {
            logger.info("$name loaded")
        }.onFailure {
            if (it !is ClassNotFoundException)
                logger.warn("Failed loading $name", it)
            else
                logger.info("$name not present — skipping")
        }.getOrNull()

    override fun setup() {
        logger.info("LifeCycle wrapper setup")
    }

    override fun start() {
        logger.info("LifeCycle wrapper start")
        plugins.forEach(::startPlugin)
    }

    private fun startPlugin(p: PluginHandle) {
        repeat(10) { attempt ->
            try {
                loadConfigs(p.instance)
                p.setup.invoke(p.instance)
                p.start.invoke(p.instance)

                logger.info("${p.name} started")
                return
            } catch (e: InvocationTargetException) {
                when (val cause = e.cause) {
                    is IllegalStateException -> handleIllegalState(p, cause, attempt)
                    else -> return fail(p, e)
                }
            } catch (e: Exception) {
                return fail(p, e)
            }
        }
        logger.warn("Giving up on ${p.name}")
    }

    private fun handleIllegalState(
        p: PluginHandle,
        cause: IllegalStateException,
        attempt: Int
    ) {
        val msg = cause.message.orEmpty()

        when {
            "Config is not loaded" in msg -> {
                logger.warn("Config not ready for ${p.name} (${attempt + 1}/10)")
                Thread.sleep(1000)
            }

            "plugin null is not enabled" in msg -> {
                logger.warn("Manual startup for ${p.name}")
                p.manualStart(this, p.instance)
                throw Stop
            }

            else -> throw cause
        }
    }

    private fun fail(p: PluginHandle, e: Exception) {
        logger.warn("Failed starting ${p.name}", e)
    }

    private object Stop : RuntimeException()

    private fun manualStartPlugin(
        plugin: JavaPlugin,
        instance: Any,
        configFieldName: String? = null,
        initializeAction: ((Any) -> Unit)? = null,
        components: List<Any>? = null,
        events: List<Any>? = null,
        systems: List<Any>? = null,
        commands: List<Any>? = null
    ) {
        val config = if (configFieldName != null) {
            readConfig<Any>(instance, configFieldName) ?: return
        } else {
            readConfig<Any>(instance) ?: return
        }

        initializeAction?.invoke(config)

        components?.forEach { component ->
            runCatching {
                val method = plugin.entityStoreRegistry.javaClass.getMethod(
                    "registerComponent",
                    Class::class.java,
                    java.util.function.Supplier::class.java
                )
                method.invoke(
                    plugin.entityStoreRegistry,
                    component.javaClass,
                    java.util.function.Supplier { component }
                )
            }
        }
        
        events?.forEach { event ->
            runCatching {
                @Suppress("UNCHECKED_CAST")
                plugin.entityStoreRegistry.registerSystem(
                    event as com.hypixel.hytale.component.system.ISystem<com.hypixel.hytale.server.core.universe.world.storage.EntityStore>
                )
            }
        }
        
        systems?.forEach { system ->
            runCatching {
                @Suppress("UNCHECKED_CAST")
                plugin.entityStoreRegistry.registerSystem(
                    system as com.hypixel.hytale.component.system.ISystem<com.hypixel.hytale.server.core.universe.world.storage.EntityStore>
                )
            }
        }

        commands?.forEach { command ->
            runCatching { 
                @Suppress("UNCHECKED_CAST")
                plugin.commandRegistry.registerCommand(
                    command as com.hypixel.hytale.server.core.command.system.AbstractCommand
                )
            }
        }

        logger.info("Manual ${instance.javaClass.simpleName} startup complete")
    }

    private fun loadConfigs(instance: Any) =
        instance.javaClass.declaredFields
            .filter { it.type.name.endsWith(".util.Config") }
            .forEach { field ->
                runCatching {
                    field.isAccessible = true
                    val cfg = field.get(instance) ?: return@runCatching
                    val future = cfg.javaClass
                        .getMethod("load")
                        .invoke(cfg) as? CompletableFuture<*>
                    future?.join()
                }
            }

    private inline fun <reified T> readConfig(
        instance: Any,
        fieldName: String? = null
    ): T? =
        runCatching {
            val cfgField = instance.javaClass
                .getDeclaredField("config")
                .apply { isAccessible = true }

            val cfg = cfgField.get(instance)
            val cfgObj = cfg.javaClass.getMethod("get").invoke(cfg)

            if (fieldName == null) cfgObj as T
            else {
                cfgObj.javaClass
                    .getDeclaredField(fieldName)
                    .apply { isAccessible = true }
                    .get(cfgObj) as T
            }
        }.onFailure {
            logger.warn("Failed reading config for ${instance.javaClass.simpleName}", it)
        }.getOrNull()
}