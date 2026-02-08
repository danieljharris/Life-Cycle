package DrDan.AnimalsBreed.event

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

import DrDan.AnimalsBreed.AnimalsBreed
import DrDan.AnimalsBreed.AnimalsBreedAction
import DrDan.AnimalsBreed.config.BreedEntry
import DrDan.AnimalsBreed.breed_ecs.AnimalsBreedComponent

class AnimalsBreedEvent(
    private val config: List<BreedEntry>
) : RefSystem<EntityStore>() {

    override fun getQuery(): Query<EntityStore> = Query.or(NPCEntity.getComponentType())

    override fun onEntityAdded(
        ref: Ref<EntityStore>,
        reason: AddReason,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {
        if (reason != AddReason.SPAWN) return

        // Skip if already has AnimalsBreedComponent
        if (store.getComponent(ref, AnimalsBreed.getComponentType()) != null) return

        val npcComponentType = NPCEntity.getComponentType() as? ComponentType<EntityStore, NPCEntity> ?: return
        val npc = store.getComponent(ref, npcComponentType) ?: return
        val npcName = npc.roleName ?: return

        val breedEntry = config.find { it.breedingGroup?.contains(npcName) == true } ?: return

        // Create component with breeding group
        val comp = AnimalsBreedComponent()
        comp.breedingGroup = breedEntry.breedingGroup

        commandBuffer.addComponent(ref, AnimalsBreed.getComponentType(), comp)
        println("AnimalsBreedEvent: Added breed component to $npcName, with breed group ${breedEntry.breedingGroup?.joinToString()}")
    }

    override fun onEntityRemove(
        ref: Ref<EntityStore>,
        reason: RemoveReason,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {}
}
