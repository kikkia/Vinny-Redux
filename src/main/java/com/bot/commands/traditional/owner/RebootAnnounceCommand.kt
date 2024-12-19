package com.bot.commands.traditional.owner

import com.bot.commands.traditional.OwnerCommand
import com.bot.db.ResumeAudioDAO
import com.bot.db.mappers.ResumeAudioTrackMapper
import com.bot.i18n.Translator
import com.bot.voice.GuildVoiceProvider
import com.jagrosh.jdautilities.command.CommandEvent

class RebootAnnounceCommand : OwnerCommand() {
    private val resumeAudioDAO = ResumeAudioDAO.getInstance()
    private val translator = Translator.getInstance()

    init {
        name = "reboot"
    }

    override fun executeCommand(commandEvent: CommandEvent) {
        // Just pre-emptively remove all leftovers
        resumeAudioDAO.removeAll()
        val connections = GuildVoiceProvider.getInstance().getAll()
        for (conn in connections) {
            if (conn.nowPlaying() != null) {
                val locale = conn.guild.locale.locale
                try {
                    resumeAudioDAO.storeResumeGuild(conn.guild.id,
                        conn.currentVoiceChannel!!.id,
                        conn.lastTextChannel!!.id,
                        conn.getVolume(),
                        conn.volumeLocked,
                        ResumeAudioTrackMapper.queueToTracks(conn))
                } catch (e: Exception) {
                    logger.severe("Failed to store guild ${conn.guild.id}", e)
                    commandEvent.replyError("Failed to store for ${conn.guild}, $e")
                    e.printStackTrace()
                    conn.lastTextChannel!!.sendMessage(translator.translate("REBOOT_ERROR_MESSAGE", locale)).complete()
                }
                // TODO: handle error on send message
                conn.lastTextChannel!!.sendMessage(translator.translate("REBOOT_ANNOUNCE_MESSAGE", locale)).complete()
            }
        }
        commandEvent.reactSuccess()
    }
}