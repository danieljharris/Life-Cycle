package DrDan.AnimalsBreed.breed_ecs

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.SystemGroup
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.server.npc.entities.NPCEntity
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import java.util.function.BiConsumer
import com.hypixel.hytale.math.vector.Vector3d

class AnimalsBreedChunkConsumer(
    private val nearbyParents: MutableList<String>,
    private val pos: Vector3d,
    private val myGroup: Array<String>?,
    private val myRole: String?,
    private val npcCompType: com.hypixel.hytale.component.ComponentType<EntityStore, NPCEntity>,
    private val transformCompType: com.hypixel.hytale.component.ComponentType<EntityStore, TransformComponent>,
    private val store: Store<EntityStore>
) : BiConsumer<ArchetypeChunk<EntityStore>, Any?> {

    override fun accept(chunk: ArchetypeChunk<EntityStore>, cb: Any?) {
        // val size = chunk.size()
        // for (i in 0 until size) {
        //     val otherRef = chunk.getReferenceTo(i)
        //     val otherNpc = chunk.getComponent(i, npcCompType) ?: continue
        //     val otherTransform = try { store.getComponent(otherRef, transformCompType) as? TransformComponent } catch (e: Exception) { null } ?: continue
        //     val otherPos = otherTransform.position
        //     val dx = pos.x - otherPos.x
        //     val dy = pos.y - otherPos.y
        //     val dz = pos.z - otherPos.z
        //     val distSq = dx*dx + dy*dy + dz*dz
        //     if (distSq <= 1.0 * 1.0) {
        //         val otherBreedComp = try { store.getComponent(otherRef, DrDan.AnimalsBreed.AnimalsBreed.getComponentType()) } catch (e: Exception) { null }
        //         val otherGroup = try { (otherBreedComp as? DrDan.AnimalsBreed.AnimalsBreed)?.breedingGroup } catch (e: Exception) { null }
        //         if (myGroup != null && otherGroup != null) {
        //             // check intersection
        //             if (myGroup.any { otherGroup.contains(it) }) {
        //                 val otherRole = try { otherNpc.getRoleName() } catch (e: Exception) { null }
        //                 if (myRole != null) nearbyParents.add(myRole)
        //                 if (otherRole != null) nearbyParents.add(otherRole)
        //             }
        //         }
        //     }
        // }
    }
}
