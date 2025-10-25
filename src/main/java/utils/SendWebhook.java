package utils;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import config.ReadConfig;

// SendWebhook
//
// Void SendWebhook.sendMessage  ; Send a text message to the configured Discord webhook
// Inputs : Message content to send
//
// SendWebhook.shutdown  ; Close and clean up the webhook client connection
public class SendWebhook {
    private final WebhookClient client;

    public SendWebhook() {
        ReadConfig config = ReadConfig.getInstance(); // Get singleton config
        String webhookUrl = config.getGeneral().getDiscordWebhook();

        // Build the webhook
        WebhookClientBuilder builder = new WebhookClientBuilder(webhookUrl);

        builder.setThreadFactory((job) -> {
            Thread thread = new Thread(job);
            thread.setName("WebhookThread");
            thread.setDaemon(true);
            return thread;
        });

        builder.setWait(true); // Wait for responses
        this.client = builder.build();
    }
    // Send message
    public void sendMessage(String content) {
        WebhookMessageBuilder message = new WebhookMessageBuilder();
        message.setContent(content);
        client.send(message.build());
    }

    // Shutdown
    public void shutdown() {
        client.close();
    }
}
