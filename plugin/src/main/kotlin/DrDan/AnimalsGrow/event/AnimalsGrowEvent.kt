package DrDan.AnimalsGrow.event

import com.hypixel.hytale.component.AddReason
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.RemoveReason
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.RefSystem
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.entities.NPCEntity
import DrDan.AnimalsGrow.AnimalsGrow
import DrDan.AnimalsGrow.AnimalsGrowAction
import DrDan.AnimalsGrow.config.GrowthEntry
import DrDan.AnimalsGrow.grow_ecs.AnimalsGrowComponent

/**
 * System that adds AnimalsGrowComponent to baby animals when they spawn.
 * 
 * The component is initialized with the current in-game time, so growth
 * is based on in-game time elapsed (not real time). This means:
 * - When players sleep, in-game time advances faster
 * - Animals will grow faster when players sleep (like crops)
 */
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

        var growthEntry = config.find { it.baby == npcName } ?: return
        val growthSeconds = growthEntry.timeToGrowUpSeconds ?: return
        
        // Get current in-game time to record spawn time
        val worldTimeResource = store.getResource(WorldTimeResource.getResourceType())
        
        // Create component with in-game spawn time and growth duration
        val animalsGrowComponent = AnimalsGrowComponent(
            spawnTime = worldTimeResource.gameTime,
            growthDurationSeconds = growthSeconds.toLong()
        )
        
        commandBuffer.addComponent(ref, AnimalsGrow.getComponentType(), animalsGrowComponent)
        println("AnimalsGrowEvent: Added growth component to $npcName, will grow in ${growthSeconds}s (in-game time)")
    }

    override fun onEntityRemove(
        ref: Ref<EntityStore>,
        reason: RemoveReason,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {}
}
