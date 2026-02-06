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
import com.hypixel.hytale.protocol.AnimationSlot
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import DrDan.AnimalsGrow.AnimalsGrowAction
import DrDan.AnimalsGrow.config.GrowthEntry

import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f

class AnimalsGrowAnimationCommand : AbstractPlayerCommand {
    constructor() : super("aga", "Force all animals to grow from babies to adult")

    override fun execute(
        commandContext: CommandContext,
        store: Store<EntityStore>,
        ref: Ref<EntityStore>,
        playerRef: PlayerRef,
        world: World
    ) {
        val growthConfig = AnimalsGrowAction.getConfig()
        
        store.forEachChunk(java.util.function.BiConsumer { chunk: ArchetypeChunk<EntityStore>, commandBuffer: CommandBuffer<EntityStore> ->
            for (i in 0 until chunk.size()) {
                val entityRef = chunk.getReferenceTo(i)

                val npcComponentType = NPCEntity.getComponentType() as? ComponentType<EntityStore, NPCEntity> ?: continue
                val npcEntity = store.getComponent(entityRef, npcComponentType) ?: continue
                val npcName: String = npcEntity.roleName ?: continue

                for (growthEntry in growthConfig) {
                    if (growthEntry.baby == null || npcName != growthEntry.baby) continue

                    AnimationUtils.playAnimation(
                            entityRef,
                            AnimationSlot.Action,
                            "Grow",
                            true,
                            store
                    );
                    break
                }
            }
        })
    }

    companion object {
        val LOGGER: HytaleLogger = HytaleLogger.forEnclosingClass()
    }
}
