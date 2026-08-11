package com.ryanh.agent_discord_bot.utility;

import com.ryanh.agent_discord_bot.entity.PostOut;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PostOutFormatter {

    //Discord rejects any embed field value longer than this.
    private static final int MAX_FIELD_LENGTH = 1024;
    //Keeps the whole embed under Discord's 6000 character total.
    private static final int MAX_FIELDS_PER_SECTION = 2;

    public static String formatDate(PostOut postOut) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE M/d/yyyy");
        return postOut.getPostDate().format(formatter);
    }

    public static String formatDate(LocalDate postDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE M/d/yyyy");
        return postDate.format(formatter);
    }

    /**
     * Builds the officer report, split into one or more embed field values. A guild large enough
     * to exceed Discord's 1024 character field limit would otherwise make the embed unbuildable.
     * @param postOutList Post outs to report on
     * @return One or more field values, each within Discord's field limit
     */
    public static List<String> formatPostOutReport(List<PostOut> postOutList) {
        if(postOutList.isEmpty()) {
            return List.of("⭐ There are no post outs! ⭐");
        }

        //LinkedHashMap so the report keeps the order the post outs came back in.
        Map<String, List<String>> userOutput = new LinkedHashMap<>();
        for(PostOut post: postOutList) {
            userOutput.computeIfAbsent(post.getDiscordId(), id -> new ArrayList<>())
                    .add(formatDate(post));
        }

        //One block per user, so a user's dates never get split across two fields.
        List<String> blocks = new ArrayList<>();
        for(Map.Entry<String, List<String>> entry: userOutput.entrySet()) {
            StringBuilder block = new StringBuilder("👤 <@").append(entry.getKey()).append(">\n");
            for(String date: entry.getValue()) {
                block.append("🗓️ ").append(date).append("\n");
            }
            blocks.add(block.append("\n").toString());
        }

        return packIntoFields(blocks);
    }

    private static List<String> packIntoFields(List<String> blocks) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for(int i = 0; i < blocks.size(); i++) {
            String block = truncate(blocks.get(i));

            if(current.length() + block.length() > MAX_FIELD_LENGTH) {
                fields.add(current.toString());
                current = new StringBuilder();

                //Out of fields. Say how many users were left off rather than
                //building an embed Discord will reject.
                if(fields.size() == MAX_FIELDS_PER_SECTION) {
                    int last = MAX_FIELDS_PER_SECTION - 1;
                    fields.set(last, appendOverflowNote(fields.get(last), blocks.size() - i));
                    return fields;
                }
            }
            current.append(block);
        }

        fields.add(current.toString());
        return fields;
    }

    private static String truncate(String block) {
        return block.length() <= MAX_FIELD_LENGTH
                ? block
                : block.substring(0, MAX_FIELD_LENGTH - 1) + "…";
    }

    private static String appendOverflowNote(String field, int remaining) {
        String note = "…and " + remaining + " more\n";
        if(field.length() + note.length() > MAX_FIELD_LENGTH) {
            field = field.substring(0, MAX_FIELD_LENGTH - note.length());
        }
        return field + note;
    }
}
