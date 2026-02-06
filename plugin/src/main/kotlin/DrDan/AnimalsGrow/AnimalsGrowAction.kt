package DrDan.AnimalsGrow

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
import com.hypixel.hytale.math.vector.Vector3i
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.protocol.BlockMaterial
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.npc.NPCPlugin
import com.hypixel.hytale.component.RemoveReason
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.npc.entities.NPCEntity
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate
import com.hypixel.hytale.server.core.entity.effect.ActiveEntityEffect
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes

import DrDan.AnimalsGrow.config.GrowthEntry

/**
 * Data class to hold properties that should be transferred from baby to adult.
 */
data class GrowthTransfer(
    // EntityStatMap properties
    val healthPercentage: Float,
    val oxygenPercentage: Float,
    val staminaPercentage: Float,
    val manaPercentage: Float,
    val signatureEnergyPercentage: Float,
    val ammoPercentage: Float,
    val namePlate: String?,
    val activeEffects: Array<ActiveEntityEffect>?,
)

object AnimalsGrowAction {
    private lateinit var config: List<GrowthEntry>
    
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

        var growthEntry = config.find { it.baby == npcName } ?: return
        val adultName = growthEntry.adult ?: return

        val transformComponentType = TransformComponent.getComponentType() as? ComponentType<EntityStore, TransformComponent> ?: return
        val transform: TransformComponent = store.getComponent(ref, transformComponentType) ?: return
        
        // Capture properties to transfer BEFORE removing the baby
        val transfer = captureTransferProperties(ref, store)
        
        // Log the transformation
        println("Animal growing: $npcName -> $adultName at ${transform.position}")
        
        // Remove the baby entity
        commandBuffer.removeEntity(ref, RemoveReason.REMOVE)
        
        // Spawn on world thread for thread safety
        val world = Universe.get().defaultWorld
        if (world != null) {
            world.execute {
                var spawnPos = transform.position
                var blockSpawnPos = Vector3i(
                    spawnPos.x.toInt(),
                    spawnPos.y.toInt(),
                    spawnPos.z.toInt()
                )

                var blockType: BlockType = world.getBlockType(blockSpawnPos)?: return@execute
                var material: BlockMaterial = blockType.material

                if (material == BlockMaterial.Solid) {
                    println("Spawn position blocked for adult at ${spawnPos}, searching nearby...")

                    val offsets = listOf(
                        Vector3i(-1, 0, -1),
                        Vector3i(-1, 0, 0),
                        Vector3i(-1, 0, 1),
                        Vector3i(0, 0, -1),
                        Vector3i(0, 0, 1),
                        Vector3i(1, 0, -1),
                        Vector3i(1, 0, 0),
                        Vector3i(1, 0, 1),
                    )
                    for (offset in offsets) {

                        var tempSpawnPos = blockSpawnPos
                        var tempSpawnPosOffset = tempSpawnPos.add(offset)

                        val checkBlockType = world.getBlockType(tempSpawnPosOffset) ?: continue
                        if (checkBlockType.material == BlockMaterial.Empty) {
                            spawnPos = tempSpawnPosOffset.toVector3d()
                            println("Found nearby spawn position for adult at ${spawnPos}")
                            break
                        }
                    }
                }
                else {
                    println("Spawn position clear for adult at ${spawnPos}")
                }

                // Set spawn location in center of block
                spawnPos = Vector3d(
                    spawnPos.x.toInt().toDouble() + 0.5,
                    spawnPos.y.toInt().toDouble(),
                    spawnPos.z.toInt().toDouble() + 0.5
                )

                val spawnResult = NPCPlugin.get().spawnNPC(store, adultName, null, spawnPos, transform.rotation)
                
                val adultRef = spawnResult?.first()
                if (adultRef != null) { applyTransferProperties(adultRef, store, transfer) }
            }
        } else {
            println("ERROR: Could not get default world from Universe!")
        }
    }
    
    /**
     * Capture properties from the baby entity that should transfer to the adult.
     */
    private fun captureTransferProperties(
        ref: Ref<EntityStore>,
        store: Store<EntityStore>
    ): GrowthTransfer {
        var healthPercentage = 1.0f
        var oxygenPercentage = 1.0f
        var staminaPercentage = 1.0f
        var manaPercentage = 1.0f
        var signatureEnergyPercentage = 1.0f
        var ammoPercentage = 1.0f
        var namePlate: String? = null
        var activeEffects: Array<ActiveEntityEffect>? = null
        
        // Get EntityStatMap
        val statMapType = EntityStatMap.getComponentType() as? ComponentType<EntityStore, EntityStatMap>
        if (statMapType != null) {
            val statMap = store.getComponent(ref, statMapType)
            if (statMap != null) {

                // Get health percentage
                val healthStat = statMap.get(DefaultEntityStatTypes.getHealth())
                if (healthStat != null) {
                    healthPercentage = healthStat.asPercentage()
                    println("  Captured health: ${healthStat.get()}/${healthStat.max} (${(healthPercentage * 100).toInt()}%)")
                }

                // Get oxygen percentage
                val oxygenStat = statMap.get(DefaultEntityStatTypes.getOxygen())
                if (oxygenStat != null) {
                    oxygenPercentage = oxygenStat.asPercentage()
                    println("  Captured oxygen: ${oxygenStat.get()}/${oxygenStat.max} (${(oxygenPercentage * 100).toInt()}%)")
                }

                // Get stamina percentage
                val staminaStat = statMap.get(DefaultEntityStatTypes.getStamina())
                if (staminaStat != null) {
                    staminaPercentage = staminaStat.asPercentage()
                    println("  Captured stamina: ${staminaStat.get()}/${staminaStat.max} (${(staminaPercentage * 100).toInt()}%)")
                }

                // Get mana percentage
                val manaStat = statMap.get(DefaultEntityStatTypes.getMana())
                if (manaStat != null) {
                    manaPercentage = manaStat.asPercentage()
                    println("  Captured mana: ${manaStat.get()}/${manaStat.max} (${(manaPercentage * 100).toInt()}%)")
                }

                // Get signature energy percentage
                val signatureEnergyStat = statMap.get(DefaultEntityStatTypes.getSignatureEnergy())
                if (signatureEnergyStat != null) {
                    signatureEnergyPercentage = signatureEnergyStat.asPercentage()
                    println("  Captured signature energy: ${signatureEnergyStat.get()}/${signatureEnergyStat.max} (${(signatureEnergyPercentage * 100).toInt()}%)")
                }

                // Get ammo percentage
                val ammoStat = statMap.get(DefaultEntityStatTypes.getAmmo())
                if (ammoStat != null) {
                    ammoPercentage = ammoStat.asPercentage()
                    println("  Captured ammo: ${ammoStat.get()}/${ammoStat.max} (${(ammoPercentage * 100).toInt()}%)")
                }
            }
        }
        
        // Get custom nameplate
        val namePlateComponent = store.getComponent(ref, Nameplate.getComponentType())
        if (namePlateComponent != null) {
            namePlate = namePlateComponent.getText()
            println("  Captured nameplate: ${namePlate}")
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
            oxygenPercentage = oxygenPercentage,
            staminaPercentage = staminaPercentage,
            manaPercentage = manaPercentage,
            signatureEnergyPercentage = signatureEnergyPercentage,
            ammoPercentage = ammoPercentage,
            namePlate = namePlate,
            activeEffects = activeEffects,
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
        val statMapType = EntityStatMap.getComponentType() as? ComponentType<EntityStore, EntityStatMap>
        if (statMapType != null) {
            val statMap = store.getComponent(adultRef, statMapType)
            if (statMap != null) {
                // Apply health percentage
                val healthStat = statMap.get(DefaultEntityStatTypes.getHealth())?: return
                val newHealth = healthStat.max * transfer.healthPercentage
                statMap.setStatValue(DefaultEntityStatTypes.getHealth(), newHealth)
                println("  Applied health to adult: $newHealth/${healthStat.max} (${(transfer.healthPercentage * 100).toInt()}%)")

                // Apply oxygen
                val oxygenStat = statMap.get(DefaultEntityStatTypes.getOxygen())
                if (oxygenStat != null) {
                    val newOxygen = oxygenStat.max * transfer.oxygenPercentage
                    statMap.setStatValue(DefaultEntityStatTypes.getOxygen(), newOxygen)
                    println("  Applied oxygen to adult: $newOxygen/${oxygenStat.max} (${(transfer.oxygenPercentage * 100).toInt()}%)")
                }

                // Apply stamina
                val staminaStat = statMap.get(DefaultEntityStatTypes.getStamina())
                if (staminaStat != null) {
                    val newStamina = staminaStat.max * transfer.staminaPercentage
                    statMap.setStatValue(DefaultEntityStatTypes.getStamina(), newStamina)
                    println("  Applied stamina to adult: $newStamina/${staminaStat.max} (${(transfer.staminaPercentage * 100).toInt()}%)")
                }

                // Apply mana
                val manaStat = statMap.get(DefaultEntityStatTypes.getMana())
                if (manaStat != null) {
                    val newMana = manaStat.max * transfer.manaPercentage
                    statMap.setStatValue(DefaultEntityStatTypes.getMana(), newMana)
                    println("  Applied mana to adult: $newMana/${manaStat.max} (${(transfer.manaPercentage * 100).toInt()}%)")
                }

                // Apply signature energy
                val signatureEnergyStat = statMap.get(DefaultEntityStatTypes.getSignatureEnergy())
                if (signatureEnergyStat != null) {
                    val newSignatureEnergy = signatureEnergyStat.max * transfer.signatureEnergyPercentage
                    statMap.setStatValue(DefaultEntityStatTypes.getSignatureEnergy(), newSignatureEnergy)
                    println("  Applied signature energy to adult: $newSignatureEnergy/${signatureEnergyStat.max} (${(transfer.signatureEnergyPercentage * 100).toInt()}%)")
                }

                // Apply ammo
                val ammoStat = statMap.get(DefaultEntityStatTypes.getAmmo())
                if (ammoStat != null) {
                    val newAmmo = ammoStat.max * transfer.ammoPercentage
                    statMap.setStatValue(DefaultEntityStatTypes.getAmmo(), newAmmo)
                    println("  Applied ammo to adult: $newAmmo/${ammoStat.max} (${(transfer.ammoPercentage * 100).toInt()}%)")
                }
            }
        }
        
        // Apply custom nameplate
        if (transfer.namePlate != null) {
            store.addComponent(adultRef, Nameplate.getComponentType(), Nameplate(transfer.namePlate))
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
    
    }
}