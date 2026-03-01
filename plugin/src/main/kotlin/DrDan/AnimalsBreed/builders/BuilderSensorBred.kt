package DrDan.AnimalsBreed.builders

import com.google.gson.JsonElement
import com.hypixel.hytale.server.npc.asset.builder.Builder
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport
import com.hypixel.hytale.server.npc.asset.builder.holder.BooleanHolder
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase
import com.hypixel.hytale.server.npc.instructions.Sensor
import DrDan.AnimalsBreed.sensors.SensorBred

class BuilderSensorBred : BuilderSensorBase() {

    protected val isBred: BooleanHolder = BooleanHolder()

    fun getIsBred(builderSupport: BuilderSupport): Boolean {
        return this.isBred.get(builderSupport.getExecutionContext())
    }

    override fun getShortDescription(): String = "Checks whether or not the NPC is bred"

    override fun getLongDescription(): String = getShortDescription()

    override fun build(builderSupport: BuilderSupport): Sensor = SensorBred(this, builderSupport)

    override fun getBuilderDescriptorState(): BuilderDescriptorState = BuilderDescriptorState.Stable

    override fun readConfig(data: JsonElement): Builder<Sensor> {
        this.getBoolean(
            data,
            "IsBred",
            this.isBred,
            false,
            BuilderDescriptorState.Stable,
            "If NPC is bred or not",
            ""
        )
        return this
    }
}