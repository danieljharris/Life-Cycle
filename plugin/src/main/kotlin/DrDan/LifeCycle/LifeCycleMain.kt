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
        load("AnimalsGrow", "DrDan.AnimalsGrow.AnimalsGrow"),
        load("AnimalsBreed", "DrDan.AnimalsBreed.AnimalsBreed")
    )

    private fun load(
        name: String,
        className: String
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
                manualStart = { plugin, inst -> autoDiscoverAndStart(plugin, inst, name) }
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
                    is IllegalStateException -> {
                        try {
                            handleIllegalState(p, cause, attempt)
                        } catch (stop: Stop) {
                            // Manual startup completed successfully
                            logger.info("${p.name} started via manual startup")
                            return
                        }
                    }
                    else -> return fail(p, e)
                }
            } catch (e: Stop) {
                // Manual startup completed successfully
                logger.info("${p.name} started via manual startup")
                return
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

    private fun autoDiscoverAndStart(plugin: JavaPlugin, instance: Any, pluginName: String) {
        logger.info("Auto-discovering classes for $pluginName...")
        
        // Read the start() method to discover what needs to be registered
        val startMethod = instance.javaClass.getDeclaredMethod("start").apply { isAccessible = true }
        val startMethodCode = startMethod.toString()
        
        // Find the package of the plugin
        val packageName = instance.javaClass.`package`.name
        logger.info("  Package: $packageName")
        
        // Auto-discover components (classes ending with "Component")
        val components = discoverClasses(packageName, "Component")
            .mapNotNull { instantiateIfPossible(it) }
        logger.info("  Found ${components.size} components: ${components.map { it.javaClass.simpleName }}")
        
        // Auto-discover systems (classes ending with "System")
        val systems = discoverClasses(packageName, "System")
            .mapNotNull { instantiateIfPossible(it) }
        logger.info("  Found ${systems.size} systems: ${systems.map { it.javaClass.simpleName }}")
        
        // Auto-discover events (classes ending with "Event")
        // Events need config, so we need to read it first
        val config = readConfig<Any>(instance) ?: return
        val events = discoverClasses(packageName, "Event")
            .mapNotNull { instantiateWithConfig(it, config) }
        logger.info("  Found ${events.size} events: ${events.map { it.javaClass.simpleName }}")
        
        // Auto-discover commands (classes ending with "Command")
        val commandClasses = discoverClasses(packageName, "Command")
        logger.info("  Discovered ${commandClasses.size} command classes: ${commandClasses.map { it.simpleName }}")
        val commands = commandClasses.mapNotNull { instantiateIfPossible(it) }
        logger.info("  Instantiated ${commands.size} commands: ${commands.map { it.javaClass.simpleName }}")
        
        // Initialize action if there's an Action class
        discoverClasses(packageName, "Action")
            .firstOrNull()
            ?.let { actionClass ->
                runCatching {
                    val initMethod = actionClass.getDeclaredMethod("initialize", Any::class.java)
                        .apply { isAccessible = true }
                    initMethod.invoke(null, config)
                    logger.info("  Initialized Action: ${actionClass.simpleName}")
                }
            }
        
        manualStartPlugin(
            plugin = plugin,
            instance = instance,
            configFieldName = null,
            initializeAction = null,
            components = components,
            events = events,
            systems = systems,
            commands = commands
        )
        
        logger.info("Auto-discovered $pluginName: ${components.size} components, ${events.size} events, ${systems.size} systems, ${commands.size} commands")
    }
    
    private fun discoverClasses(packageName: String, suffix: String): List<Class<*>> {
        val classes = mutableListOf<Class<*>>()
        
        // Try common subpackages
        val subpackages = listOf("", ".command", ".event", ".${suffix.lowercase()}_ecs", ".config", ".grow_ecs", ".breed_ecs")
        
        for (subpackage in subpackages) {
            val fullPackage = "$packageName$subpackage"
            
            // Try multiple naming patterns
            val pluginBaseName = packageName.substringAfterLast('.')
            val potentialNames = listOf(
                "$pluginBaseName$suffix",
                "${pluginBaseName}Test$suffix",
                "$pluginBaseName${suffix}Test"
            )
            
            potentialNames.forEach { className ->
                runCatching {
                    val fullClassName = "$fullPackage.$className"
                    logger.debug("  Trying to load: $fullClassName")
                    val cls = Class.forName(fullClassName)
                    if (cls.simpleName.endsWith(suffix)) {
                        classes.add(cls)
                        logger.debug("  ✓ Found: ${cls.simpleName}")
                    }
                }.onFailure {}
            }
        }
        
        return classes
    }
    
    private fun instantiateIfPossible(clazz: Class<*>): Any? =
        runCatching {
            clazz.getDeclaredConstructor().newInstance()
        }.getOrNull()
    
    private fun instantiateWithConfig(clazz: Class<*>, config: Any): Any? =
        runCatching {
            clazz.constructors.firstOrNull { it.parameterCount == 1 }
                ?.newInstance(config)
        }.getOrElse {
            instantiateIfPossible(clazz)
        }

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
            logger.info("Attempting to register command: ${command.javaClass.simpleName}")
            runCatching { 
                @Suppress("UNCHECKED_CAST")
                val abstractCommand = command as com.hypixel.hytale.server.core.command.system.AbstractCommand
                logger.info("  - Cast successful to AbstractCommand")
                plugin.commandRegistry.registerCommand(abstractCommand)
                logger.info("  - Command registered successfully: ${command.javaClass.simpleName}")
            }.onFailure { e ->
                logger.warn("  - Failed to register command ${command.javaClass.simpleName}", e)
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