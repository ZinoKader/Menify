package main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Menify by
 * Zino Kader 2017
 * https://www.zinokader.se
 */

public class Menify extends Application {

    private static final int STATUS_BAR_WIDTH = 300;
    private static final int STATUS_BAR_HEIGHT = 16;
    private static final int HORIZONTAL_OFFSET = 20;
    private static final int VERTICAL_OFFSET = 13;

    private static final int DEFAULT_FONT_SIZE = 13;
    private static final String DEFAULT_FONT = "San Francisco";

    private TrayIcon trayIcon;

    //start scrolling at these offsets
    private int scrollX = HORIZONTAL_OFFSET;
    private int scrollY = VERTICAL_OFFSET;
    private ScrollDirection scrollDirection = ScrollDirection.LEFT; //scroll to left as default

    private ScheduledExecutorService mainExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledExecutorService scrollingTextExecutor = Executors.newSingleThreadScheduledExecutor();
    private static boolean isScrolling = false;

    private static String spotifyMetaData;
    private static String previousSpotifyMetaData;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        Log.set(Log.LEVEL_DEBUG);

        //Let application live on even if no window is showing
        Platform.setImplicitExit(false);
        primaryStage.hide();

        if(SystemTray.isSupported()) {

            SystemTray tray = SystemTray.getSystemTray();

            BufferedImage placeholderImage = new BufferedImage(STATUS_BAR_WIDTH, STATUS_BAR_HEIGHT, BufferedImage.TYPE_INT_ARGB);

            ActionListener quitListener = action -> System.exit(0);
            ActionListener startupAddListener = action -> AppleScriptHelper.evalAppleScript(ScriptConstants.ADD_TO_STARTUP);
            ActionListener startupRemoveListener = action -> AppleScriptHelper.evalAppleScript(ScriptConstants.REMOVE_FROM_STARTUP);

            PopupMenu popupMenu = new PopupMenu();

            MenuItem addToStartupItem = new MenuItem("Start Menify on login");
            MenuItem removeFromStartupItem = new MenuItem("Remove Menify from startup");
            MenuItem quitItem = new MenuItem("Quit");

            addToStartupItem.addActionListener(startupAddListener);
            removeFromStartupItem.addActionListener(startupRemoveListener);
            quitItem.addActionListener(quitListener);

            popupMenu.add(addToStartupItem);
            popupMenu.add(removeFromStartupItem);
            popupMenu.add(quitItem);

            trayIcon = new TrayIcon(placeholderImage, "", popupMenu);

            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                Log.debug("Failed to add application to tray");
                e.printStackTrace();
                System.exit(0);
            }

            final Runnable refreshPlayingText = () -> {

                spotifyMetaData = AppleScriptHelper.evalAppleScript(ScriptConstants.SPOTIFY_META_DATA_SCRIPT);
     
                if(spotifyMetaData != null) {
                    if(spotifyMetaData.equals(previousSpotifyMetaData)) {
                        Log.debug("-- same song --");
                    } else {
                        if(isScrolling) { //if we're already scrolling, destroy old and recreate new executor
                            scrollingTextExecutor.shutdownNow();
                            try {
                                scrollingTextExecutor.awaitTermination(1, TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                                Log.debug("Did not terminate old scrollingTextExecutor in time");
                            }
                            scrollingTextExecutor = Executors.newSingleThreadScheduledExecutor();
                        }
                        previousSpotifyMetaData = spotifyMetaData;
                        drawText(spotifyMetaData);
                        Log.debug("|| song changed ||");
                    }
                }
            };

            //update every second
            mainExecutor.scheduleAtFixedRate(refreshPlayingText, 0, 1000, TimeUnit.MILLISECONDS);
        }
        else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("System Tray not supported");
            alert.setHeaderText("Could not start Menify");
            alert.setContentText("System Tray applications are not supported in your operating system, sorry!");
            alert.showAndWait();
            Log.debug("Exiting: system tray is not supported");
            System.exit(0);
        }


    }

    private void drawText(String text) {

        BufferedImage staticImage = new BufferedImage(STATUS_BAR_WIDTH, STATUS_BAR_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        BufferedImage scrollingImage = new BufferedImage(STATUS_BAR_WIDTH, STATUS_BAR_HEIGHT, BufferedImage.TYPE_INT_ARGB);

        Graphics2D staticG2d = staticImage.createGraphics();
        setAntiAliasing(staticG2d);
        staticG2d.setFont(getDefaultFont(DEFAULT_FONT_SIZE));


        //check if the text is too long and needs to be displayed in a scrolling fashion
        if(textShouldScroll(text, staticG2d)) {

            scrollingTextExecutor.execute(() -> {

                isScrolling = true;
                resetScrollingPositions();

                while(spotifyMetaData.equals(previousSpotifyMetaData)) {

                    Graphics2D scrollingG2d = scrollingImage.createGraphics();
                    setAntiAliasing(scrollingG2d);
                    scrollingG2d.setFont(getDefaultFont(DEFAULT_FONT_SIZE));

                    int textWidth = scrollingG2d.getFontMetrics().stringWidth(text);

                    if(scrollX <= -textWidth + STATUS_BAR_WIDTH - HORIZONTAL_OFFSET) {
                        scrollDirection = ScrollDirection.RIGHT;
                    }
                    else if(scrollX >= HORIZONTAL_OFFSET) {
                        scrollDirection = ScrollDirection.LEFT;
                    }

                    switch(scrollDirection) {
                        case RIGHT:
                            scrollX += 1;
                            break;
                        case LEFT:
                            scrollX -= 1;
                            break;
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Log.debug("Closed down old scrolling thread.");
                        break;
                    }

                    scrollingG2d.setBackground(new Color(0, 0, 0, 0));
                    scrollingG2d.clearRect(0, 0, scrollingImage.getWidth(), scrollingImage.getHeight());
                    drawTextOutline(scrollingG2d, text, scrollX, scrollY);
                    scrollingG2d.drawString(text, scrollX, scrollY);
                    trayIcon.setImage(scrollingImage);

                    scrollingG2d.dispose();
                    scrollingImage.flush();
                }
            });


        }
        else { //text should fit, display it statically

            isScrolling = false;

            int startX = ((STATUS_BAR_WIDTH - staticG2d.getFontMetrics().stringWidth(text)) / 2); //center text
            int startY = VERTICAL_OFFSET;
            drawTextOutline(staticG2d, text, startX, startY);
            staticG2d.drawString(text, startX, startY);
            trayIcon.setImage(staticImage);
        }
        
        staticG2d.dispose();
        staticImage.flush();

    }

    private void drawTextOutline(Graphics2D g2d, String text, int x, int y) {
        g2d.setColor(Color.black);
        g2d.drawString(text, x - 1, y - 1);
        g2d.drawString(text, x - 1, y + 1);
        g2d.drawString(text, x + 1, y - 1);
        g2d.drawString(text, x + 1, y + 1);
        g2d.setColor(Color.WHITE);
    }

    private void setAntiAliasing(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
    }

    private Font getDefaultFont(int size) {
        return new Font(DEFAULT_FONT, Font.PLAIN, size);
    }

    private boolean textShouldScroll(String text, Graphics2D g2d) {
        return g2d.getFontMetrics().stringWidth(text) > STATUS_BAR_WIDTH - 10;
    }

    private void resetScrollingPositions() {
        //reset scrolling positions to default on song change
        scrollX = HORIZONTAL_OFFSET;
        scrollY = VERTICAL_OFFSET;
    }

}
