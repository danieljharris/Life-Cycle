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
import DrDan.AnimalsGrow.AnimalsGrow
import DrDan.AnimalsGrow.grow_ecs.AnimalsGrowComponent
import java.time.Instant

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
        tests.add(SpawnBabyTest())
        tests.add(BabyHasGrowthComponentTest())
        tests.add(GrowthProgressTest())
    }
    
    fun runAllTests(world: World, store: Store<EntityStore>, playerPosition: Vector3d) {
        testsPassed = 0
        testsFailed = 0
        
        println("[AG_TEST:START]")
        println("Running ${tests.size} tests...")
        
        // Clean up any existing test entities first
        cleanupTestEntities(store)
        
        // Run each test
        for (test in tests) {
            try {
                val result = test.run(world, store, playerPosition)
                if (result.passed) {
                    testsPassed++
                    println("[AG_TEST:${test.name}:PASS]")
                } else {
                    testsFailed++
                    println("[AG_TEST:${test.name}:FAIL:${result.reason}]")
                }
            } catch (e: Exception) {
                testsFailed++
                println("[AG_TEST:${test.name}:FAIL:Exception: ${e.message}]")
            }
        }
        
        println("[AG_TEST:END:$testsPassed/${tests.size}]")
        
        // Schedule cleanup after tests
        world.execute {
            cleanupTestEntities(store)
        }
    }
    
    private fun cleanupTestEntities(store: Store<EntityStore>) {
        // Find and remove entities with "Test_Bunny" DisplayNameComponent
        val displayNameType = DisplayNameComponent.getComponentType() as? ComponentType<EntityStore, DisplayNameComponent> ?: return
        store.forEachChunk(java.util.function.BiConsumer { chunk: ArchetypeChunk<EntityStore>, commandBuffer: CommandBuffer<EntityStore> ->
            for (i in 0 until chunk.size()) {
                val ref = chunk.getReferenceTo(i)
                val displayNameComponent = store.getComponent(ref, displayNameType)
                if (displayNameComponent != null) {
                    val displayName = displayNameComponent.displayName?.rawText ?: ""
                    if (displayName == "Test_Bunny") {
                        commandBuffer.removeEntity(ref)
                    }
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
    abstract fun run(world: World, store: Store<EntityStore>, playerPosition: Vector3d): TestResult
}

/**
 * Test: Spawn a baby bunny and verify it exists
 */
class SpawnBabyTest : TestCase("SpawnBaby") {
    override fun run(world: World, store: Store<EntityStore>, playerPosition: Vector3d): TestResult {
        val spawnPos = Vector3d(playerPosition.x + 2, playerPosition.y, playerPosition.z)
        
        // Spawn a bunny (baby)
        val spawnResult = NPCPlugin.get().spawnNPC(store, "Bunny", null, spawnPos, Vector3f(0f, 0f, 0f))
        
        if (spawnResult == null) {
            return TestResult(false, "spawnNPC returned null")
        }
        
        val ref = spawnResult.first()
        if (ref == null) {
            return TestResult(false, "spawnNPC ref is null")
        }

        // Set name to Test_ for identification
        val displayNameType = DisplayNameComponent.getComponentType() as? ComponentType<EntityStore, DisplayNameComponent>
        if (displayNameType != null) {
                val message = Message.raw("Test_Bunny")
                store.replaceComponent(ref, displayNameType, DisplayNameComponent(message))
        }
        
        // Verify the NPC exists
        val npcComponentType = NPCEntity.getComponentType() as? ComponentType<EntityStore, NPCEntity>
            ?: return TestResult(false, "Could not get NPCEntity component type")
        
        val npc = store.getComponent(ref, npcComponentType)
        if (npc == null) {
            return TestResult(false, "Spawned entity has no NPCEntity component")
        }
        
        if (npc.roleName != "Bunny") {
            return TestResult(false, "Expected roleName 'Bunny' but got '${npc.roleName}'")
        }
        
        return TestResult(true)
    }
}

/**
 * Test: Verify spawned baby has AnimalsGrowComponent attached
 */
class BabyHasGrowthComponentTest : TestCase("BabyHasGrowthComponent") {
    override fun run(world: World, store: Store<EntityStore>, playerPosition: Vector3d): TestResult {
        val spawnPos = Vector3d(playerPosition.x + 4, playerPosition.y, playerPosition.z)
        
        // Spawn a bunny
        val spawnResult = NPCPlugin.get().spawnNPC(store, "Bunny", null, spawnPos, Vector3f(0f, 0f, 0f))
            ?: return TestResult(false, "spawnNPC returned null")
        
        val ref = spawnResult.first()
            ?: return TestResult(false, "spawnNPC ref is null")
        
        // Check for AnimalsGrowComponent
        val growComponentType = AnimalsGrow.getComponentType()
        
        val growComponent = store.getComponent(ref, growComponentType)
        if (growComponent == null) {
            return TestResult(false, "Baby does not have AnimalsGrowComponent - check if 'Bunny' is in growth config")
        }
        
        return TestResult(true)
    }
}

/**
 * Test: Verify growth component has valid data
 */
class GrowthProgressTest : TestCase("GrowthComponentData") {
    override fun run(world: World, store: Store<EntityStore>, playerPosition: Vector3d): TestResult {
        val spawnPos = Vector3d(playerPosition.x + 6, playerPosition.y, playerPosition.z)
        
        // Spawn a bunny
        val spawnResult = NPCPlugin.get().spawnNPC(store, "Bunny", null, spawnPos, Vector3f(0f, 0f, 0f))
            ?: return TestResult(false, "spawnNPC returned null")
        
        val ref = spawnResult.first()
            ?: return TestResult(false, "spawnNPC ref is null")
        
        val growComponentType = AnimalsGrow.getComponentType()
        
        val growComponent = store.getComponent(ref, growComponentType)
            ?: return TestResult(false, "No AnimalsGrowComponent on baby")
        
        // Verify component has valid data
        if (growComponent.growthDurationSeconds <= 0) {
            return TestResult(false, "Growth duration is invalid: ${growComponent.growthDurationSeconds}")
        }
        
        // Verify spawn time was set (not EPOCH)
        if (growComponent.spawnTime == Instant.EPOCH) {
            return TestResult(false, "Spawn time was not set (still EPOCH)")
        }
        
        return TestResult(true)
    }
}
