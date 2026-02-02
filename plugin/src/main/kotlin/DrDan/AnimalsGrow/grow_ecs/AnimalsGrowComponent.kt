package DrDan.AnimalsGrow.grow_ecs

import com.hypixel.hytale.component.Component
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

class AnimalsGrowComponent : Component<EntityStore> {
    var tickInterval: Float = 0f
    var remainingTicks: Int = 0
    var elapsedTime: Float = 0f

    constructor() : this(1.0f, 10)

    constructor(tickInterval: Float, totalTicks: Int) {
        this.tickInterval = tickInterval
        this.remainingTicks = totalTicks
        this.elapsedTime = 0f
    }

    constructor(other: AnimalsGrowComponent) {
        this.tickInterval = other.tickInterval
        this.remainingTicks = other.remainingTicks
        this.elapsedTime = other.elapsedTime
    }

    override fun clone(): Component<EntityStore> = AnimalsGrowComponent(this)

    fun addElapsedTime(dt: Float) { this.elapsedTime += dt }
    fun resetElapsedTime() { this.elapsedTime = 0f }
    fun decrementRemainingTicks() { this.remainingTicks-- }
    fun isExpired(): Boolean = this.remainingTicks <= 0
}
