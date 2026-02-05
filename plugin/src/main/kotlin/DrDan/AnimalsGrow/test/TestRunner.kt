package DrDan.AnimalsGrow.test

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.npc.NPCPlugin
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.component.RemoveReason
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.server.npc.entities.NPCEntity
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes


import DrDan.AnimalsGrow.AnimalsGrow
import DrDan.AnimalsGrow.grow_ecs.AnimalsGrowComponent

import java.time.Instant
import java.util.function.BiConsumer
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch

/**
 * Simple in-game test framework for AnimalsGrow plugin.
 * Outputs structured log messages that can be parsed by test.sh
 * 
 * Log format:
 *  [AG_TEST:START]
 *  [AG_TEST:START:${name}]
 *  [AG_TEST:END:${name}:PASS]
 *  [AG_TEST:END:${name}:FAIL:${reason}]
 *  [AG_TEST:END:${name}:FAIL:Exception:${reason}]"
 */
object TestRunner {
    private val tests = mutableListOf<TestCase>()
    
    // Register all tests
    init {
        tests.add(BabyGrowTest())
        tests.add(BabyHasGrowthComponentTest())
        tests.add(KeepHealthOnGrowTest())
    }
    
    fun runAllTests(world: World, store: Store<EntityStore>, playerPosition: Vector3d) {
        println("[AG_TEST:START:${tests.size}]")
        println("Running ${tests.size} tests...")
        
        Thread {
            for (test in tests) {
                try {
                    test.run(world, store, playerPosition)
                } catch (e: Exception) {
                    println("[AG_TEST:END:${test.name}:FAIL:Exception:${e.message}]")
                }
            }
        }.start()
    }
}

abstract class TestCase(val name: String) {
    abstract fun run(world: World, store: Store<EntityStore>, playerPosition: Vector3d)
    fun start() {
        println("[AG_TEST:START:${name}]")
    }
    fun result(passed: Boolean, reason: String = "") {
        if (passed) {
            println("[AG_TEST:END:${name}:PASS]")
        } else {
            println("[AG_TEST:END:${name}:FAIL:$reason]")
        }
    }
    fun spawn(world: World, store: Store<EntityStore>, spawnPos: Vector3d, testNPCName: String): Ref<EntityStore> {
        var refReturn: Array<Ref<EntityStore>> = arrayOf()
        val checkLatchSpawn = CountDownLatch(1)
        world.execute {
            val spawnResult = NPCPlugin.get().spawnNPC(store, "Bunny", null, spawnPos, Vector3f(0f, 0f, 0f))?: return@execute
            val ref = spawnResult.first()?: return@execute

            store.addComponent(ref, Nameplate.getComponentType(), Nameplate(testNPCName))

            refReturn = arrayOf(ref)
            checkLatchSpawn.countDown()
        }
        try { checkLatchSpawn.await(5, TimeUnit.SECONDS) } catch (e: InterruptedException) {}
        return refReturn[0]
    }
    fun getSpawned(world: World, store: Store<EntityStore>, testNPCName: String): Ref<EntityStore>? {
        val resultLatch = CountDownLatch(1)
        val foundRef = arrayOf<Ref<EntityStore>?>(null)
        world.execute {
            store.forEachChunk(BiConsumer { chunk: ArchetypeChunk<EntityStore>, commandBuffer: CommandBuffer<EntityStore> ->
                if (foundRef[0] != null) return@BiConsumer  // Early exit if already found
                for (i in 0 until chunk.size()) {
                    if (foundRef[0] != null) return@BiConsumer  // Early exit if already found

                    val ref = chunk.getReferenceTo(i)
                    val namePlate: Nameplate = store.getComponent(ref, Nameplate.getComponentType()) ?: continue

                    if (namePlate.getText() == testNPCName) {
                        foundRef[0] = ref
                        return@BiConsumer
                    }
                }
            })
            resultLatch.countDown()
        }
        try { resultLatch.await(5, TimeUnit.SECONDS) } catch (e: InterruptedException) {}
        return foundRef[0]
    }
    fun removeEntity(world: World, store: Store<EntityStore>, testNPCName: String) {
        getSpawned(world, store, testNPCName)?.let { ref ->
            world.execute {
                store.removeEntity(ref, RemoveReason.REMOVE)
            }
        }
    }
    fun removeEntity(world: World, store: Store<EntityStore>, ref: Ref<EntityStore>) {
        world.execute {
            store.removeEntity(ref, RemoveReason.REMOVE)
        }
    }
}

class BabyGrowTest : TestCase("BabyGrowTest") {
    override fun run(world: World, store: Store<EntityStore>, playerPosition: Vector3d) {
        start()
        val testNPCName = "Test_BabyGrowTest"
        val spawnPos = Vector3d(playerPosition.x + 2, playerPosition.y, playerPosition.z)
        val ref = spawn(world, store, spawnPos, testNPCName)

        println("[AG_TEST:COMMAND:time midday]")
        println("[AG_TEST:COMMAND:time midnight]")
        println("[AG_TEST:COMMAND:time midday]")
        Thread.sleep(5000) // sleep for 5 seconds to day night cycle to advance and growth to occur

        val spawnedRef = getSpawned(world, store, testNPCName)
        if (spawnedRef == null) {
            println("Reference to NPC changed after growth")
            result(false, "Reference to NPC changed after growth")
            removeEntity(world, store, testNPCName)
            return
        }

        val resultLatch = CountDownLatch(1)
        val found = arrayOf(false) // Use array to allow mutation within inner class
        world.execute {
            val npcComponentType = NPCEntity.getComponentType() as? ComponentType<EntityStore, NPCEntity> ?: return@execute
            val npcEntity = store.getComponent(spawnedRef, npcComponentType) ?: return@execute
            val npcName: String = npcEntity.roleName ?: return@execute

            if (npcName == "Rabbit") {
                println("Found $testNPCName as adult Rabbit!")
                found[0] = true
                return@execute
            }

            resultLatch.countDown()
        }
        try { resultLatch.await(5, TimeUnit.SECONDS) } catch (e: InterruptedException) {}

        if (found[0]) {
            result(true)
        } else {
            println("Did not find grown adult Rabbit for $testNPCName")
            result(false, "Baby did not grow into adult within expected time")
        }

        removeEntity(world, store, spawnedRef)
    }
}

class BabyHasGrowthComponentTest : TestCase("BabyHasGrowthComponent") {
    override fun run(world: World, store: Store<EntityStore>, playerPosition: Vector3d) {
        start()
        val testNPCName = "Test_BabyHasGrowthComponent"
        val spawnPos = Vector3d(playerPosition.x + 2, playerPosition.y, playerPosition.z)
        val ref = spawn(world, store, spawnPos, testNPCName)

        Thread.sleep(1000) // Wait for the spawn to process

        val hasGrowthComponent = arrayOf(false)
        val checkLatch = CountDownLatch(1)
        world.execute {
            val growthComponent = store.getComponent(ref, AnimalsGrow.getComponentType())
            if (growthComponent != null) {
                hasGrowthComponent[0] = true
            }
            checkLatch.countDown()
        }
        try { checkLatch.await(5, TimeUnit.SECONDS) } catch (e: InterruptedException) {}

        if (hasGrowthComponent[0]) {
            result(true)
        } else {
            println("Spawned baby did not have AnimalsGrowComponent")
            result(false, "Spawned baby did not have AnimalsGrowComponent")
        }

        removeEntity(world, store, testNPCName)
    }
}

class KeepHealthOnGrowTest : TestCase("KeepHealthOnGrowTest") {
    override fun run(world: World, store: Store<EntityStore>, playerPosition: Vector3d) {
        start()
        val testNPCName = "Test_KeepHealthOnGrowTest"
        val spawnPos = Vector3d(playerPosition.x + 2, playerPosition.y, playerPosition.z)
        val ref = spawn(world, store, spawnPos, testNPCName)

        val healthBeforeGrow = arrayOf(1.0f)
        val checkLatchDamage = CountDownLatch(1)
        world.execute {
            // Damage the bunny to 50% health
            val statMapType = EntityStatMap.getComponentType() as? ComponentType<EntityStore, EntityStatMap>
            if (statMapType != null) {
                val statMap = store.getComponent(ref, statMapType)
                if (statMap != null) {
                    val healthMax = statMap.maximizeStatValue(DefaultEntityStatTypes.getHealth())
                    val halfHealth = healthMax * 0.5f
                    statMap.setStatValue(DefaultEntityStatTypes.getHealth(), halfHealth)
                    healthBeforeGrow[0] = statMap.get(DefaultEntityStatTypes.getHealth())?.asPercentage() ?: 1.0f
                    println("Set bunny health to ${healthBeforeGrow[0]}% before growth")
                }
            }
            checkLatchDamage.countDown()
        }
        try { checkLatchDamage.await(5, TimeUnit.SECONDS) } catch (e: InterruptedException) {}

        println("[AG_TEST:COMMAND:time midday]")
        println("[AG_TEST:COMMAND:time midnight]")
        println("[AG_TEST:COMMAND:time midday]")
        Thread.sleep(5000) // sleep for 5 seconds to day night cycle to advance and growth to occur

        val spawnedRef = getSpawned(world, store, testNPCName)
        if (spawnedRef == null) {
            println("Reference to NPC changed after growth")
            result(false, "Reference to NPC changed after growth")
            removeEntity(world, store, testNPCName)
            return
        }

        val healthKept = arrayOf(false)
        val checkLatch = CountDownLatch(1)
        world.execute {
            val npcComponentType = NPCEntity.getComponentType() as? ComponentType<EntityStore, NPCEntity> ?: return@execute
            val npcEntity = store.getComponent(spawnedRef, npcComponentType) ?: return@execute
            val npcName: String = npcEntity.roleName ?: return@execute

            if (npcName == "Rabbit") {
                val statMapType = EntityStatMap.getComponentType() as? ComponentType<EntityStore, EntityStatMap>
                if (statMapType != null) {
                    val statMap = store.getComponent(spawnedRef, statMapType)
                    if (statMap != null) {
                        val healthValue = statMap.get(DefaultEntityStatTypes.getHealth())?.asPercentage() ?: 1.0f
                        println("Health after growth: ${healthValue}%")
                        healthKept[0] = healthValue == healthBeforeGrow[0]
                    }
                }
            }
            checkLatch.countDown()
        }
        try { checkLatch.await(5, TimeUnit.SECONDS) } catch (e: InterruptedException) {}

        if (healthKept[0]) {
            result(true)
        } else {
            println("Grown adult did not keep health percentage from baby")
            result(false, "Grown adult did not keep health percentage from baby")
        }

        removeEntity(world, store, spawnedRef)
    }
}

