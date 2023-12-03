package com.janboerman.f2pstarassist.discord;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Please provide a token!");
            System.exit(1);
        }

        final String token = args[0];
        final DiscordClient client = DiscordClient.create(token);

        final long applicationId = client.getApplicationId().block();

        //command definition
        ApplicationCommandRequest greetCmdRequest = ApplicationCommandRequest.builder()
                .name("greet")
                .description("Greets you")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("name")
                        .description("Your name")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build()
                ).build();

        //register command
        Disposable disposable = client.getApplicationService()
                .createGlobalApplicationCommand(applicationId, greetCmdRequest)
                .subscribe();

        //register all functionalities of the bot
        final Mono<Void> clientComputation = client.withGateway((GatewayDiscordClient gateway) -> {
            //log when we log in (https://docs.discord4j.com/basic-bot-tutorial)
            Mono<Void> logLogin = gateway.on(ReadyEvent.class, event -> Mono.fromRunnable(() -> {
                final User self = event.getSelf();
                System.out.printf("Logged in as %s#%s%n", self.getUsername(), self.getDiscriminator());     //TODO actually use a logger?! :D
            })).then();

            //reply pong! to !ping messages (https://docs.discord4j.com/basic-bot-tutorial)
            Mono<Void> handlePingCommand = gateway.on(MessageCreateEvent.class, event -> {
                Message message = event.getMessage();
                if (message.getContent().equalsIgnoreCase("!ping")) {
                    return message.getChannel().flatMap(channel -> channel.createMessage("pong!"));         //important, 'return' the action!
                } else {
                    return Mono.empty();
                }
            }).then();

            //chat input command ("slash command") (//https://docs.discord4j.com/interactions/application-commands)
            Mono<Void> handleGreetCommand = gateway.on(ChatInputInteractionEvent.class, event -> {
                if ("greet".equals(event.getCommandName())) {
                    ApplicationCommandInteractionOption nameOption = event.getOption("name").get();  //can safely perform Optional#get since the option is required!
                    ApplicationCommandInteractionOptionValue nameValue = nameOption.getValue().get();       //get the provided value
                    String name = nameValue.asString();
                    return event.reply("Hello, " + name + "!");                                     //important: 'return' the action!
                } else {
                    return Mono.empty();
                }
            }).then();

            //combine actions
            return logLogin.and(handlePingCommand).and(handleGreetCommand);
        });

        clientComputation.block();
    }

}
