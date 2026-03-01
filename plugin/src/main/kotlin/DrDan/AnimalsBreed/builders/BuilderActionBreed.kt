package DrDan.AnimalsBreed.builders

import com.hypixel.hytale.server.npc.instructions.Action
import com.hypixel.hytale.server.npc.asset.builder.Builder
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState
import com.hypixel.hytale.server.npc.asset.builder.holder.StringArrayHolder
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase

import com.google.gson.JsonElement
import DrDan.AnimalsBreed.actions.ActionBreed

class BuilderActionBreed : BuilderActionBase() {
    protected val breedItems: StringArrayHolder = StringArrayHolder()

    override fun getLongDescription(): String { return getShortDescription() }
    override fun getShortDescription(): String { return "Breeds a NPC to make it a pet" }
    override fun build(builderSupport: BuilderSupport): Action { return ActionBreed(this, builderSupport) }
    override fun getBuilderDescriptorState(): BuilderDescriptorState { return BuilderDescriptorState.Stable }
    
    fun getBreedItems(support: BuilderSupport): Array<String> { return this.breedItems.get(support.getExecutionContext()) ?: emptyArray() }

    override fun readConfig(data: JsonElement): Builder<Action> {
        this.requireStringArray(
            data,
            "BreedItems",
            this.breedItems,
            1,
            Int.MAX_VALUE,
            null,
            BuilderDescriptorState.Stable,
            "The items to breed an NPC",
            "The items to breed an NPC"
        )
        return super.readConfig(data)
    }
}