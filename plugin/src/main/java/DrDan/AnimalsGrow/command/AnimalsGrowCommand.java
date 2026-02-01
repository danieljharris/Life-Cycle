package DrDan.AnimalsGrow.command;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.component.Ref;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import DrDan.AnimalsGrow.AnimalsGrowAction;

public class AnimalsGrowCommand extends AbstractPlayerCommand {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public AnimalsGrowCommand() {
        super("ag", "Force all animals to grow from babies to adult");
    }

    @Override
    protected void execute(@NonNullDecl CommandContext comandContext, @NonNullDecl Store<EntityStore> store,
        @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        store.forEachChunk((ArchetypeChunk<EntityStore> chunk, CommandBuffer<EntityStore> commandBuffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                Ref<EntityStore> entityRef = chunk.getReferenceTo(i);
                AnimalsGrowAction.getInstance().Grow(entityRef, store, commandBuffer);
            }
        });
    }
}