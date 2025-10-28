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
    public static boolean convert(String path, Image image, String audioPath) {
        try {
            // Save Image to a temporary PNG
            File tmpImage = new File(path + ".png");
            ImageIO.write((java.awt.image.BufferedImage) image, "png", tmpImage);

            // Determine FFmpeg path based on current OS
            String os = System.getProperty("os.name").toLowerCase();
            String ffmpegPath = os.contains("win") ? "./ffmpeg/win/bin/ffmpeg.exe" : "ffmpeg";

            // Build FFmpeg command
            ProcessBuilder pb;

            if (audioPath != null && !audioPath.isEmpty()) {
                pb = new ProcessBuilder(
                        ffmpegPath,
                        "-y",
                        "-loop", "1",
                        "-i", tmpImage.getAbsolutePath(),
                        "-i", audioPath,
                        "-c:v", "libx264",
                        "-preset", "veryfast",
                        "-crf", "28",
                        "-pix_fmt", "yuv420p",
                        "-shortest",             // stop when shortest input (audio or video) ends
                        path + ".mp4"
                );
            } else {
                pb = new ProcessBuilder(
                        ffmpegPath,
                        "-y",
                        "-loop", "1",
                        "-i", tmpImage.getAbsolutePath(),
                        "-c:v", "libx264",
                        "-preset", "veryfast",
                        "-crf", "28",
                        "-pix_fmt", "yuv420p",
                        "-t", "15",
                        path + ".mp4"
                );
            }

            // Redirect and run
            pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
            pb.redirectError(ProcessBuilder.Redirect.PIPE);

            Process process = pb.start();

            new Thread(() -> {
                try (var is = process.getInputStream()) {
                    while (is.read() != -1) {}
                } catch (Exception ignored) {}
            }).start();

            new Thread(() -> {
                try (var is = process.getErrorStream()) {
                    while (is.read() != -1) {}
                } catch (Exception ignored) {}
            }).start();

            process.waitFor();
            tmpImage.delete();
            return true;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

}
