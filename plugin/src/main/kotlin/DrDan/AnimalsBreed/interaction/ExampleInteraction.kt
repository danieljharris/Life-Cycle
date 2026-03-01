// package DrDan.AnimalsBreed.interaction

// import com.google.gson.JsonElement
// import com.hypixel.hytale.builtin.adventure.npcshop.npc.ActionOpenBarterShop
// import com.hypixel.hytale.builtin.adventure.npcshop.npc.BarterShopExistsValidator
// import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState
// import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport
// import com.hypixel.hytale.server.npc.asset.builder.InstructionType
// import com.hypixel.hytale.server.npc.asset.builder.holder.AssetHolder
// import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase
// import com.hypixel.hytale.server.npc.instructions.Action

// import java.util.EnumSet
// import javax.annotation.Nonnull

// class BuilderActionOpenBarterShop : BuilderActionBase() {

// 	@Nonnull
// 	protected val shopId: AssetHolder = AssetHolder()

// 	@Nonnull
// 	override fun getShortDescription(): String {
// 		return "Open the barter shop UI for the current player"
// 	}

// 	@Nonnull
// 	override fun getLongDescription(): String {
// 		return getShortDescription()
// 	}

// 	@Nonnull
// 	override fun build(@Nonnull builderSupport: BuilderSupport): Action {
// 		return ActionOpenBarterShop(this, builderSupport)
// 	}

// 	@Nonnull
// 	override fun getBuilderDescriptorState(): BuilderDescriptorState {
// 		return BuilderDescriptorState.Stable
// 	}

// 	@Nonnull
// 	fun readConfig(@Nonnull data: JsonElement): BuilderActionOpenBarterShop {
// 		requireAsset(data, "Shop", this.shopId, BarterShopExistsValidator.required(), BuilderDescriptorState.Stable, "The barter shop to open", null)
// 		requireInstructionType(EnumSet.of(InstructionType.Interaction))
// 		return this
// 	}

// 	fun getShopId(@Nonnull support: BuilderSupport): String {
// 		return shopId.get(support.getExecutionContext())
// 	}
// }
