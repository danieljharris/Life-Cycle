package DrDan.AnimalsBreed.registry

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent

import java.util.concurrent.ConcurrentHashMap

import DrDan.AnimalsBreed.AnimalsBreed
import DrDan.AnimalsBreed.AnimalsBreedAction

object AnimalsBreedRegistry {
    private val refs: MutableSet<Ref<EntityStore>> = ConcurrentHashMap.newKeySet() // Set ensures no duplication

    fun add(ref: Ref<EntityStore>) { refs.add(ref) }

    fun remove(ref: Ref<EntityStore>) { refs.remove(ref) }

    fun getAllSnapshot(): Set<Ref<EntityStore>> = HashSet(refs)

    fun tick(store: Store<EntityStore>) {
        val currentRefs = getAllSnapshot().toMutableSet()
        if (currentRefs.isEmpty()) return

        val matched = mutableSetOf<Ref<EntityStore>>()
        val iterator = currentRefs.iterator()

        while (iterator.hasNext()) {
            val ref = iterator.next()

            // skip already matched refs
            if (matched.contains(ref)) {
                iterator.remove()
                continue
            }

            val breedComp = try {
                store.getComponent(ref, AnimalsBreed.getComponentType())
            } catch (_: Throwable) {
                // invalid ref or store state; remove from registry and skip
                remove(ref)
                iterator.remove()
                continue
            }
            if (breedComp == null) {
                // component removed, drop from registry and this iteration
                remove(ref)
                iterator.remove()
                continue
            }

            if (!breedComp.getIsInLove() || breedComp.recentlyBred) {
                iterator.remove()
                continue
            }

            var breedGroup = breedComp.breedingGroup
            if (breedGroup.isEmpty()) {
                iterator.remove()
                continue
            }

            val closestMate = findClosestWithin(store, ref, 5.0, breedGroup, currentRefs)
            if (closestMate == null) {
                // no mate nearby this tick
                iterator.remove()
                continue
            }

            // If mate already matched elsewhere, skip
            if (matched.contains(closestMate)) {
                iterator.remove()
                continue
            }

            // mark both as matched so we don't process them again this tick
            matched.add(ref)
            matched.add(closestMate)

            AnimalsBreedAction.breed(ref, closestMate, store)

            iterator.remove()
        }
    }

    fun findClosestWithin(store: Store<EntityStore>, sourceRef: Ref<EntityStore>, maxDistance: Double, breedGroup: Array<String>, refs: Set<Ref<EntityStore>>): Ref<EntityStore>? {
        val transformType = TransformComponent.getComponentType() as? ComponentType<EntityStore, TransformComponent>?: return null

        val sourceTransform = try {
            store.getComponent(sourceRef, transformType)
        } catch (_: Throwable) {
            // source ref became invalid
            remove(sourceRef)
            return null
        } ?: return null
        val srcPos = sourceTransform.position

        var closestRef: Ref<EntityStore>? = null
        var closestDist = Double.MAX_VALUE

        for (other in refs) {
            if (other == sourceRef) continue

            // Check if other is in the same breed group
            val otherBreedComp = try {
                store.getComponent(other, AnimalsBreed.getComponentType())
            } catch (_: Throwable) {
                remove(other)
                continue
            }
            if (otherBreedComp == null) { remove(other); continue }
            val otherBreedGroup = otherBreedComp.breedingGroup
            if (otherBreedGroup.isEmpty() || !otherBreedGroup.any { breedGroup.contains(it) }) continue

            // Check distance is within maxDistance
            val otherTransform = try {
                store.getComponent(other, transformType)
            } catch (_: Throwable) {
                remove(other)
                continue
            }
            if (otherTransform == null) { remove(other); continue }
            val dist = srcPos.distanceTo(otherTransform.position)
            if (dist <= maxDistance && dist < closestDist) {
                closestDist = dist
                closestRef = other
            }
        }

        return closestRef
    }
}
