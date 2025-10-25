package utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;

// ImageToVideo
//
// Bool ImageToVideo.convert  ; Convert a single Image object into a short MP4 video using FFmpeg
// Inputs : Output file path (without extension), Image object to convert
public class ImageToVideo {
    public static boolean convert(String path, Image image) {
        try {
            // Save Image to a temporary PNG
            File tmpImage = new File(path + ".png");
            ImageIO.write((java.awt.image.BufferedImage) image, "png", tmpImage);

            // Determine FFmpeg path based on current OS
            String os = System.getProperty("os.name").toLowerCase();
            String ffmpegPath;
            if (os.contains("win")) {
                ffmpegPath = "./ffmpeg/win/bin/ffmpeg.exe"; // bundled binary
            } else {
                ffmpegPath = "ffmpeg"; // system-installed on Linux
            }

            // Build FFmpeg command
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-y",               // overwrite output
                    "-loop", "1",
                    "-i", tmpImage.getAbsolutePath(),
                    "-c:v", "libx264",
                    "-preset", "veryfast",
                    "-crf", "28",
                    "-pix_fmt", "yuv420p",
                    "-t", "15",
                    path + ".mp4"
            );


            // Redirect streams to avoid hanging, but discard output
            pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
            pb.redirectError(ProcessBuilder.Redirect.PIPE);

            Process process = pb.start();

            // Consume stdout in background thread
            new Thread(() -> {
                try (var is = process.getInputStream()) {
                    while (is.read() != -1) {}
                } catch (Exception ignored) {}
            }).start();

            // Consume stderr in background thread
            new Thread(() -> {
                try (var is = process.getErrorStream()) {
                    while (is.read() != -1) {}
                } catch (Exception ignored) {}
            }).start();

            // Wait for FFmpeg to finish
            process.waitFor();

            // Delete temporary image
            tmpImage.delete();

            return true;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();

            return false;
        }
    }
}
