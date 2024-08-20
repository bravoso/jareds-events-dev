package com.bravoso.jaredsevents;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class CommandHandler {

    private EventManager eventManager;

    public CommandHandler(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    private static final List<String> EVENTS = Arrays.asList(
            "nojump", "noforward", "noleftclick", "oneheart", "blindness", "adventuremode", "damageiftouchingblocks",
            "nomining", "notoolsorweapons", "nobuildables", "nonether", "withoutdoinganything", "nocrafting"
    );

    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal("jevent");

        builder.then(CommandManager.literal("start")
                .executes(this::startEvent));

        builder.then(CommandManager.literal("stop")
                .executes(this::stopEvent));

        builder.then(CommandManager.literal("event")
                .then(CommandManager.argument("eventName", StringArgumentType.word())
                        .suggests((context, builder1) -> CommandSource.suggestMatching(EVENTS, builder1))
                        .executes(this::triggerEvent)));

        dispatcher.register(builder);
    }

    private int startEvent(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();

            // Check if eventManager is properly initialized
            if (eventManager == null) {
                source.sendError(Text.literal("Event manager is not initialized."));
                return 0;
            }

            eventManager.startRandomEvent();  // Start the event

            // Feedback to the player
            source.sendFeedback(() -> Text.literal("Event started!").formatted(Formatting.GREEN), true);

            return 1;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("An error occurred while trying to start the event."));
            e.printStackTrace();  // Log the error
            return 0;
        }
    }

    private int stopEvent(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (eventManager.isEventActive()) {
            eventManager.stopEvent();
            source.sendFeedback(() -> Text.literal("Event stopped!").formatted(Formatting.RED), true);
        } else {
            source.sendError(Text.literal("No active event to stop."));
        }
        return 1;
    }

    private int triggerEvent(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String eventName = StringArgumentType.getString(context, "eventName");

        if (!eventManager.isEventActive()) {
            source.sendError(Text.literal("No active event to trigger a specific event."));
            return 0;
        }

        boolean success = eventManager.triggerSpecificEvent(eventName);
        if (success) {
            source.sendFeedback(() -> Text.literal("Event " + eventName + " triggered!").formatted(Formatting.GREEN), true);
        } else {
            source.sendError(Text.literal("Event " + eventName + " not found!"));
        }
        return success ? 1 : 0;
    }
}
