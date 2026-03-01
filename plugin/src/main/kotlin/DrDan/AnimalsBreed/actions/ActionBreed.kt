package DrDan.AnimalsBreed.actions

import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport
import com.hypixel.hytale.server.npc.corecomponents.ActionBase
import com.hypixel.hytale.server.npc.role.Role
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider
import com.hypixel.hytale.server.npc.instructions.Action
import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource

import DrDan.AnimalsBreed.builders.BuilderActionBreed
import DrDan.AnimalsBreed.AnimalsBreedAction
import DrDan.AnimalsBreed.AnimalsBreed
import DrDan.AnimalsBreed.breed_ecs.AnimalsBreedComponent

class ActionBreed(builder: BuilderActionBreed, support: BuilderSupport) : ActionBase(builder) {
    override fun execute(ref: Ref<EntityStore>, role: Role, sensorInfo: InfoProvider?, dt: Double, store: Store<EntityStore>): Boolean {
        // Ensure the entity has an AnimalsBreedComponent; if not, add a default one
        val existing = store.getComponent(ref, AnimalsBreed.getComponentType()) as? AnimalsBreedComponent
        if (existing == null) {
            val worldTimeResource = store.getResource(WorldTimeResource.getResourceType()) ?: return true
            val comp = AnimalsBreedComponent(worldTimeResource.gameTime, arrayOf())
            try {
                store.addComponent(ref, AnimalsBreed.getComponentType(), comp)
            } catch (e: Exception) {
            }
        }

        return true
    }
}
