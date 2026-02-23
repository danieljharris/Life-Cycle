package DrDan.AnimalsBreed.actions

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport
import com.hypixel.hytale.server.npc.corecomponents.ActionBase
import com.hypixel.hytale.server.npc.role.Role
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider

import DrDan.AnimalsBreed.actions.ActionTame
import DrDan.AnimalsBreed.builders.BuilderActionTame

class ActionTame(builder: BuilderActionTame, support: BuilderSupport) : ActionBase(builder) {
    protected val tameItems: Set<String> = HashSet(builder.getTameItems(support).toList())

    override fun canExecute(
        ref: Ref<EntityStore>,
        role: Role,
        sensorInfo: InfoProvider?,
        dt: Double,
        store: Store<EntityStore>
    ): Boolean {
        return super.canExecute(ref, role, sensorInfo, dt, store) && role.getStateSupport().getInteractionIterationTarget() != null
    }

    override fun execute(
        ref: Ref<EntityStore>,
        role: Role,
        sensorInfo: InfoProvider?,
        dt: Double,
        store: Store<EntityStore>
    ): Boolean {
        super.execute(ref, role, sensorInfo, dt, store)

        val playerReference = role.getStateSupport().getInteractionIterationTarget() ?: return false

        val playerRefComponent = store.getComponent(playerReference, PlayerRef.getComponentType()) ?: return false
        val player = store.getComponent(playerReference, Player.getComponentType()) ?: return false

        val tameComponent = store.getComponent(ref, AnimalsBreed.getComponentType()) ?: return false

        tameComponent.setIsTame(true)

        player.sendMessage(Message.raw("Action Tame was successfully called"))
        return true
    }
}