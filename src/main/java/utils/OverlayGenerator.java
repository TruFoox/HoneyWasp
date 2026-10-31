package utils;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

public class OverlayGenerator { // Generates fake twitter post overlay. This was hell to make I lowkey should have just used AI lmao
    public static boolean Generate(String path, Image image) throws IOException { // User needs to supply logo, image, and service name
        String accountName = "HoneyWasp";
        String caption = "Oh my god this is so funny i think im gonna fucking die";
        // Calculate how tall to make the fake twitter post
        int maxWidth = 1350; // Max dimensions need to be calibrated better these are just values that seem to work
        int maxHeight = 2000;

        double scale = Math.min((double) maxWidth / image.getWidth(null), (double) maxHeight / image.getHeight(null));

        int resizedWidth = (int) (image.getWidth(null) * scale);
        int resizedHeight = (int) (image.getHeight(null) * scale);

        // Create empty image
        BufferedImage TwitterOverlayImage = new BufferedImage(1500, Math.max(1500, 500 + resizedHeight), BufferedImage.TYPE_3BYTE_BGR);

        Graphics2D g = TwitterOverlayImage.createGraphics();
        g.setColor(Color.WHITE); // Color blank image white (black by default)
        g.fillRect(0, 0, 1500,  Math.max(1500, 500 + resizedHeight));

        // Load and resize logo
        BufferedImage logo = ImageIO.read(new File("logo.png"));

        int size = 150;
        int offset = 75;

        // Draw black circle (couldn't think of a better way to force alpha to be black)
        g.setColor(Color.BLACK);
        g.fillOval(offset, offset, size, size);

        // Draw logo over circle
        g.setClip(new Ellipse2D.Double(offset, offset, size, size));  // Mask image w/ a circle

        // Draw logo inside the circle, preserving alpha transparency
        g.drawImage(logo, offset, offset, size, size, null);

        // Remove mask to circle for everything after logo
        g.setClip(null);

        g.setFont(new Font("Arial", Font.PLAIN, 45));
        g.setColor(Color.BLACK);
        g.drawString(accountName, 240, 135);
        FontMetrics metrics = g.getFontMetrics();
        int textWidth = metrics.stringWidth(accountName); // Used for placement of checkmark

        BufferedImage checkmark = ImageIO.read(new File("checkmark.png"));

        g.drawImage(checkmark, 245 + textWidth, 98, 45, 45, null); // Draw checkmark next to account name

        g.setColor(Color.GRAY); // Draw @ under name
        g.setFont(new Font("Arial", Font.PLAIN, 30));
        g.drawString("@" + accountName.toLowerCase().replace(" ", ""), 240, 180);

        // Draw caption
        String[] words = caption.split(" ");
        StringBuilder line = new StringBuilder();
        g.setFont(new Font("Arial", Font.PLAIN, 60));
        g.setColor(Color.BLACK);
        metrics = g.getFontMetrics();

        int captionY = 350; // Start at 350 from top
        for (String word : words) { // doesnt automatically wrap text so i need to wrap it manually
            String testLine = line.isEmpty() ? word : line + " " + word;

            if (metrics.stringWidth(testLine) > 1350 && !line.isEmpty()) {
                g.drawString(line.toString(), 75, captionY);
                captionY += metrics.getHeight() + 2;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(testLine);
            }
        }
        if (!line.isEmpty()) { // Draw last line
            g.drawString(line.toString(), 75, captionY);
        }

        // Draw meme centered w/ rounded corners
        g.setClip(new RoundRectangle2D.Double(75 + maxWidth/2 - resizedWidth/2, captionY + 50, resizedWidth, resizedHeight, 40, 40));
        g.drawImage(image, 75 + maxWidth/2 - resizedWidth/2, captionY + 50, resizedWidth, resizedHeight, null);
        g.setClip(null);

        g.dispose();

        ImageIO.write(TwitterOverlayImage, "png", new File(path + "\\temp.png"));

        return true;
    }
}
