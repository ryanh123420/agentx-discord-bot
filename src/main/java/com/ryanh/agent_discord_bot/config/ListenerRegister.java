package com.ryanh.agent_discord_bot.config;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListenerRegister {
    public ListenerRegister(JDA jda, List<ListenerAdapter> listeners) {
        listeners.forEach(jda::addEventListener);
    }
}
