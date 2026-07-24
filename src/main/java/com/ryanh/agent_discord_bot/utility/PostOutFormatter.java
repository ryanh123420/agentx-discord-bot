package com.ryanh.agent_discord_bot.utility;

import com.ryanh.agent_discord_bot.entity.PostOut;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostOutFormatter {

    public static String formatDate(PostOut postOut) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE M/d/yyyy");
        return postOut.getPostDate().format(formatter);
    }

    public static String formatDate(LocalDate postDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE M/d/yyyy");
        return postDate.format(formatter);
    }

    public static String formatPostOutReport(List<PostOut> postOutList) {
        if(postOutList.isEmpty()) {
            return "⭐ There are no post outs! ⭐";
        }
        StringBuilder builder = new StringBuilder();
        Map<String, List<String>> userOutput = new HashMap<>();
        for(PostOut post: postOutList) {
            String discordUser = post.getDiscordId();
            String date = post.getPostDate().format(DateTimeFormatter.ofPattern("EEE M/d/yyyy"));
            if(!userOutput.containsKey(discordUser)) {
                userOutput.put(discordUser, new ArrayList<>());
            }
            userOutput.get(discordUser).add(date);
        }

        for(String user: userOutput.keySet()) {
            builder.append("👤 <@").append(user).append(">")
                    .append("\n");
            for(String date: userOutput.get(user)) {
                builder.append("🗓️ ")
                        .append(date)
                        .append("\n");
            }
            builder.append("\n");
        }

        return builder.toString();
    }
}
