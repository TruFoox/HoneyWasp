package utils;

import services.Services;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

public class OverlayGenerator {

    public static boolean Generate(Services service) throws IOException {
        String accountName = "HoneyWasp";

        Output.debugPrint(null, "Locating FFmpeg");
        File ffmpegFile = new File("./ffmpeg/win/bin/ffmpeg.exe");

        String ffmpegPath;
        if (ffmpegFile.exists() && System.getProperty("os.name").toLowerCase().contains("win")) {
            ffmpegPath = ffmpegFile.getAbsolutePath();
        } else {
            Output.debugPrint(null, "FFmpeg not found. Must be added to system path if not already");
            ffmpegPath = "ffmpeg";
        }

        BufferedImage image;

        Output.debugPrint(null, "Loading media");
        // Load image or extract first video frame
        if (service.VIDEO_MODE) {

            File frameFile = Path.of("cache", service.name.toLowerCase(), "video_frame.png").toFile();

            Files.createDirectories(frameFile.toPath().getParent());

            Process process = new ProcessBuilder(
                    ffmpegPath,
                    "-y",
                    "-loglevel", "error",
                    "-i", service.fileDir,
                    "-map", "0:v:0",
                    "-frames:v", "1",
                    frameFile.getAbsolutePath()
            ).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            try {
                if (process.waitFor() != 0) {
                    throw new IOException("Failed to extract video frame.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }

            image = ImageIO.read(frameFile);

        } else {
            image = ImageIO.read(new File(service.fileDir));
        }

        if (image == null) {
            throw new IOException("Could not load media.");
        } else {
            Output.debugPrint(null, "Loaded media");
        }

        // Calculate how tall to make the fake twitter post
        int maxWidth = 1350;
        int maxHeight = 2000;

        double scale = Math.min((double) maxWidth / image.getWidth(), (double) maxHeight / image.getHeight());

        int resizedWidth = (int) (image.getWidth() * scale);
        int resizedHeight = (int) (image.getHeight() * scale);

        int overlayX = 75 + maxWidth / 2 - resizedWidth / 2;
        int overlayY;

        // Create empty image
        BufferedImage TwitterOverlayImage = new BufferedImage(1500, Math.max(1500, 500 + resizedHeight), BufferedImage.TYPE_3BYTE_BGR);

        Graphics2D g = TwitterOverlayImage.createGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 1500, TwitterOverlayImage.getHeight());

        // Load and resize logo
        BufferedImage logo;

        try {
            try {
                logo = ImageIO.read(new File("logo.png"));
            } catch (Exception _) {
                logo = ImageIO.read(new File("icon.png"));
            }
        } catch (Exception _) {
            Output.webhookPrint(service,
                    "You didnt supply an icon.png or logo.png.\n" +
                            "\tSupply an image or disable generate_twitter_container under [General_Settings]. Quitting...");
            return false;
        }

        int size = 150;
        int offset = 75;

        // Draw black circle
        g.setColor(Color.BLACK);
        g.fillOval(offset, offset, size, size);

        // Draw logo over circle
        g.setClip(new Ellipse2D.Double(offset, offset, size, size));
        g.drawImage(logo, offset, offset, size, size, null);
        g.setClip(null);

        g.setFont(new Font("Arial", Font.PLAIN, 45));
        g.setColor(Color.BLACK);
        g.drawString(accountName, 240, 135);

        FontMetrics metrics = g.getFontMetrics();
        int textWidth = metrics.stringWidth(accountName);

        BufferedImage checkmark = ImageIO.read(new File("checkmark.png"));

        g.drawImage(checkmark, 245 + textWidth, 98, 45, 45, null);

        g.setColor(Color.GRAY);
        g.setFont(new Font("Arial", Font.PLAIN, 30));
        g.drawString("@" + accountName.toLowerCase().replace(" ", ""), 240, 180);

        // Draw caption
        String[] words = service.caption.split(" ");
        StringBuilder line = new StringBuilder();

        g.setFont(new Font("Arial", Font.PLAIN, 60));
        g.setColor(Color.BLACK);
        metrics = g.getFontMetrics();

        int captionY = 350;
        for (String word : words) {
            String testLine = line.isEmpty() ? word : line + " " + word;

            if (metrics.stringWidth(testLine) > 1350 && !line.isEmpty()) {
                g.drawString(line.toString(), offset, captionY);
                captionY += metrics.getHeight() + 2;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(testLine);
            }
        }

        if (!line.isEmpty()) {
            g.drawString(line.toString(), offset, captionY);
        }

        // Draw meme centered with rounded corners
        overlayY = captionY + 50;

        g.setClip(new RoundRectangle2D.Double(overlayX, overlayY, resizedWidth, resizedHeight, 40, 40));

        if (!service.VIDEO_MODE) {
            g.drawImage(image, overlayX, overlayY, resizedWidth, resizedHeight, null);
        }

        g.setClip(null);
        g.dispose();

        // Write overlay to file
        File out = Path.of("cache", service.name.toLowerCase(), "temp.png").toFile();

        Files.createDirectories(out.toPath().getParent());

        ImageIO.write(TwitterOverlayImage, "png", out);

        // If video, composite it over the generated overlay (I just smashed together parts of ImageToVideo for this)
        if (service.VIDEO_MODE) {
            File videoOut = Path.of("cache", service.name.toLowerCase(), "temp_overlay.mp4").toFile();

            // Build FFmpeg command
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-y",
                    "-i", service.fileDir,
                    "-i", out.getAbsolutePath(),
                    "-filter_complex",
                    "[0:v]scale=" + resizedWidth + ":" + resizedHeight + "[video];" +
                            "[1:v][video]overlay=" + overlayX + ":" + overlayY,
                    "-map", "0:a?",
                    "-c:v", "libx264",
                    "-loglevel", "error",
                    "-preset", "fast",
                    "-crf", "28",
                    "-r", "24",
                    "-pix_fmt", "yuv420p",
                    "-c:a", "aac",
                    videoOut.getAbsolutePath()
            );

            pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
            pb.redirectError(ProcessBuilder.Redirect.PIPE);

            Output.debugPrint(null, "Running FFmpeg");

            Process process = pb.start();

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line2;
                    while ((line2 = reader.readLine()) != null) {
                        System.out.println("[FFmpeg stdout] " + line2);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line2;
                    while ((line2 = reader.readLine()) != null) {
                        System.err.println("[FFmpeg stderr] " + line2);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            service.fileDir = videoOut.getPath();

            int exitCode;
            try {
                exitCode = process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }

            if (exitCode != 0) {
                throw new IOException("FFmpeg exited with error code " + exitCode);
            }
        }

        Output.debugPrint(null, "Finished generating overlay");
        return true;
    }
}