package com.ryanh.agent_discord_bot.listener;

import com.ryanh.agent_discord_bot.service.PostOutService;
import org.springframework.stereotype.Component;

@Component
public class DeletePostOutListener {

    private final PostOutService postOutService;

    public DeletePostOutListener(PostOutService postOutService) {
        this.postOutService = postOutService;
    }
}
