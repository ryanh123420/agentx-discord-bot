package com.ryanh.agent_discord_bot.listener;

import com.ryanh.agent_discord_bot.service.PostOutService;
import org.springframework.stereotype.Component;

@Component
public class EditPostOutListener {

    private final PostOutService postOutService;

    public EditPostOutListener(PostOutService postOutService) {
        this.postOutService = postOutService;
    }
}
