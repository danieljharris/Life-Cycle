package DrDan.AnimalsBreed.command

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
import DrDan.AnimalsBreed.AnimalsBreedAction
import DrDan.AnimalsBreed.config.BreedEntry
import DrDan.AnimalsBreed.AnimalsBreed
import DrDan.AnimalsBreed.breed_ecs.AnimalsBreedComponent

import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f

class AnimalsBreedCommand : AbstractPlayerCommand {
    constructor() : super("agbreed", "Put all animals in love")

    override fun execute(
        commandContext: CommandContext,
        store: Store<EntityStore>,
        ref: Ref<EntityStore>,
        playerRef: PlayerRef,
        world: World
    ) {
        val breedGroups = AnimalsBreedAction.getBreedGroups()
        val babyMappings = AnimalsBreedAction.getBabyMappings()

        store.forEachChunk(java.util.function.BiConsumer { chunk: ArchetypeChunk<EntityStore>, commandBuffer: CommandBuffer<EntityStore> ->
            for (i in 0 until chunk.size()) {
                val entityRef = chunk.getReferenceTo(i)
                addBreedComponentIfAdult(entityRef, store, commandBuffer, breedGroups, babyMappings)
            }
        })
    }

    private fun addBreedComponentIfAdult(
        ref: Ref<EntityStore>,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>,
        breedGroups: List<BreedEntry>,
        babyMappings: List<DrDan.AnimalsBreed.config.BabyForAdultEntry>
    ) {
        val npcComponentType = NPCEntity.getComponentType() as? ComponentType<EntityStore, NPCEntity> ?: return
        val npcEntity = store.getComponent(ref, npcComponentType) ?: return
        val npcName: String = try { npcEntity.getRoleName() } catch (e: Exception) { return }

        // Already has component?
        val existing = store.getComponent(ref, AnimalsBreed.getComponentType())
        if (existing != null) return

        // Check breed groups
        val matchingGroup = breedGroups.find { it.breedingGroup?.contains(npcName) == true }

        // Check baby mapping adults
        val isMappedAdult = babyMappings.any { it.adult == npcName }

        if (matchingGroup != null || isMappedAdult) {
            val comp = AnimalsBreedComponent()
            if (matchingGroup != null) comp.breedingGroup = matchingGroup.breedingGroup
            // add component to entity
            commandBuffer.addComponent(ref, AnimalsBreed.getComponentType(), comp)
            LOGGER.at(java.util.logging.Level.INFO).log("Added AnimalsBreedComponent to $npcName (group=${matchingGroup?.breedingGroup?.joinToString()})")
        }
    }

    companion object {
        val LOGGER: HytaleLogger = HytaleLogger.forEnclosingClass()
    }
}
