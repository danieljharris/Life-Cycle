package DrDan.AnimalsGrow.test

import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.server.npc.NPCPlugin
import com.hypixel.hytale.server.npc.entities.NPCEntity
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.component.RemoveReason

import DrDan.AnimalsGrow.AnimalsGrow
import DrDan.AnimalsGrow.grow_ecs.AnimalsGrowComponent

import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer

/**
 * Simple in-game test framework for AnimalsGrow plugin.
 * Outputs structured log messages that can be parsed by test.sh
 * 
 * Log format:
 *   [AG_TEST:testName:PASS]
 *   [AG_TEST:testName:FAIL:reason]
 *   [AG_TEST:START]
 *   [AG_TEST:END:passed/total]
 */
object TestRunner {
    private val tests = mutableListOf<TestCase>()
    private var testsPassed = 0
    private var testsFailed = 0
    
    init {
        // Register all tests
        // tests.add(SpawnBabyTest())
        tests.add(BabyGrowTest())
        // tests.add(BabyHasGrowthComponentTest())
        // tests.add(GrowthProgressTest())
    }
    
    fun runAllTests(world: World, store: Store<EntityStore>, playerPosition: Vector3d) {
        println("[AG_TEST:START]")
        println("Running ${tests.size} tests...")
        
        // Clean up any existing test entities first
        // cleanupTestEntities(store)

        // Run each test on the world thread and wait for all to finish
        for (test in tests) {
            Thread {
                try {
                    test.run(world, store, playerPosition)
                } catch (e: Exception) {
                    println("[AG_TEST:END:${test.name}:FAIL:Exception: ${e.message}]")
                }
            }.start()
        }

        // Schedule cleanup after 2 seconds without blocking the main thread
        // Thread {
        //     Thread.sleep(2000)
        //     world.execute { TestRunner.cleanupTestEntities(store) }
        // }.start()
    }
    
    // Find and remove entities with "Test_Bunny" DisplayNameComponent
    @JvmStatic
    fun cleanupTestEntities(store: Store<EntityStore>, name: String) {
        println("Cleaning up test entities with name: $name")
        store.forEachChunk(BiConsumer { chunk: ArchetypeChunk<EntityStore>, commandBuffer: CommandBuffer<EntityStore> ->
            for (i in 0 until chunk.size()) {
                val ref = chunk.getReferenceTo(i)
                val nameComponent: DisplayNameComponent = store.getComponent(ref, DisplayNameComponent.getComponentType()) ?: continue
                val displayName = nameComponent.displayName?.rawText ?: continue
                if (displayName == name) {
                    commandBuffer.removeEntity(ref, RemoveReason.REMOVE)
                }
            }
        })
    }
}

data class TestResult(
    val passed: Boolean,
    val reason: String = ""
)

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
}

// /**
//  * Test: Spawn a baby bunny and verify it exists
//  */
// class SpawnBabyTest : TestCase("SpawnBaby") {
//     override fun run(world: World, store: Store<EntityStore>, playerPosition: Vector3d): TestResult {
//         val spawnPos = Vector3d(playerPosition.x + 2, playerPosition.y, playerPosition.z)
        
//         // Spawn a bunny (baby)
//         val spawnResult = NPCPlugin.get().spawnNPC(store, "Bunny", null, spawnPos, Vector3f(0f, 0f, 0f))
        
//         if (spawnResult == null) {
//             return TestResult(false, "spawnNPC returned null")
//         }
        
//         val ref = spawnResult.first()
//         if (ref == null) {
//             return TestResult(false, "spawnNPC ref is null")
//         }

//         val message = Message.raw("Test_Bunny")
//         store.replaceComponent(ref, DisplayNameComponent.getComponentType(), DisplayNameComponent(message))
        
//         // Verify the NPC exists
//         val npcComponentType = NPCEntity.getComponentType() as? ComponentType<EntityStore, NPCEntity>
//             ?: return TestResult(false, "Could not get NPCEntity component type")
        
//         val npc = store.getComponent(ref, npcComponentType)
//         if (npc == null) {
//             return TestResult(false, "Spawned entity has no NPCEntity component")
//         }
        
//         if (npc.roleName != "Bunny") {
//             return TestResult(false, "Expected roleName 'Bunny' but got '${npc.roleName}'")
//         }

//         Thread {
//             Thread.sleep(2000)
//             world.execute { TestRunner.cleanupTestEntities(store) }
//         }.start()
        
//         return TestResult(true)
//     }
// }

class BabyGrowTest : TestCase("BabyGrowTest") {
    override fun run(world: World, store: Store<EntityStore>, playerPosition: Vector3d) {
        start()

        val testNPCName = "Test_BabyGrowTest"

        val spawnPos = Vector3d(playerPosition.x + 2, playerPosition.y, playerPosition.z)
        
        // Spawn a bunny
        world.execute {
            val spawnResult = NPCPlugin.get().spawnNPC(store, "Bunny", null, spawnPos, Vector3f(0f, 0f, 0f))
                ?: return@execute
                // ?: return@run TestResult(false, "spawnNPC returned null")
            
            val ref = spawnResult.first()
                ?: return@execute
                // ?: return@run TestResult(false, "spawnNPC ref is null")

            val message = Message.raw(testNPCName)
            store.replaceComponent(ref, DisplayNameComponent.getComponentType(), DisplayNameComponent(message))
        }


        println("[AG_TEST:COMMAND:time midday]")
        println("[AG_TEST:COMMAND:time midnight]")
        println("[AG_TEST:COMMAND:time midday]")

        Thread.sleep(5000) // sleep for 5 seconds to day night cycle to advance and growth to occur
        println("Checking if baby has grown...")

        val resultLatch = CountDownLatch(1)
        val found = arrayOf(false) // Use array to allow mutation within inner class
        world.execute {
            store.forEachChunk(BiConsumer { chunk: ArchetypeChunk<EntityStore>, commandBuffer: CommandBuffer<EntityStore> ->
                if (found[0]) return@BiConsumer  // Early exit if already found
                for (i in 0 until chunk.size()) {
                    if (found[0]) return@BiConsumer  // Early exit if already found

                    val entityRef = chunk.getReferenceTo(i)
                    val name: DisplayNameComponent = store.getComponent(entityRef, DisplayNameComponent.getComponentType()) ?: continue
                    val displayName = name.displayName?.rawText ?: continue

                    val npcComponentType = NPCEntity.getComponentType() as? ComponentType<EntityStore, NPCEntity> ?: continue
                    val npcEntity = store.getComponent(entityRef, npcComponentType) ?: continue
                    val npcName: String = npcEntity.roleName ?: continue

                    if (displayName == testNPCName && npcName == "Rabbit") {
                        println("Found $testNPCName as adult Rabbit!")
                        found[0] = true
                        return@BiConsumer
                    }
                }
            })
            resultLatch.countDown()
        }

        // Wait for the world thread check to complete (timeout to avoid hanging)
        try { resultLatch.await(5, TimeUnit.SECONDS) } catch (e: InterruptedException) {}

        if (found[0]) {
            result(true)
        } else {
            println("Did not find grown adult Rabbit for $testNPCName")
            result(false, "Baby did not grow into adult within expected time")
        }

        world.execute { TestRunner.cleanupTestEntities(store, testNPCName) }
    }
}

// /**
//  * Test: Verify spawned baby has AnimalsGrowComponent attached
//  */
// class BabyHasGrowthComponentTest : TestCase("BabyHasGrowthComponent") {
//     override fun run(world: World, store: Store<EntityStore>, playerPosition: Vector3d): TestResult {
//         val spawnPos = Vector3d(playerPosition.x + 4, playerPosition.y, playerPosition.z)
        
//         // Spawn a bunny
//         val spawnResult = NPCPlugin.get().spawnNPC(store, "Bunny", null, spawnPos, Vector3f(0f, 0f, 0f))
//             ?: return TestResult(false, "spawnNPC returned null")
        
//         val ref = spawnResult.first()
//             ?: return TestResult(false, "spawnNPC ref is null")
        
//         // Check for AnimalsGrowComponent
//         val growComponentType = AnimalsGrow.getComponentType()
        
//         val growComponent = store.getComponent(ref, growComponentType)
//         if (growComponent == null) {
//             return TestResult(false, "Baby does not have AnimalsGrowComponent - check if 'Bunny' is in growth config")
//         }
        
//         return TestResult(true)
//     }
// }

// /**
//  * Test: Verify growth component has valid data
//  */
// class GrowthProgressTest : TestCase("GrowthComponentData") {
//     override fun run(world: World, store: Store<EntityStore>, playerPosition: Vector3d): TestResult {
//         val spawnPos = Vector3d(playerPosition.x + 6, playerPosition.y, playerPosition.z)
        
//         // Spawn a bunny
//         val spawnResult = NPCPlugin.get().spawnNPC(store, "Bunny", null, spawnPos, Vector3f(0f, 0f, 0f))
//             ?: return TestResult(false, "spawnNPC returned null")
        
//         val ref = spawnResult.first()
//             ?: return TestResult(false, "spawnNPC ref is null")
        
//         val growComponentType = AnimalsGrow.getComponentType()
        
//         val growComponent = store.getComponent(ref, growComponentType)
//             ?: return TestResult(false, "No AnimalsGrowComponent on baby")
        
//         // Verify component has valid data
//         if (growComponent.growthDurationSeconds <= 0) {
//             return TestResult(false, "Growth duration is invalid: ${growComponent.growthDurationSeconds}")
//         }
        
//         // Verify spawn time was set (not EPOCH)
//         if (growComponent.spawnTime == Instant.EPOCH) {
//             return TestResult(false, "Spawn time was not set (still EPOCH)")
//         }
        
//         return TestResult(true)
//     }
// }
