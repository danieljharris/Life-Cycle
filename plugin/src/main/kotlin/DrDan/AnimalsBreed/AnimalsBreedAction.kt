package DrDan.AnimalsBreed

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.protocol.Color
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.npc.NPCPlugin
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.protocol.BlockMaterial
import com.hypixel.hytale.component.RemoveReason
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.npc.entities.NPCEntity
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.component.spatial.SpatialResource
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate
import com.hypixel.hytale.server.core.modules.entity.EntityModule
import com.hypixel.hytale.server.core.universe.world.ParticleUtil
import com.hypixel.hytale.server.core.entity.effect.ActiveEntityEffect
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSpawner
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes


import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent
import com.hypixel.hytale.server.core.asset.type.model.config.Model

import it.unimi.dsi.fastutil.objects.ObjectList;

import DrDan.AnimalsBreed.config.BreedEntry

object AnimalsBreedAction {
    private var breedGroups: List<DrDan.AnimalsBreed.config.BreedEntry> = listOf()
    private var babyForAdult: List<DrDan.AnimalsBreed.config.BabyForAdultEntry> = listOf()

    fun initialize(breedConfig: DrDan.AnimalsBreed.config.AnimalsBreedConfig) {
        this.breedGroups = breedConfig.breedGroup
        this.babyForAdult = breedConfig.babyForAdult
    }

    fun getBreedGroups(): List<DrDan.AnimalsBreed.config.BreedEntry> = breedGroups

    fun getBabyMappings(): List<DrDan.AnimalsBreed.config.BabyForAdultEntry> = babyForAdult

    fun getBabyOptionsForParents(parents: Collection<String>): List<String> {
        val results = mutableSetOf<String>()
        for (p in parents) {
            for (entry in babyForAdult) {
                if (entry.adult == p && entry.baby != null) results.add(entry.baby!!)
            }
        }
        return results.toList()
    }

    fun tryBreed(
        ref: Ref<EntityStore>,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {
        val npcComponentType = NPCEntity.getComponentType() as? ComponentType<EntityStore, NPCEntity> ?: return
        val npcEntity = store.getComponent(ref, npcComponentType) ?: return
        val npcName: String = try { npcEntity.getRoleName() } catch (e: Exception) { return }

        val breedEntry = breedGroups.find { it.breedingGroup?.contains(npcName) == true } ?: return

        val transformComponentType = TransformComponent.getComponentType() as? ComponentType<EntityStore, TransformComponent> ?: return
        val transform: TransformComponent = store.getComponent(ref, transformComponentType) ?: return

        // find baby options for this parent
        val babies = babyForAdult.filter { it.adult == npcName }.mapNotNull { it.baby }
        if (babies.isEmpty()) return

        val chosen = babies.shuffled().first()
        try {
            val npcPlugin = NPCPlugin.get()
            val spawnPos = Vector3d(transform.position.x, transform.position.y, transform.position.z)
            val yawPitch = Vector3f(0f, 0f, 0f)
            npcPlugin.spawnNPC(store as com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore>, chosen, chosen, spawnPos, yawPitch)
            println("Animal breeding: $npcName spawned baby $chosen at ${transform.position}")
        } catch (e: Exception) {
            println("Animal breeding spawn failed: ${e}")
        }
    }
}