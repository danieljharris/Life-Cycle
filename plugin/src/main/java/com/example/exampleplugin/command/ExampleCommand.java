package com.example.exampleplugin.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.HolderSystem;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;

import javax.annotation.Nonnull;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.jline.console.impl.Builtins.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleCommand extends AbstractPlayerCommand {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public ExampleCommand() {
        super("example", "An example command");
    }

    @Override
    protected void execute(@NonNullDecl CommandContext comandContext, @NonNullDecl Store<EntityStore> store,
        @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
            EventTitleUtil.showEventTitleToPlayer(playerRef, Message.raw("Dr Dan's First Plugin"), Message.raw("Great Things To Come... 3"), true);
            // EntityStore entityStore = world.getEntityStore();
            // entityStore.getWorld().getEntityStore().getStore().get

        // Store<EntityStore> entityStore = world.getEntityStore().getStore();
        LOGGER.atInfo().log("Logging all entities in the world...");
        
        final int[] entityCount = {0};
        
        store.forEachChunk((ArchetypeChunk<EntityStore> chunk, CommandBuffer<EntityStore> commandBuffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                entityCount[0]++;
                
                // Try to get UUID if available
                UUIDComponent uuidComponent = chunk.getComponent(i, UUIDComponent.getComponentType());
                String uuidStr = uuidComponent != null ? uuidComponent.getUuid().toString() : "N/A";
                
                // Try to get model component if available
                ModelComponent modelComponent = chunk.getComponent(i, ModelComponent.getComponentType());
                String modelName = modelComponent != null ? modelComponent.getModel().getModelAssetId() : "N/A";
                
                // Try to get transform component if available
                TransformComponent transform = chunk.getComponent(i, TransformComponent.getComponentType());
                String position = transform != null ? 
                    String.format("(%.1f, %.1f, %.1f)", 
                        transform.getPosition().getX(),
                        transform.getPosition().getY(),
                        transform.getPosition().getZ()) : "N/A";

                if(modelName.equals("Piglet")) {
                        world.execute(() -> {
                        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
                        
                        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset("Pig");
                        Model model = Model.createScaledModel(modelAsset, 1.0f);
                        // TransformComponent transform = store.getComponent(playerRef.getReference(), EntityModule.get().getTransformComponentType());

                        // Vector3d vector3d = new Vector3d(0, 0, 0); // position
                        // Vector3f vector3f = new Vector3f(0, 0, 0); // rotation
                        // TransformComponent transform = new TransformComponent(vector3d, vector3f);

                        Vector3d vector3d = transform.getPosition();

                        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(vector3d, new Vector3f(0, 0, 0)));
                        holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
                        holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
                        holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(model.getBoundingBox()));
                        holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
                        holder.addComponent(Interactions.getComponentType(), new Interactions()); // you need to add interactions here if you want your entity to be interactable

                        holder.ensureComponent(UUIDComponent.getComponentType());
                        holder.ensureComponent(Interactable.getComponentType()); // if you want your entity to be interactable

                        store.addEntity(holder, AddReason.SPAWN);
                    });
                }
                
                // LOGGER.atInfo().log("Entity #%d: Model=%s, UUID=%s, Position=%s", entityCount[0], modelName, uuidStr, position);
                LOGGER.atInfo().log("Entity Model=%s", modelName);
            }
        });
        
        LOGGER.atInfo().log("Total entities logged: %d", entityCount[0]);
        EventTitleUtil.showEventTitleToPlayer(playerRef, Message.raw("Entities Logged"), Message.raw("Check console for details"), true);

        

        world.execute(() -> {
            Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
            
            ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset("Minecart");
            Model model = Model.createScaledModel(modelAsset, 1.0f);
            // TransformComponent transform = store.getComponent(playerRef.getReference(), EntityModule.get().getTransformComponentType());

            // Vector3d vector3d = new Vector3d(0, 0, 0); // position
            // Vector3f vector3f = new Vector3f(0, 0, 0); // rotation
            // TransformComponent transform = new TransformComponent(vector3d, vector3f);

            Vector3d vector3d = playerRef.getTransform().getPosition();

            holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(vector3d, new Vector3f(0, 0, 0)));
            holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
            holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
            holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(model.getBoundingBox()));
            holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
            holder.addComponent(Interactions.getComponentType(), new Interactions()); // you need to add interactions here if you want your entity to be interactable

            holder.ensureComponent(UUIDComponent.getComponentType());
            holder.ensureComponent(Interactable.getComponentType()); // if you want your entity to be interactable

            store.addEntity(holder, AddReason.SPAWN);
        });
    }
}