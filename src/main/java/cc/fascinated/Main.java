package cc.fascinated;

import cc.fascinated.suggestion.Suggestion;
import cc.fascinated.suggestion.SuggestionManager;
import lombok.Getter;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

/**
 * Project: TestBot-DevRoom
 * Created by Fascinated#4735 on 01/09/2021
 */
public class Main extends ListenerAdapter {

    @Getter public static JDA jda;

    @SneakyThrows
    public static void main(String[] args) {
        jda = JDABuilder.createDefault(BotConstants.getTOKEN())
            .enableIntents(GatewayIntent.GUILD_EMOJIS)
            .enableCache(CacheFlag.EMOTE)
            .setActivity(Activity.listening("suggestions"))
                .addEventListeners(new Main())
            .build();


        /*
         * I HATE slash commands
         * they are so shit, and they take 4789347329847892 years to work
         */
        jda.upsertCommand(new CommandData("suggest",
                "Create a suggestion"
                ).addOption(OptionType.STRING, "suggestion", "The suggestion")
        ).queue();
        jda.upsertCommand(new CommandData("checksuggestion",
                "If the suggestion has more than a 75% like/dislike ratio it will be accepted"
                ).addOption(OptionType.STRING, "id", "The suggestions id")
        ).queue();
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        System.out.println("Ready kek");
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        System.out.println(event.getMessage().getContentRaw());
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (event.getUser() == null)
            return;
        if (event.getUser().isBot())
            return;

        Suggestion suggestion = SuggestionManager.suggestions.get(event.getMessageId());
        if (suggestion != null) {
            MessageHistory history = event.getChannel().getHistoryAround(event.getMessageId(), 1).complete();
            Message message = history.getMessageById(event.getMessageId());

            List<MessageReaction> reactions = message != null ? message.getReactions() : null;
            if (reactions.size() < 2)
                return;
            boolean isInTick = false, isInCross = false;

            for (MessageReaction reaction : reactions) {
                if (reaction.getReactionEmote().getEmoji().equalsIgnoreCase("✅")) {
                    if (reaction.retrieveUsers().complete().contains(event.getUser())) {
                        isInTick = true;
                    }
                }
                if (reaction.getReactionEmote().getEmoji().equalsIgnoreCase("❌")) {
                    if (reaction.retrieveUsers().complete().contains(event.getUser())) {
                        isInCross = true;
                    }
                }
            }

            if (isInTick && isInCross) {
                event.getReaction().removeReaction(event.getUser()).queue();
                event.getUser().openPrivateChannel().queue(privateChannel -> {
                    privateChannel.sendMessage("You cannot react twice to a suggestion").queue();
                });
            }
        }
    }

    @Override
    public void onSlashCommand(SlashCommandEvent event) {
        TextChannel channel = event.getTextChannel();
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (member == null)
            return;
        User user = member.getUser();

        System.out.println(event.getName());

        if (event.getName().equals("suggest")) {
            List<OptionMapping> options = event.getOptions();
            if (options.size() < 1) {
                event.reply("You need to provide a suggestion.").setEphemeral(true).queue();
                return;
            }
            String suggestion = options.get(0).getAsString();

            channel.sendMessageEmbeds(
                  new EmbedBuilder()
                      .setColor(Color.CYAN)
                      .setAuthor("Suggestion", null, user.getAvatarUrl())
                      .setDescription(suggestion + "\n\nSuggested by: " + member.getEffectiveName() + "#" + user.getDiscriminator())
                      .build()
            ).queue(msg -> {
                SuggestionManager.createSuggestion(msg.getId(),
                        new Suggestion(
                                msg.getChannel().getId(),
                                suggestion
                        )
                );
                msg.addReaction("✅").queue();
                msg.addReaction("❌").queue();
            });
            event.reply("Created Suggestion!").setEphemeral(true).queue();
        }

        if (event.getName().equals("checksuggestion")) {
            List<OptionMapping> options = event.getOptions();
            if (options.size() < 1) {
                event.reply("You need to provide a suggestion id.").setEphemeral(true).queue();
                return;
            }
            String suggestionId = options.get(0).getAsString();

            MessageHistory history = channel.getHistoryAround(suggestionId, 1).complete();
            Message message = history.getMessageById(suggestionId);
            if (message == null && !SuggestionManager.suggestions.containsKey(suggestionId)) {
                event.reply("Unknown suggestion.").setEphemeral(true).queue();
                return;
            }

            double outcome = SuggestionManager.endSuggestion(suggestionId);
            Suggestion suggestion = SuggestionManager.suggestions.get(suggestionId);

            if ((message != null ? message.getEmbeds().size() : 0) < 1)
                return;
            if (outcome < 75) {
                message.editMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.red)
                        .setAuthor("Suggestion", null, user.getAvatarUrl())
                        .setDescription(suggestion.getSuggestion() + "\n\nStatus: Denied\nSuggested by: " + member.getEffectiveName() + "#" + user.getDiscriminator())
                        .build()
                ).queue();
                event.reply("Denied suggestion.").setEphemeral(true).queue();
            } else {
                message.editMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.green)
                        .setAuthor("Suggestion", null, user.getAvatarUrl())
                        .setDescription(suggestion.getSuggestion() + "\n\nStatus: Accepted\nSuggested by: " + member.getEffectiveName() + "#" + user.getDiscriminator())
                        .build()
                ).queue();
                event.reply("Accepted suggestion.").setEphemeral(true).queue();
            }
        }
    }
}
