package DrDan.AnimalsBreed.registry

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.system.tick.TickingSystem
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

import DrDan.AnimalsBreed.AnimalsBreed
import DrDan.AnimalsBreed.registry.AnimalsBreedRegistry

class AnimalsBreedRegistrySystem : TickingSystem<EntityStore>() {
    override fun tick(dt: Float, index: Int, store: Store<EntityStore>) {
        // val world = store.getExternalData().getWorld()
        AnimalsBreedRegistry.tick(store)
    }
}