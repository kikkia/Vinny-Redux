package com.bot.commands.general;

import com.bot.commands.GeneralCommand;
import com.bot.utils.CommandPermissions;
import com.bot.utils.MetricsManager;
import com.jagrosh.jdautilities.command.CommandEvent;

public class PingCommand extends GeneralCommand {

    public PingCommand() {
        this.name = "ping";
        this.guildOnly = false;
        this.help = "Gets the ping from Vinny to discord.";

    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        metricsManager.markCommand(this, commandEvent.getAuthor(), commandEvent.getGuild());
        // Check the permissions to do the command
        if (!CommandPermissions.canExecuteCommand(this, commandEvent))
            return;

        commandEvent.reply(commandEvent.getJDA().getPing() + "ms");
        metricsManager.markCommand(this, commandEvent.getAuthor(), commandEvent.getGuild());
    }
}
