package com.bot.commands.voice

import com.bot.Bot
import com.bot.commands.VoiceCommand
import com.bot.exceptions.MaxQueueSizeException
import com.bot.utils.Config
import com.bot.utils.FormattingUtils
import com.bot.voice.GuildVoiceProvider
import com.bot.voice.LavaLinkClient
import com.bot.voice.LavaLinkClient.Companion.getInstance
import com.bot.voice.VoiceSendHandler
import com.jagrosh.jdautilities.command.CommandEvent
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import datadog.trace.api.Trace
import dev.arbjerg.lavalink.client.LinkState
import net.dv8tion.jda.api.entities.Message
import java.util.concurrent.TimeUnit

class PlayCommand(private val bot: Bot) : VoiceCommand() {

    private val urlRegex = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"

    init {
        name = "play"
        arguments = "<title|URL>"
        help = "plays the provided audio track"
    }

    @Trace(operationName = "executeCommand", resourceName = "Play")
    override fun executeCommand(commandEvent: CommandEvent) {
        val guildVoiceConnection = guildVoiceProvider.getGuildVoiceConnection(commandEvent.guild)
        if (commandEvent.args.isEmpty()) {
            if (guildVoiceConnection.getPaused()) {
                guildVoiceConnection.setPaused(false)
                commandEvent.reply(commandEvent.client.success + " Resumed paused stream.")
            } else {
                commandEvent.reply(
                    """${commandEvent.client.warning} You must give me something to play.
                    `${commandEvent.client.prefix}play <URL>` - Plays media at the provided URL
                    `${commandEvent.client.prefix}play <search term>` - Searches youtube for the first result of the search term"""
                )
            }
            return
        }
        var url = commandEvent.args.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        if (!url.matches(urlRegex.toRegex())) {
            val searchPrefix = Config.getInstance().getConfig(Config.DEFAULT_SEARCH_PROVIDER, "ytsearch:")
            url = searchPrefix.plus(url)
        }
        commandEvent.reply("\u231A Loading... `[" + commandEvent.args + "]`") { _: Message? ->
            guildVoiceConnection.loadTrack(url, commandEvent)
        }
    }
}