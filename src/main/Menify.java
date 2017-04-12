package main;

import com.oracle.tools.packager.Log;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * This shit was thrown together way too fast for you to even think about judging the code
 */

public class Menify extends Application {

    private static final int EOF = -1;
    private static final String ADD_TO_STARTUP =
            "tell application \"System Events\" \n" +
            "  make new login item at end of login items with properties" +
                    " {name:\"Menify\", path:(POSIX path of (path to application \"Menify\")), hidden:true}\n" +
            "end tell";

    @Override
    public void start(Stage primaryStage) throws Exception {

        Platform.setImplicitExit(false);
        primaryStage.hide();

        TrayIcon trayIcon;

        if (SystemTray.isSupported()) {

            SystemTray tray = SystemTray.getSystemTray();

            BufferedImage image = new BufferedImage(300, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setFont(getHelvetiva(13));
            setRenderingHints(g2d);
            g2d.drawString("No music playing", 160, 13);

            ActionListener quitListener = e -> System.exit(0);
            ActionListener startUpAddListener = e -> eval(ADD_TO_STARTUP);

            PopupMenu popup = new PopupMenu();

            MenuItem addToStartupItem = new MenuItem("Start Menify with login");
            MenuItem quitItem = new MenuItem("Quit");

            addToStartupItem.addActionListener(startUpAddListener);
            quitItem.addActionListener(quitListener);
            popup.add(addToStartupItem);
            popup.add(quitItem);

            trayIcon = new TrayIcon(image, "", popup);

            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(quitListener);

            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                e.printStackTrace();
                System.exit(0);
            }


            Runnable refreshPlayingText = () -> {

                BufferedImage updatedImage = new BufferedImage(300, 16, BufferedImage.TYPE_INT_ARGB);
                Graphics2D updatedG2d = updatedImage.createGraphics();

                String spotifyMetaDataScript = "tell application \"System Events\"\n" +
                        "  set myList to (name of every process)\n" +
                        "end tell\n" +
                        "if myList contains \"Spotify\" then\n" +
                        "  tell application \"Spotify\"\n" +
                        "    if player state is stopped then\n" +
                        "      set output to \"Stopped\"\n" +
                        "    else\n" +
                        "      set trackname to name of current track\n" +
                        "      set artistname to artist of current track\n" +
                        "      set albumname to album of current track\n" +
                        "      if player state is playing then\n" +
                        "        set output to artistname & \" - \" & trackname & \"\"\n" +
                        "      else if player state is paused then\n" +
                        "        set output to artistname & \" - \" & trackname & \"\"\n" +
                        "      end if\n" +
                        "    end if\n" +
                        "  end tell\n" +
                        "else\n" +
                        "  set output to \"Spotify not running\"\n" +
                        "end if";

                String spotifyMetaData = eval(spotifyMetaDataScript);

                if(spotifyMetaData != null && spotifyMetaData.isEmpty()) {
                    spotifyMetaData = "No music playing";
                }

                setRenderingHints(updatedG2d);
                updatedG2d.setFont(getHelvetiva(13));
                FontMetrics fm = updatedG2d.getFontMetrics();

                drawTextWrapped(spotifyMetaData, fm, updatedG2d);

                trayIcon.setImage(updatedImage);
            };


            //update every 0.5s
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            executor.scheduleAtFixedRate(refreshPlayingText, 0, 500, TimeUnit.MILLISECONDS);

        } else {
            Log.debug("closing because system tray is not supported");
            System.exit(0);
        }


    }

    private Font getHelvetiva(int size) {
        return new Font("Helvetiva Neue", Font.PLAIN, size);
    }

    private void drawTextWrapped(String text, FontMetrics textMetrics, Graphics2D g2d) {


        int startX = ((300 - textMetrics.stringWidth(text)) / 2); //center text
        int startY = 13;

        g2d.setFont(getHelvetiva(g2d.getFont().getSize()));

        if(text.length() > 100) {
            startX = ((300 - textMetrics.stringWidth("Title too long")) / 2); //recenter based on "too long"-text
            g2d.drawString("Title too long", startX, startY);

        } else {

            while (textMetrics.stringWidth(text) > 295) {

                g2d.setFont(getHelvetiva(g2d.getFont().getSize() - 1));

                float newSize;
                Font newFont;

                //I'd rather not do proper math on a weekend, thank you very much
                if (textMetrics.stringWidth(text) > 420) {
                    newSize = (float) (g2d.getFont().getSize() - 1.4);
                    newFont = g2d.getFont().deriveFont(newSize);
                    startX += 47;
                }
                else if (textMetrics.stringWidth(text) > 400) {
                    newSize = (float) (g2d.getFont().getSize() - 1.4);
                    newFont = g2d.getFont().deriveFont(newSize);
                    startX += 43;
                } else if (textMetrics.stringWidth(text) > 380) {
                    newSize = (float) (g2d.getFont().getSize() - 1.6);
                    newFont = g2d.getFont().deriveFont(newSize);
                    startX += 55;
                } else if (textMetrics.stringWidth(text) > 360) {
                    newSize = (float) (g2d.getFont().getSize() - 1.5);
                    newFont = g2d.getFont().deriveFont(newSize);
                    startX += 40;
                } else if (textMetrics.stringWidth(text) > 345) {
                    newSize = (float) (g2d.getFont().getSize() - 1.4);
                    newFont = g2d.getFont().deriveFont(newSize);
                    startX += 35;
                } else if (textMetrics.stringWidth(text) > 330) {
                    newSize = (float) (g2d.getFont().getSize() - 0.75);
                    newFont = g2d.getFont().deriveFont(newSize);
                    startX += 26;
                } else {
                    newSize = (float) (g2d.getFont().getSize() - 0.5);
                    newFont = g2d.getFont().deriveFont(newSize);
                    startX += 15;
                }

                g2d.setFont(newFont);
                textMetrics = g2d.getFontMetrics(); //get new modified metrics

            }


            g2d.drawString(text, startX, startY);
        }

    }

    private void setRenderingHints(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
    }
    private static String eval(String code) {
        Runtime runtime = Runtime.getRuntime();
        String[] args = { "osascript", "-e", code };

        try {
            Process process = runtime.exec(args);
            process.waitFor();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            InputStream is = process.getInputStream();
            copyLarge(is, baos, new byte[4096]);
            return baos.toString().trim();

        } catch (IOException | InterruptedException e) {
            Log.debug(e);
            return null;
        }
    }

    private static long copyLarge(InputStream input, OutputStream output, byte[] buffer) throws IOException {

        long count = 0;
        int n;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }


    public static void main(String[] args) {
        launch(args);
    }
}
