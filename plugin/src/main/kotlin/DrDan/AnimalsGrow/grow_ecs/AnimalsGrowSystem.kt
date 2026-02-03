package DrDan.AnimalsGrow.grow_ecs

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.SystemGroup
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.tick.EntityTickingSystem
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

import DrDan.AnimalsGrow.AnimalsGrow
import DrDan.AnimalsGrow.AnimalsGrowAction
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f

data class GrowthSpawn(val npcName: String, val position: Vector3d, val rotation: Vector3f)

/**
 * System that checks animal growth based on in-game time.
 * 
 * Uses WorldTimeResource to get the current in-game time, which advances
 * faster when players sleep. This means animals will grow faster when
 * players sleep, just like crops do in Hytale.
 */
class AnimalsGrowSystem : EntityTickingSystem<EntityStore>() {
    
    // Log throttle - only log every N ticks to reduce spam
    private var tickCount = 0
    private val logInterval = 100 // Log every 100 ticks (~5 seconds at 20 TPS)

    override fun tick(
        dt: Float,
        index: Int,
        archetypeChunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {
        val animalsGrow = archetypeChunk.getComponent(index, AnimalsGrow.getComponentType()) ?: return
        val ref = archetypeChunk.getReferenceTo(index)
        
        // Get the current in-game time from WorldTimeResource
        val worldTimeResource = store.getResource(WorldTimeResource.getResourceType()) ?: return
        val currentGameTime = worldTimeResource.gameTime
        
        // Check if animal should grow based on in-game time elapsed
        if (animalsGrow.shouldGrow(currentGameTime)) {
            AnimalsGrowAction.grow(ref, store, commandBuffer)
            println("AnimalsGrowSystem: Animal has grown up! (In-game time based)")
        } else {
            // Throttled progress logging
            tickCount++
            if (tickCount >= logInterval) {
                tickCount = 0
                val progress = animalsGrow.getGrowthProgress(currentGameTime)
                val remaining = animalsGrow.getRemainingSeconds(currentGameTime)
                println("AnimalsGrowSystem: Growth progress: ${(progress * 100).toInt()}%, ${remaining}s remaining (in-game time)")
            }
        }
    }

    override fun getGroup(): SystemGroup<EntityStore>? = DamageModule.get().gatherDamageGroup

    override fun getQuery(): Query<EntityStore> = Query.and(AnimalsGrow.getComponentType())
}
