package DrDan.AnimalsGrow;

import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.List;

import DrDan.AnimalsGrow.annotation.ReturnIfNull;
import DrDan.AnimalsGrow.config.GrowthEntry;

public final class AnimalsGrowAction {
    private static AnimalsGrowAction instance;
    private final List<GrowthEntry> config;

    private AnimalsGrowAction(List<GrowthEntry> config) { this.config = config; }

    public static void initialize(List<GrowthEntry> config) {
        if (instance == null) {
            instance = new AnimalsGrowAction(config);
        }
    }

    public static AnimalsGrowAction getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AnimalsGrowAction not initialized. Call initialize() first.");
        }
        return instance;
    }

    public final void Grow(Ref<EntityStore> ref, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        @ReturnIfNull NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());
        @ReturnIfNull String npcName = npcEntity.getRoleName();

        // Check if the entity model matches any growth entry's babies
        for (GrowthEntry growthEntry : config) {
            if (npcName.equals(growthEntry.getBaby())) {
                @ReturnIfNull TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
                commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
                NPCPlugin.get().spawnNPC(store, growthEntry.getAdult(), null, transform.getPosition(), transform.getRotation());
                break;
            }
        }
    }
}