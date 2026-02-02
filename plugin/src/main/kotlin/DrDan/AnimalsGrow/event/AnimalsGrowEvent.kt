package DrDan.AnimalsGrow.event

import com.hypixel.hytale.component.AddReason
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.RemoveReason
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.RefSystem
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.entities.NPCEntity
import DrDan.AnimalsGrow.AnimalsGrow
import DrDan.AnimalsGrow.AnimalsGrowAction
import DrDan.AnimalsGrow.config.GrowthEntry
import DrDan.AnimalsGrow.grow_ecs.AnimalsGrowComponent

class AnimalsGrowEvent(
    private val config: List<GrowthEntry>
) : RefSystem<EntityStore>() {

    override fun getQuery(): Query<EntityStore> = Query.or(NPCEntity.getComponentType())

    override fun onEntityAdded(
        ref: Ref<EntityStore>,
        reason: AddReason,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {
        if (reason != AddReason.SPAWN) return

        // Skip if already has AnimalsGrowComponent
        if (store.getComponent(ref, AnimalsGrow.getComponentType()) != null) return

        val npcComponentType = NPCEntity.getComponentType() as? ComponentType<EntityStore, NPCEntity> ?: return
        val npc = store.getComponent(ref, npcComponentType) ?: return
        val npcName = npc.roleName ?: return

        for (growthEntry in config) {
            if (growthEntry.baby == null || npcName != growthEntry.baby) continue

            val seconds = growthEntry.timeToGrowUpSeconds ?: continue
            val tickInterval = 1.0f
            val totalTicks = (seconds / tickInterval).toInt()
            
            val animalsGrowComponent = AnimalsGrowComponent(tickInterval, totalTicks)
            commandBuffer.addComponent(ref, AnimalsGrow.getComponentType(), animalsGrowComponent)
            break
        }
    }

    override fun onEntityRemove(
        ref: Ref<EntityStore>,
        reason: RemoveReason,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {}
}
