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
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource
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
import DrDan.AnimalsBreed.config.BabyForAdultEntry
import DrDan.AnimalsBreed.config.AnimalsBreedConfig

object AnimalsBreedAction {
    private var breedGroups: List<BreedEntry> = listOf()
    private var babyForAdult: List<BabyForAdultEntry> = listOf()

    fun initialize(breedConfig: AnimalsBreedConfig) {
        this.breedGroups = breedConfig.breedGroup
        this.babyForAdult = breedConfig.babyForAdult
    }

    fun getBreedGroups(): List<BreedEntry> = breedGroups

    fun getBabyMappings(): List<BabyForAdultEntry> = babyForAdult

    fun getBabyOptionsForParents(parents: Collection<String>): List<String> {
        val results = mutableSetOf<String>()
        for (p in parents) {
            for (entry in babyForAdult) {
                if (entry.adult == p && entry.baby != null) results.add(entry.baby!!)
            }
        }
        return results.toList()
    }

    fun breed(
        ref1: Ref<EntityStore>,
        ref2: Ref<EntityStore>,
        store: Store<EntityStore>
    ) {
        val npcComponentType = NPCEntity.getComponentType() as? ComponentType<EntityStore, NPCEntity> ?: return

        val breed1 = store.getComponent(ref1, AnimalsBreed.getComponentType()) ?: return
        val npcEntity1 = store.getComponent(ref1, npcComponentType) ?: return
        val npcName1: String = try { npcEntity1.getRoleName() } catch (e: Exception) { return }

        val breed2 = store.getComponent(ref2, AnimalsBreed.getComponentType()) ?: return
        val npcEntity2 = store.getComponent(ref2, npcComponentType) ?: return
        val npcName2: String = try { npcEntity2.getRoleName() } catch (e: Exception) { return }

        val transformComponentType = TransformComponent.getComponentType() as? ComponentType<EntityStore, TransformComponent> ?: return
        val transform: TransformComponent = store.getComponent(ref1, transformComponentType) ?: return

        // find baby options for this parent
        val baby1 = babyForAdult.find { it.adult == npcName1 }?.baby ?: return
        val baby2 = babyForAdult.find { it.adult == npcName2 }?.baby ?: return

        // Randomly pick one of the two strings as the baby to spawn
        val chosen = listOf(baby1, baby2).shuffled().first()

        val world = store.getExternalData().getWorld()
        world.execute {
            try {
                NPCPlugin.get().spawnNPC(store, chosen, null, transform.position, transform.rotation)
                println("Animal breeding: $npcName1 spawned baby $chosen at ${transform.position}")
            } catch (e: Exception) {
                println("Animal breeding spawn failed: ${e}")
            }

            val worldTimeResource = store.getResource(WorldTimeResource.getResourceType())

            breed1.startBredCooldown(worldTimeResource.gameTime)
            breed2.startBredCooldown(worldTimeResource.gameTime)

            store.replaceComponent(ref1, AnimalsBreed.getComponentType(), breed1)
            store.replaceComponent(ref2, AnimalsBreed.getComponentType(), breed2)
        }
    }
}