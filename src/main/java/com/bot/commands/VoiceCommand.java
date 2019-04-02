package com.bot.commands;

import com.bot.exceptions.ForbiddenCommandException;
import com.bot.utils.CommandCategories;
import com.bot.metrics.MetricsManager;
import com.bot.utils.CommandPermissions;
import com.bot.utils.Logger;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.Permission;

public abstract class VoiceCommand extends Command {
    protected MetricsManager metricsManager;

    public VoiceCommand() {
        this.category = CommandCategories.VOICE;
        this.guildOnly = true;
        this.botPermissions = new Permission[]{Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS};

        this.metricsManager = MetricsManager.getInstance();
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        // Check the permissions to do the command
        try {
            if (!CommandPermissions.canExecuteCommand(this, commandEvent))
                return;
        } catch (ForbiddenCommandException e) {
            commandEvent.replyWarning(e.getMessage());
            return;
        } catch (Exception e) {
            commandEvent.replyError("Something went wrong parsing permissions, please try again later.");
            Logger logger = new Logger(this.getClass().getName());
            logger.severe("Failed command " + this.getClass().getName() + ": ", e);
            e.printStackTrace();
        }

        try {
            executeCommand(commandEvent);
        } catch (Exception e) {
            commandEvent.replyError("Something went wrong, please try again later");
            Logger logger = new Logger(this.getClass().getName());
            logger.severe("Failed command " + this.getClass().getName() + ": ", e);
            e.printStackTrace();
        }
    }

    protected abstract void executeCommand(CommandEvent commandEvent);
}
