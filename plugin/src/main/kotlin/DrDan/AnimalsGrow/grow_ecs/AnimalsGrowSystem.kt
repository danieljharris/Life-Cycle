package DrDan.AnimalsGrow.grow_ecs

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.SystemGroup
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.tick.EntityTickingSystem
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

import DrDan.AnimalsGrow.AnimalsGrow
import DrDan.AnimalsGrow.AnimalsGrowAction
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f

data class GrowthSpawn(val npcName: String, val position: Vector3d, val rotation: Vector3f)

class AnimalsGrowSystem : EntityTickingSystem<EntityStore>() {

    override fun tick(
        dt: Float,
        index: Int,
        archetypeChunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {
        val animalsGrow = archetypeChunk.getComponent(index, AnimalsGrow.getComponentType()) ?: return
        val ref = archetypeChunk.getReferenceTo(index)
        
        animalsGrow.addElapsedTime(dt)
        if (animalsGrow.elapsedTime >= animalsGrow.tickInterval) {
            animalsGrow.resetElapsedTime()
            animalsGrow.decrementRemainingTicks()
            println("AnimalsGrowSystem: Ticked entity. Remaining ticks: ${animalsGrow.remainingTicks}")
        }
        if (animalsGrow.isExpired()) {
            AnimalsGrowAction.grow(ref, store, commandBuffer)
            println("AnimalsGrowSystem: Entity has grown!")
        }
    }

    override fun getGroup(): SystemGroup<EntityStore>? = DamageModule.get().gatherDamageGroup

    override fun getQuery(): Query<EntityStore> = Query.and(AnimalsGrow.getComponentType())
}
