package DrDan.AnimalsGrow.grow_ecs

import com.hypixel.hytale.component.Component
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import java.time.Instant

/**
 * Component that tracks animal growth using in-game time.
 * When players sleep, in-game time advances faster, causing animals to grow faster.
 * This mirrors how crops work in Hytale.
 */
class AnimalsGrowComponent : Component<EntityStore> {
    /** The in-game time when this entity was spawned */
    var spawnTime: Instant = Instant.EPOCH
    
    /** How many in-game seconds until the animal grows up */
    var growthDurationSeconds: Long = 0L

    constructor() : this(Instant.EPOCH, 600L) // Default 10 minutes in-game time

    constructor(spawnTime: Instant, growthDurationSeconds: Long) {
        this.spawnTime = spawnTime
        this.growthDurationSeconds = growthDurationSeconds
    }

    constructor(other: AnimalsGrowComponent) {
        this.spawnTime = other.spawnTime
        this.growthDurationSeconds = other.growthDurationSeconds
    }

    override fun clone(): Component<EntityStore> = AnimalsGrowComponent(this)

    /**
     * Check if the animal should grow up based on current in-game time.
     * This automatically accounts for time skips from sleeping.
     */
    fun shouldGrow(currentGameTime: Instant): Boolean {
        val elapsedSeconds = java.time.Duration.between(spawnTime, currentGameTime).seconds
        return elapsedSeconds >= growthDurationSeconds
    }
    
    /**
     * Get the progress towards full growth (0.0 to 1.0+)
     */
    fun getGrowthProgress(currentGameTime: Instant): Float {
        val elapsedSeconds = java.time.Duration.between(spawnTime, currentGameTime).seconds
        return (elapsedSeconds.toFloat() / growthDurationSeconds.toFloat()).coerceAtLeast(0f)
    }
    
    /**
     * Get remaining in-game seconds until growth
     */
    fun getRemainingSeconds(currentGameTime: Instant): Long {
        val elapsedSeconds = java.time.Duration.between(spawnTime, currentGameTime).seconds
        return (growthDurationSeconds - elapsedSeconds).coerceAtLeast(0L)
    }
}
