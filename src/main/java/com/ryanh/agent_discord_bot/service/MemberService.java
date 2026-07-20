package com.ryanh.agent_discord_bot.service;

import com.ryanh.agent_discord_bot.entity.Member;
import com.ryanh.agent_discord_bot.entity.RaidCharacter;
import com.ryanh.agent_discord_bot.model.WowUtilsRoster;
import com.ryanh.agent_discord_bot.repository.RaidCharacterRepository;
import com.ryanh.agent_discord_bot.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final RaidCharacterRepository raidCharacterRepository;
    private final WowUtilsClient wowUtilsClient;

    public MemberService(MemberRepository memberRepository,
                         RaidCharacterRepository raidCharacterRepository,
                         WowUtilsClient wowUtilsClient) {
        this.memberRepository = memberRepository;
        this.raidCharacterRepository = raidCharacterRepository;
        this.wowUtilsClient = wowUtilsClient;
    }

    public String insertUser(String battleTag, String discordId) {
        if(memberRepository.findByDiscordId(discordId).isPresent()) {
            return "You are already registered! Use /unregister if you need to setup a different BattleTag.";
        }
        if(memberRepository.findByBattleTag(battleTag).isPresent()) {
            return "This BattleTag is already registered to a different user.";
        }

        WowUtilsRoster roster = wowUtilsClient.getRoster();

        Optional<WowUtilsRoster.Member> match = roster.members()
                .stream()
                .filter(m -> m.battletag() != null)
                .filter(m -> m.battletag().equalsIgnoreCase(battleTag))
                .findFirst();

        if(match.isEmpty()) {
            return "BattleTag not found on WoWUtils.";
        }

        Member raider = new Member();
        raider.setBattleTag(battleTag);
        raider.setDiscordId(discordId);
        memberRepository.save(raider);

        WowUtilsRoster.Member member = match.get();
        for(WowUtilsRoster.Character c: member.characters()) {
            RaidCharacter raidChar = new RaidCharacter(c.name(), raider, c.charClass(), c.spec());
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

    public boolean checkIfRegistered(String discordId) {
        return memberRepository.findByDiscordId(discordId).isPresent();
    }

    public String removeUser(String discordId) {
        Optional<Member> user = memberRepository.findByDiscordId(discordId);
        memberRepository.delete(user.get());
        return "BattleTag and characters have been unregistered.";
    }
}
