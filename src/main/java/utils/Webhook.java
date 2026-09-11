package utils;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import main.HoneyWasp;

public class Webhook {
    private final WebhookClient client;

    public Webhook() { // Constructor
        String webhookUrl = HoneyWasp.config.General().getDiscordWebhook();

        // Build the webhook
        WebhookClientBuilder builder = new WebhookClientBuilder(webhookUrl);

        builder.setThreadFactory((job) -> {
            Thread thread = new Thread(job);
            thread.setName("WebhookThread");
            thread.setDaemon(true);
            return thread;
        });

        builder.setWait(false); // Wait for responses
        client = builder.build();
    }

    // Send message
    public void sendMessage(String content) {
        Output.debugPrint(null, "Attempting to send webhook");
        WebhookMessageBuilder message = new WebhookMessageBuilder();
        message.setContent(content);
        client.send(message.build());
    }
}
