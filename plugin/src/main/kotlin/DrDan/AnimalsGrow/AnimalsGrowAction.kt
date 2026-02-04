package DrDan.AnimalsGrow

import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent
import com.hypixel.hytale.server.core.entity.effect.ActiveEntityEffect
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.npc.NPCPlugin
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.RemoveReason
import com.hypixel.hytale.server.npc.entities.NPCEntity
import DrDan.AnimalsGrow.config.GrowthEntry

/**
 * Data class to hold properties that should be transferred from baby to adult.
 */
data class GrowthTransfer(
    val healthPercentage: Float,                    // Health as percentage (0.0 to 1.0)
    val displayName: Message?,                      // Custom display name if set
    val activeEffects: Array<ActiveEntityEffect>?,  // Active buffs/debuffs
    val ownerUUID: java.util.UUID?                  // Owner UUID for tamed animals
)

object AnimalsGrowAction {
    private lateinit var config: List<GrowthEntry>
    
    // Health stat is typically index 0 in Hytale's stat system
    private const val HEALTH_STAT_ID = "Health"
    
    fun initialize(growthConfig: List<GrowthEntry>) { config = growthConfig }
    
    fun getConfig(): List<GrowthEntry> = config

    fun grow(
        ref: Ref<EntityStore>,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {
        val npcComponentType = NPCEntity.getComponentType() as? ComponentType<EntityStore, NPCEntity> ?: return
        val npcEntity = store.getComponent(ref, npcComponentType) ?: return
        val npcName: String = npcEntity.roleName ?: return

        for (growthEntry in config) {
            if (growthEntry.baby == null || npcName != growthEntry.baby) continue
            val adultName = growthEntry.adult ?: continue

            val transformComponentType = TransformComponent.getComponentType() as? ComponentType<EntityStore, TransformComponent> ?: return
            val transform: TransformComponent = store.getComponent(ref, transformComponentType) ?: return
            
            // Capture properties to transfer BEFORE removing the baby
            val transfer = captureTransferProperties(ref, store)
            
            // Log the transformation
            println("Animal growing: $npcName -> $adultName at ${transform.position} (health: ${(transfer.healthPercentage * 100).toInt()}%)")
            
            // Remove the baby entity
            commandBuffer.removeEntity(ref, RemoveReason.REMOVE)
            
            // Spawn on world thread for thread safety
            val world = Universe.get().defaultWorld
            if (world != null) {
                world.execute {
                    val spawnResult = NPCPlugin.get().spawnNPC(store, adultName, null, transform.position, transform.rotation)
                    
                    // spawnNPC returns a Pair<Ref, INonPlayerCharacter> - extract the Ref
                    val adultRef = spawnResult?.first()
                    
                    // Apply transferred properties to the adult
                    if (adultRef != null) {
                        applyTransferProperties(adultRef, store, transfer)
                    }
                }
            } else {
                println("ERROR: Could not get default world from Universe!")
            }

            break
        }
    }
    
    /**
     * Capture properties from the baby entity that should transfer to the adult.
     */
    private fun captureTransferProperties(
        ref: Ref<EntityStore>,
        store: Store<EntityStore>
    ): GrowthTransfer {
        var healthPercentage = 1.0f  // Default to full health
        var displayName: Message? = null
        var activeEffects: Array<ActiveEntityEffect>? = null
        var ownerUUID: java.util.UUID? = null
        
        // Get health percentage from EntityStatMap
        val statMapType = EntityStatMap.getComponentType() as? ComponentType<EntityStore, EntityStatMap>
        if (statMapType != null) {
            val statMap = store.getComponent(ref, statMapType)
            if (statMap != null) {
                val healthStat = statMap.get(HEALTH_STAT_ID)
                if (healthStat != null) {
                    healthPercentage = healthStat.asPercentage()
                    println("  Captured health: ${healthStat.get()}/${healthStat.max} (${(healthPercentage * 100).toInt()}%)")
                }
            }
        }
        
        // Get custom display name
        val displayNameType = DisplayNameComponent.getComponentType() as? ComponentType<EntityStore, DisplayNameComponent>
        if (displayNameType != null) {
            val displayNameComponent = store.getComponent(ref, displayNameType)
            if (displayNameComponent != null) {
                displayName = displayNameComponent.displayName
                println("  Captured display name: ${displayName?.rawText}")
            }
        }
        
        // Get active effects (buffs/debuffs)
        val effectControllerType = EffectControllerComponent.getComponentType() as? ComponentType<EntityStore, EffectControllerComponent>
        if (effectControllerType != null) {
            val effectController = store.getComponent(ref, effectControllerType)
            if (effectController != null) {
                activeEffects = effectController.allActiveEntityEffects
                if (activeEffects != null && activeEffects.isNotEmpty()) {
                    println("  Captured ${activeEffects.size} active effects")
                }
            }
        }
        
        // Check for owner/taming info (placeholder for future taming system integration)
        // NPCEntity doesn't have a direct owner field exposed
        
        return GrowthTransfer(
            healthPercentage = healthPercentage,
            displayName = displayName,
            activeEffects = activeEffects,
            ownerUUID = ownerUUID
        )
    }
    
    /**
     * Apply transferred properties to the newly spawned adult.
     */
    private fun applyTransferProperties(
        adultRef: Ref<EntityStore>,
        store: Store<EntityStore>,
        transfer: GrowthTransfer
    ) {
        // Apply health percentage
        val statMapType = EntityStatMap.getComponentType() as? ComponentType<EntityStore, EntityStatMap>
        if (statMapType != null) {
            val statMap = store.getComponent(adultRef, statMapType)
            if (statMap != null) {
                val healthStat = statMap.get(HEALTH_STAT_ID)
                if (healthStat != null) {
                    // Calculate the new health value based on percentage of adult's max health
                    val newHealth = healthStat.max * transfer.healthPercentage
                    statMap.setStatValue(healthStat.index, newHealth)
                    println("  Applied health to adult: $newHealth/${healthStat.max} (${(transfer.healthPercentage * 100).toInt()}%)")
                }
            }
        }
        
        // Apply custom display name
        if (transfer.displayName != null) {
            // TODO: Maybe only do this when transfer name begins with "Test_"
            store.replaceComponent(adultRef, DisplayNameComponent.getComponentType(), DisplayNameComponent(transfer.displayName))
        }
        
        // Apply active effects
        if (transfer.activeEffects != null && transfer.activeEffects.isNotEmpty()) {
            val effectControllerType = EffectControllerComponent.getComponentType() as? ComponentType<EntityStore, EffectControllerComponent>
            if (effectControllerType != null) {
                val effectController = store.getComponent(adultRef, effectControllerType)
                if (effectController != null) {
                    // Add all the captured effects to the adult
                    effectController.addActiveEntityEffects(transfer.activeEffects)
                    println("  Applied ${transfer.activeEffects.size} effects to adult")
                }
            }
        }
        
        // Apply owner (taming) - placeholder for future implementation
        // transfer.ownerUUID?.let { ... }
    }
}
