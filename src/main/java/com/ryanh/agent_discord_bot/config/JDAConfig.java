package com.ryanh.agent_discord_bot.config;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Config class for JDA
 */
@Configuration
public class JDAConfig {

    @Value("${AGENCY_BOT_DISCORD_TOKEN}")
    private String token;

    @Value("${GUILD_ID}")
    private String guildId;

    @Bean
    public JDA build(List<ListenerAdapter> listeners) throws InterruptedException {
        JDA jda = JDABuilder
                .createDefault(token, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                .addEventListeners(listeners.toArray())
                .build()
                .awaitReady();

        jda.getGuildById(guildId)
                .updateCommands()
                .addCommands(
                        Commands.slash("register", "Register with BattleTag")
                                .addOption(OptionType.STRING, "battletag", "Enter BattleTag", true),
                        Commands.slash("unregister", "Un-register a BattleTag")
        ).queue();

        return jda;
    }


}
