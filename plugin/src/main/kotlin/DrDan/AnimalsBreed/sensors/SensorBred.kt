package DrDan.AnimalsBreed.sensors

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport
import com.hypixel.hytale.server.npc.corecomponents.SensorBase
import com.hypixel.hytale.server.npc.role.Role
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider

import DrDan.AnimalsBreed.builders.BuilderSensorBred
import DrDan.AnimalsBreed.components.BreedComponent

class SensorBred(builder: BuilderSensorBred, support: BuilderSupport) : SensorBase(builder) {

    companion object {
        private val LOGGER: HytaleLogger = HytaleLogger.forEnclosingClass()
    }

    protected val isBred: Boolean = builder.getIsBred(support)

    override fun getSensorInfo(): InfoProvider? = null

    override fun matches(ref: Ref<EntityStore>, role: Role, dt: Double, store: Store<EntityStore>): Boolean {
        val breedComponent = store.getComponent(ref, BreedComponent.getComponentType())
        if (breedComponent == null) {
            LOGGER.atSevere().log("Sensor Bred failed to get Breed Component")
            return false
        } else {
            LOGGER.atInfo().log("Sensor Bred successfully got Breed Component")
        }

        return super.matches(ref, role, dt, store) && (breedComponent.isBreed() == this.isBred)
    }
}