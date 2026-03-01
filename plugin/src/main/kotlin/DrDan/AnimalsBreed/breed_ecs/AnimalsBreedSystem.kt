package DrDan.AnimalsBreed.breed_ecs

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.component.SystemGroup
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.server.npc.entities.NPCEntity
import com.hypixel.hytale.component.system.tick.EntityTickingSystem
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent

import java.util.function.BiConsumer

import DrDan.AnimalsBreed.AnimalsBreed
import DrDan.AnimalsBreed.AnimalsBreedAction

class AnimalsBreedSystem : EntityTickingSystem<EntityStore>() {
    override fun tick(
        dt: Float,
        index: Int,
        archetypeChunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>) {
        val animalsBreedComp = archetypeChunk.getComponent(index, AnimalsBreed.getComponentType()) ?: return

        val worldTimeResource = store.getResource(WorldTimeResource.getResourceType()) ?: return
        val currentGameTime = worldTimeResource.gameTime

        if (animalsBreedComp.isInLove && animalsBreedComp.shouldStopInLove(currentGameTime)) {
            animalsBreedComp.stopInLove();
        }

        if (animalsBreedComp.recentlyBred && animalsBreedComp.shouldStopBredCooldown(currentGameTime)) {
            animalsBreedComp.stopBredCooldown();
        }
        
        val ref = archetypeChunk.getReferenceTo(index)
        commandBuffer.replaceComponent(ref, AnimalsBreed.getComponentType(), animalsBreedComp)
    }

    override fun getGroup(): SystemGroup<EntityStore>? = DamageModule.get().gatherDamageGroup

    override fun getQuery(): Query<EntityStore> = Query.and(AnimalsBreed.getComponentType())
}
