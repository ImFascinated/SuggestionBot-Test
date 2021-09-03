package cc.fascinated.suggestion;

import cc.fascinated.Main;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Project: TestBot-DevRoom
 * Created by Fascinated#4735 on 02/09/2021
 */
public class SuggestionManager {

    public static Map<String, Suggestion> suggestions = new HashMap<>();

    public static void createSuggestion(String messageId, Suggestion suggestion) {
        suggestions.put(messageId, suggestion);
    }

    public static double endSuggestion(String messageId) {
        if (!suggestions.containsKey(messageId)) return -1;
        Suggestion suggestion = suggestions.get(messageId);
        TextChannel channel = Main.jda.getTextChannelById(suggestion.getChannelId());
        if (channel == null) return -1;

        MessageHistory history = channel.getHistoryAround(messageId, 1).complete();
        Message suggestionMessage = history.getMessageById(messageId);
        if (suggestionMessage == null) return -1;
        List<MessageReaction> reactions = suggestionMessage.getReactions();
        if (reactions.size() < 2)
            return -1;
        double likes = 0;
        double dislikes = 0;
        for (MessageReaction reaction : reactions) {
            if (reaction.getReactionEmote().getEmoji().equalsIgnoreCase("✅")) {
                likes = reaction.getCount() - 1;
            }
            if (reaction.getReactionEmote().getEmoji().equalsIgnoreCase("❌")) {
                dislikes = reaction.getCount() - 1;
            }
        }
        if (likes == 0 && dislikes == 0)
            return 0;
        return (likes / (likes + dislikes)) * 100;
    }
}
