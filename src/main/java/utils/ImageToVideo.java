package utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

// ImageToVideo
//
// Bool ImageToVideo.convert  ; Convert a single Image object into a short MP4 video using FFmpeg
// Inputs : Output file path (without extension), Image object to convert
public class ImageToVideo {
    public static boolean convert(String path, Image image, String audioPath) {
        try {
            int width = image.getWidth(null);
            int height = image.getHeight(null);

            if (width <= 0 || height <= 0) {
                System.err.println("Invalid image: width or height <= 0");
                return false;
            }

            // Ensure width/height divisible by 2 (lib264 requirement)
            if (width % 2 != 0) width++;
            if (height % 2 != 0) height++;

            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = bufferedImage.createGraphics();
            g2d.setColor(Color.BLACK); // background fill if needed
            g2d.fillRect(0, 0, width, height);
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();


            // Fill extra pixels with transparent background
            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, width, height);
            g2d.setComposite(AlphaComposite.SrcOver);

            // Draw original image at top-left
            g2d.drawImage(image, 0, 0, image.getWidth(null), image.getHeight(null), null);
            g2d.dispose();

            // Write temp PNG
            File tmpImage = new File(path + ".png");
            ImageIO.write(bufferedImage, "png", tmpImage);

            // Force flush to disk
            bufferedImage.flush();


            File ffmpegFile = new File("./ffmpeg/win/bin/ffmpeg.exe");

            String ffmpegPath;
            if (ffmpegFile.exists() && System.getProperty("os.name").toLowerCase().contains("win")) {
                ffmpegPath = ffmpegFile.getAbsolutePath();
            } else {
                ffmpegPath = "ffmpeg"; // fallback
            }


            // Build FFmpeg command
            ProcessBuilder pb;

            if (audioPath != null && !audioPath.isEmpty()) {
                pb = new ProcessBuilder(
                        ffmpegPath,
                        "-y",
                        "-loop", "1", // endless loop
                        "-i", tmpImage.getAbsolutePath(),
                        "-i", audioPath,
                        "-c:v", "libx264",
                        "-loglevel", "error", // only errors
                        "-preset", "fast", // compression speed
                        "-crf", "28",
                        "-r", "10", // fps
                        "-pix_fmt", "yuv420p",
                        "-shortest", // stop when shortest input (audio or video, but input is endless image) ends
                        "-t", "15", // stop at 15s
                        path + ".mp4"
                );
            } else {
                pb = new ProcessBuilder(
                        ffmpegPath,
                        "-y",
                        "-loop", "1",
                        "-loglevel", "error", //only errors
                        "-i", tmpImage.getAbsolutePath(),
                        "-c:v", "libx264",
                        "-preset", "fast",
                        "-r", "10", // fps
                        "-crf", "28",
                        "-pix_fmt", "yuv420p",
                        "-t", "15",
                        path + ".mp4"
                );
            }

            // Redirect and run
            pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
            pb.redirectError(ProcessBuilder.Redirect.PIPE);

            // Start process
            Process process = pb.start();

// Consume stdout
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[FFmpeg stdout] " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

// Consume stderr
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.err.println("[FFmpeg stderr] " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // Wait for FFmpeg to finish
            int exitCode = process.waitFor();

            // Delete temp image
            tmpImage.delete();

            if (exitCode != 0) {
                System.err.println("FFmpeg exited with error code " + exitCode);
                return false;
            }

            return true;


        } catch (IOException | InterruptedException e) {
            e.printStackTrace();

            return false;
        }
    }

}
