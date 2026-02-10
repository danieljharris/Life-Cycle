package DrDan.AnimalsBreed.breed_ecs

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

import java.time.Instant
import java.time.Duration

import DrDan.AnimalsBreed.config.BreedEntry

class AnimalsBreedComponent : Component<EntityStore> {
    var isInLove: Boolean = true
    var inLoveStartTime: Instant = Instant.EPOCH
    var inLoveTimeoutDurationSeconds: Long = 1800L // 30 minutes in-game time (adjust as needed)

    var recentlyBred: Boolean = false
    var bredCooldownStartTime: Instant = Instant.EPOCH
    var bredCooldownDurationSeconds: Long = 3000L // 50 minutes cooldown after breeding

    // Optional breeding group for this entity (role names of adults that constitute this group)
    var breedingGroup: Array<String>

    constructor() : this(Instant.EPOCH, arrayOf())

    constructor(inLoveStartTime: Instant, breedingGroup: Array<String>) {
        this.inLoveStartTime = inLoveStartTime
        this.inLoveTimeoutDurationSeconds = inLoveTimeoutDurationSeconds
        this.breedingGroup = breedingGroup
    }

    constructor(other: AnimalsBreedComponent) {
        this.isInLove = other.isInLove
        this.inLoveStartTime = other.inLoveStartTime
        this.inLoveTimeoutDurationSeconds = other.inLoveTimeoutDurationSeconds

        this.breedingGroup = other.breedingGroup

        this.recentlyBred = other.recentlyBred
        this.bredCooldownStartTime = other.bredCooldownStartTime
        this.bredCooldownDurationSeconds = other.bredCooldownDurationSeconds
    }

    override fun clone(): Component<EntityStore> = AnimalsBreedComponent(this)

    fun getIsInLove(): Boolean = isInLove
    fun getIsRecentlyBred(): Boolean = recentlyBred
    // property `breedingGroup` provides its own getter; avoid duplicate JVM signature

    fun shouldStopInLove(currentGameTime: Instant): Boolean {
        val elapsedSeconds = Duration.between(inLoveStartTime, currentGameTime).seconds
        return elapsedSeconds >= inLoveTimeoutDurationSeconds
    }
    
    fun getInLoveProgress(currentGameTime: Instant): Float {
        val elapsedSeconds = Duration.between(inLoveStartTime, currentGameTime).seconds
        return (elapsedSeconds.toFloat() / inLoveTimeoutDurationSeconds.toFloat()).coerceAtLeast(0f)
    }
    
    fun getRemainingSeconds(currentGameTime: Instant): Long {
        val elapsedSeconds = Duration.between(inLoveStartTime, currentGameTime).seconds
        return (inLoveTimeoutDurationSeconds - elapsedSeconds).coerceAtLeast(0L)
    }

    fun shouldStopBredCooldown(currentGameTime: Instant): Boolean {
        val elapsedSeconds = Duration.between(bredCooldownStartTime, currentGameTime).seconds
        return elapsedSeconds >= bredCooldownDurationSeconds
    }

    fun getBredCooldownProgress(currentGameTime: Instant): Float {
        val elapsedSeconds = Duration.between(bredCooldownStartTime, currentGameTime).seconds
        return (elapsedSeconds.toFloat() / bredCooldownDurationSeconds.toFloat()).coerceAtLeast(0f)
    }

    fun getBredCooldownRemainingSeconds(currentGameTime: Instant): Long {
        val elapsedSeconds = Duration.between(bredCooldownStartTime, currentGameTime).seconds
        return (bredCooldownDurationSeconds - elapsedSeconds).coerceAtLeast(0L)
    }

    fun startInLove(currentGameTime: Instant) {
        isInLove = true
        inLoveStartTime = currentGameTime
    }

    fun stopInLove() {
        isInLove = false
        inLoveStartTime = Instant.EPOCH
    }

    fun startBredCooldown(currentGameTime: Instant) {
        isInLove = false
        recentlyBred = true
        bredCooldownStartTime = currentGameTime
    }

    fun stopBredCooldown() {
        recentlyBred = false
        bredCooldownStartTime = Instant.EPOCH
    }
}
