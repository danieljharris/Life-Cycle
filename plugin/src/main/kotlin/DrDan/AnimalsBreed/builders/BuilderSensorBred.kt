package DrDan.AnimalsBreed.builders

import com.google.gson.JsonElement
import com.hypixel.hytale.server.npc.asset.builder.Builder
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport
import com.hypixel.hytale.server.npc.asset.builder.holder.BooleanHolder
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase
import com.hypixel.hytale.server.npc.instructions.Sensor
import com.willowaway.mobexampleproject.sensors.SensorTamed

class BuilderSensorTamed : BuilderSensorBase() {

    protected val isTamed: BooleanHolder = BooleanHolder()

    fun getIsTamed(builderSupport: BuilderSupport): Boolean {
        return this.isTamed.get(builderSupport.getExecutionContext())
    }

    override fun getShortDescription(): String = "Checks whether or not the NPC is tame"

    override fun getLongDescription(): String = getShortDescription()

    override fun build(builderSupport: BuilderSupport): Sensor = SensorTamed(this, builderSupport)

    override fun getBuilderDescriptorState(): BuilderDescriptorState = BuilderDescriptorState.Stable

    override fun readConfig(data: JsonElement): Builder<Sensor> {
        this.getBoolean(
            data,
            "IsTamed",
            this.isTamed,
            false,
            BuilderDescriptorState.Stable,
            "If NPC is tame or not",
            ""
        )
        return this
    }
}