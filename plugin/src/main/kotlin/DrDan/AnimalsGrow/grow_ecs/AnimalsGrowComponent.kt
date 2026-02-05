package DrDan.AnimalsGrow.grow_ecs

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

import java.time.Instant

class AnimalsGrowComponent : Component<EntityStore> {
    var spawnTime: Instant = Instant.EPOCH
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

    fun shouldGrow(currentGameTime: Instant): Boolean {
        val elapsedSeconds = java.time.Duration.between(spawnTime, currentGameTime).seconds
        return elapsedSeconds >= growthDurationSeconds
    }
    
    fun getGrowthProgress(currentGameTime: Instant): Float {
        val elapsedSeconds = java.time.Duration.between(spawnTime, currentGameTime).seconds
        return (elapsedSeconds.toFloat() / growthDurationSeconds.toFloat()).coerceAtLeast(0f)
    }
    
    fun getRemainingSeconds(currentGameTime: Instant): Long {
        val elapsedSeconds = java.time.Duration.between(spawnTime, currentGameTime).seconds
        return (growthDurationSeconds - elapsedSeconds).coerceAtLeast(0L)
    }

    // Animation
    companion object {
    @JvmStatic
    var CODEC:BuilderCodec<AnimalsGrowComponent> = BuilderCodec.builder(AnimalsGrowComponent.class, AnimalsGrowComponent::new)
        .append(
                new KeyedCodec<String>("Grow", Codec.STRING),
                (o, v) -> o.animationName = v,
                (o) -> o.animationName
        )
        .add()
        .build();
    }

    public String getAnimationName() {
        return growAnimation;
    }
}
