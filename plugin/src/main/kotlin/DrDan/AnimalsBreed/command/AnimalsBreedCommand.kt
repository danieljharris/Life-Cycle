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
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource

import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f

import DrDan.AnimalsBreed.config.BabyForAdultEntry
import DrDan.AnimalsBreed.AnimalsBreedAction
import DrDan.AnimalsBreed.registry.AnimalsBreedRegistry
import DrDan.AnimalsBreed.config.BreedEntry
import DrDan.AnimalsBreed.AnimalsBreed
import DrDan.AnimalsBreed.breed_ecs.AnimalsBreedComponent

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
        babyMappings: List<BabyForAdultEntry>
    ) {
        val npcComponentType = NPCEntity.getComponentType() as? ComponentType<EntityStore, NPCEntity> ?: return
        val npcEntity = store.getComponent(ref, npcComponentType) ?: return
        val npcName: String = try { npcEntity.getRoleName() } catch (e: Exception) { return }

        // Check breed groups
        val matchingGroup = breedGroups.find { it.breedingGroup?.contains(npcName) == true }

        // Check baby mapping adults
        val isMappedAdult = babyMappings.any { it.adult == npcName }

        if (matchingGroup != null || isMappedAdult) {
            if (matchingGroup?.breedingGroup.isNullOrEmpty()) {
                LOGGER.at(java.util.logging.Level.WARNING).log("Skipping $npcName since it has no valid breeding group")
                return
            }

            val worldTimeResource = store.getResource(WorldTimeResource.getResourceType())
            val comp = AnimalsBreedComponent(worldTimeResource.gameTime, matchingGroup!!.breedingGroup!!)

            // If component already exists, remove it and replace with new in-love version
            try {
                commandBuffer.removeComponent(ref, AnimalsBreed.getComponentType())
            } catch (e: Exception) {}

            commandBuffer.addComponent(ref, AnimalsBreed.getComponentType(), comp)
            AnimalsBreedRegistry.add(ref)
            LOGGER.at(java.util.logging.Level.INFO).log("Added AnimalsBreedComponent to $npcName (group=${matchingGroup.breedingGroup?.joinToString()})")
        }
    }

    companion object {
        val LOGGER: HytaleLogger = HytaleLogger.forEnclosingClass()
    }
}
