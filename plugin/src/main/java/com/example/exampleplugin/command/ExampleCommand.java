package com.example.exampleplugin.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;

import javax.annotation.Nonnull;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.jline.console.impl.Builtins.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleCommand extends AbstractPlayerCommand {
    public ExampleCommand() {
        super("example", "An example command");
    }

    @Override
    protected void execute(@NonNullDecl CommandContext comandContext, @NonNullDecl Store<EntityStore> store,
        @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
            EventTitleUtil.showEventTitleToPlayer(playerRef, Message.raw("Dr Dan's First Plugin"), Message.raw("Great Things To Come... 2"), true);
            // EntityStore entityStore = world.getEntityStore();
            // entityStore.getWorld().getEntityStore().getStore().get
    }
}