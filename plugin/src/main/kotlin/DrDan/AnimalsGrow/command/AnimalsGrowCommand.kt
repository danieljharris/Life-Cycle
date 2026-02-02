package DrDan.AnimalsGrow.command

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.component.RemoveReason
import com.hypixel.hytale.server.npc.NPCPlugin
import com.hypixel.hytale.server.npc.entities.NPCEntity
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import DrDan.AnimalsGrow.AnimalsGrowAction
import DrDan.AnimalsGrow.config.GrowthEntry

import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f

data class SpawnOperation(
    val position: Vector3d,
    val rotation: Vector3f,
    val npcName: String
)

class AnimalsGrowCommand : AbstractPlayerCommand {
    constructor() : super("ag", "Force all animals to grow from babies to adult")

    override fun execute(
        commandContext: CommandContext,
        store: Store<EntityStore>,
        ref: Ref<EntityStore>,
        playerRef: PlayerRef,
        world: World
    ) {
        val growthConfig = AnimalsGrowAction.getConfig()
        val spawnsToQueue = mutableListOf<SpawnOperation>()
        
        store.forEachChunk(java.util.function.BiConsumer { chunk: ArchetypeChunk<EntityStore>, commandBuffer: CommandBuffer<EntityStore> ->
            for (i in 0 until chunk.size()) {
                val entityRef = chunk.getReferenceTo(i)
                growEntity(entityRef, store, commandBuffer, growthConfig, spawnsToQueue)
            }
        })
        
        // After store processing completes, queue spawns on the world thread
        if (spawnsToQueue.isNotEmpty()) {
            for (spawn in spawnsToQueue) {
                world.execute {
                    NPCPlugin.get().spawnNPC(store, spawn.npcName, null, spawn.position, spawn.rotation)
                }
            }
        }
    }

    private fun growEntity(
        ref: Ref<EntityStore>,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>,
        growthConfig: List<GrowthEntry>,
        spawnsToQueue: MutableList<SpawnOperation>
    ) {
        val npcComponentType = NPCEntity.getComponentType() as? ComponentType<EntityStore, NPCEntity> ?: return
        val npcEntity = store.getComponent(ref, npcComponentType) ?: return
        val npcName: String = npcEntity.roleName ?: return

        for (growthEntry in growthConfig) {
            if (growthEntry.baby == null || npcName != growthEntry.baby) continue
            val adultName = growthEntry.adult ?: continue

            val transformComponentType = TransformComponent.getComponentType() as? ComponentType<EntityStore, TransformComponent> ?: return
            val transform = store.getComponent(ref, transformComponentType) ?: return
            commandBuffer.removeEntity(ref, RemoveReason.REMOVE)
            spawnsToQueue.add(SpawnOperation(transform.position, transform.rotation, adultName))
            break
        }
    }

    companion object {
        val LOGGER: HytaleLogger = HytaleLogger.forEnclosingClass()
    }
}
