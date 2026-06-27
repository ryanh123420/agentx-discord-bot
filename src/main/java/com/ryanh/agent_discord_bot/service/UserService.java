package com.ryanh.agent_discord_bot.service;

import com.ryanh.agent_discord_bot.model.RaidCharacter;
import com.ryanh.agent_discord_bot.model.User;
import com.ryanh.agent_discord_bot.model.WowUtilsRoster;
import com.ryanh.agent_discord_bot.repository.RaidCharacterRepository;
import com.ryanh.agent_discord_bot.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RaidCharacterRepository raidCharacterRepository;
    private final WowUtilsClient wowUtilsClient;

    public UserService(UserRepository userRepository,
                       RaidCharacterRepository raidCharacterRepository,
                       WowUtilsClient wowUtilsClient) {
        this.userRepository = userRepository;
        this.raidCharacterRepository = raidCharacterRepository;
        this.wowUtilsClient = wowUtilsClient;
    }

    public String insertUser(String battleTag, String discordId) {
        WowUtilsRoster roster = wowUtilsClient.getRoster();

        Optional<WowUtilsRoster.Member> match = roster.members()
                .stream()
                .filter(m -> m.battletag().equalsIgnoreCase(battleTag))
                .findFirst();

        if(match.isEmpty()) {
            return "BattleTag not found on WoWUtils";
        }

        User user = new User();
        user.setBattleTag(battleTag);
        user.setDiscordId(discordId);
        userRepository.save(user);

        WowUtilsRoster.Member member = match.get();
        for(WowUtilsRoster.Character c: member.characters()) {
            RaidCharacter raidChar = new RaidCharacter(c.name(), user, c.charClass(), c.spec());
            raidCharacterRepository.save(raidChar);
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Registered BattleTag: ").append(battleTag).append("\n");
        builder.append("Added Characters:\n");

        for(WowUtilsRoster.Character c: member.characters()) {
            builder.append(c.name()).append("\n");
        }

        return builder.toString();
    }
}
