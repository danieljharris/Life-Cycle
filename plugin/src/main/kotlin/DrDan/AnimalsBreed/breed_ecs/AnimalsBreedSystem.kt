package DrDan.AnimalsBreed.breed_ecs

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.SystemGroup
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.server.npc.entities.NPCEntity
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.system.tick.EntityTickingSystem
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport

import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import java.util.function.BiConsumer

import DrDan.AnimalsBreed.AnimalsBreed
import DrDan.AnimalsBreed.AnimalsBreedAction

class AnimalsBreedSystem : EntityTickingSystem<EntityStore>() {
    
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
        val animalsBreedComp = archetypeChunk.getComponent(index, AnimalsBreed.getComponentType()) ?: return
        val ref = archetypeChunk.getReferenceTo(index)

        // Get transform of this NPC
        val transformCompType = TransformComponent.getComponentType() as? com.hypixel.hytale.component.ComponentType<EntityStore, TransformComponent> ?: return
        val transform = store.getComponent(ref, transformCompType) ?: return
        val pos = transform.position

        // Find nearby NPCs (within 1 block) that share a breeding group
        val nearbyParents = mutableListOf<String>()

        val npcCompType = NPCEntity.getComponentType() as? com.hypixel.hytale.component.ComponentType<EntityStore, NPCEntity> ?: return

        // TODO: Make two mobs move towards each other and do some love particles,
        //       instead of just instantly spawning baby
        // npcCompType.getPathManager

        // iterate all NPC chunks and entities and collect those near this pos
        // val myGroup = animalsBreedComp.breedingGroup
        // val myNpcComp = try { archetypeChunk.getComponent(index, npcCompType) } catch (e: Exception) { null }
        // val myRole = try { myNpcComp?.getRoleName() } catch (e: Exception) { null }
        // store.forEachChunk(Query.or(npcCompType), AnimalsBreedChunkConsumer(nearbyParents, pos, myGroup, myRole, npcCompType, transformCompType, store))

        // // If we have at least two parents, pick a baby option and spawn
        // if (nearbyParents.size >= 2) {
        //     val distinctParents = nearbyParents.toSet()
        //     val babyOptions = AnimalsBreedAction.getBabyOptionsForParents(distinctParents)
        //     if (babyOptions.isNotEmpty()) {
        //         // pick random baby
        //         val chosen = babyOptions.shuffled().first()
        //         // spawn near current NPC
        //         try {
        //             val npcPlugin = com.hypixel.hytale.server.npc.NPCPlugin.get()
        //             val spawnPos = Vector3d(pos.x, pos.y, pos.z)
        //             val yawPitch = Vector3f(0f, 0f, 0f)
        //             npcPlugin.spawnNPC(store as com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore>, chosen, chosen, spawnPos, yawPitch)
        //             println("AnimalsBreedSystem: Spawned baby $chosen for parents ${distinctParents.joinToString()}")
        //         } catch (e: Exception) {
        //             println("AnimalsBreedSystem: Failed to spawn baby: ${e}")
        //         }
        //     }
        // }
    }

    override fun getGroup(): SystemGroup<EntityStore>? = DamageModule.get().gatherDamageGroup

    override fun getQuery(): Query<EntityStore> = Query.and(AnimalsBreed.getComponentType())
}
