// package DrDan.AnimalsBreed.interaction

// import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction
// import com.hypixel.hytale.server.core.entity.InteractionContext
// import com.hypixel.hytale.codec.builder.BuilderCodec
// import com.hypixel.hytale.protocol.InteractionType
// import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler
// import com.hypixel.hytale.component.Ref
// import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
// import com.hypixel.hytale.component.Store
// import com.hypixel.hytale.server.core.entity.entities.Player
// import com.hypixel.hytale.server.core.inventory.ItemStack
// import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction
// import com.hypixel.hytale.server.core.Message
// import com.hypixel.hytale.protocol.Color

// /**
//  * Interaction that triggers when a player right-clicks an entity while holding a specific item.
//  *
//  * Notes:
//  * - Replace the placeholder checks (required item id, entity/component checks) with concrete types/ids
//  *   from your project (e.g. custom component type or entity role id).
//  */
// class EntityItemInteraction : SimpleInteraction() {

//     companion object {
//         @JvmField
//         val CODEC: BuilderCodec<EntityItemInteraction> =
//             BuilderCodec.builder(EntityItemInteraction::class.java, ::EntityItemInteraction, SimpleInteraction.CODEC)
//                 .build()
//     }

//     override fun tick0(
//         firstRun: Boolean,
//         time: Float,
//         type: InteractionType,
//         context: InteractionContext,
//         cooldownHandler: CooldownHandler
//     ) {
//         // Run only on right-click (secondary)
//         if (type != InteractionType.Secondary) return

//         // Ensure the target is an entity
//         val targetEntityRef: Ref<*>? = context.getTargetEntity()
//         if (targetEntityRef == null) return

//         // Validate player (owning entity)
//         val owningEntityRef: Ref<*>? = context.getOwningEntity()
//         if (owningEntityRef == null) return
//         val ownerStore = owningEntityRef.getStore()
//         // Use reflection to avoid Kotlin generic signature mismatch with Store.getComponent
//         val getComponentMethod = ownerStore.javaClass.getMethod(
//             "getComponent",
//             com.hypixel.hytale.component.Ref::class.java,
//             com.hypixel.hytale.component.ComponentType::class.java
//         )
//         val player: Player? = try {
//             getComponentMethod.invoke(ownerStore, owningEntityRef, Player.getComponentType()) as? Player
//         } catch (e: Exception) {
//             null
//         }
//         if (player == null) return

//         // Validate held item
//         val heldItem: ItemStack? = context.getHeldItem()
//         if (heldItem == null) return

//         val REQUIRED_ITEM_ID = "Required_Item_Id" // TODO: replace with real id
//         if (!heldItem.getItem().getId().equals(REQUIRED_ITEM_ID, ignoreCase = true)) return

//         // Consume the required amount from the held slot
//         val requiredAmount = 1
//         val tx: ItemStackSlotTransaction = player.inventory
//             .getHotbar()
//             .removeItemStackFromSlot(context.getHeldItemSlot().toShort(), requiredAmount, true, false)
//         if (!tx.succeeded()) return

//         // Validate entity type or component
//         val entityStore = targetEntityRef.getStore()
//         // TODO: replace `YourComponentType` below with your actual component type/class/ref
//         // val customComponent = entityStore.getComponent(targetEntityRef, YourComponentType)
//         // if (customComponent == null) return

//         // Apply custom logic: change entity state, add component, etc.
//         player.sendMessage(
//             Message.raw("Interaction successful")
//                 .color("green")
//                 .bold(true)
//         )
//     }
// }
