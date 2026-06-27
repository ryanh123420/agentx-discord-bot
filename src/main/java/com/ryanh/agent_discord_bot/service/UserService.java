package com.ryanh.agent_discord_bot.service;

import com.ryanh.agent_discord_bot.model.User;
import com.ryanh.agent_discord_bot.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void insertUser(String battleTag, String discordId) {
        User user = new User();
        user.setBattleTag(battleTag);
        user.setDiscordId(discordId);
        userRepository.save(user);
    }
}
