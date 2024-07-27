package org.polyfrost.oneconfig.loader.utils;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.loader.ILoader;
import org.polyfrost.oneconfig.loader.IMetaHolder;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;

/**
 * @author xtrm
 */
@Log4j2
public class ErrorHandler {
    private static final String DISCORD_URL = "https://polyfrost.org/discord";

    private static final String TITLE = "OneConfig Loader (%s) - Error";
    private static final boolean CENTER_BODY = false;
    private static final int ICON_SIZE = 64;

    private static final Color GRAY_900 = new Color(13, 14, 15, 255);
    private static final Color GRAY_700 = new Color(34, 35, 38);
    private static final Color PRIMARY_500 = new Color(26, 103, 255);
    private static final Color PRIMARY_500_80 = new Color(26, 103, 204);
    private static final Color WHITE_80 = new Color(255, 255, 255, 204);
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    private ErrorHandler() {
        throw new IllegalStateException("This class cannot be instantiated.");
    }

    public static void displayError(IMetaHolder meta, String message) {
        displayError(meta, message, 1);
    }

    public static void displayError(IMetaHolder meta, String message, int errorCode) {
        setSwingStyle();

        String loaderName = meta.getName();
        String loaderVersion = meta.getVersion();
        String formattedTitle = String.format(TITLE, loaderName + "/" + loaderVersion);

        String messageBody = "An unexpected error occured.\n" + "\n" + "%s\n" + "\n" + "We recommend you join our Discord Server\nfor support, or try again later.";
        String formattedMessage = String.format(messageBody, message, DISCORD_URL);
        String maybeCenter = CENTER_BODY ? "<center>" : "";
        formattedMessage = "<html><body>" + maybeCenter + formattedMessage.replaceAll("\n", "<br/>");

        Runnable exitCallback = () -> exit(errorCode);
        try {
            showFrameDialog(formattedTitle, formattedMessage, exitCallback);
        } catch (HeadlessException exception) {
            log.error(formattedTitle);
            log.error(message);
        }
        exitCallback.run();
    }

    private static void showFrameDialog(String title, String message, Runnable exitCallback) {
        JFrame frame = new JFrame() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
            }
        };
        frame.setLocationRelativeTo(null);
        frame.setAlwaysOnTop(true);
        frame.setResizable(false);
        frame.setUndecorated(true);
        frame.setVisible(true);

        Icon icon = null;
        try {
            URL url = ILoader.class.getResource("/assets/oneconfig-loader/icon.png");
            if (url != null) {
                icon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(url).getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH), "Polyfrost Logo");
            }
        } catch (Exception ignored) {
        }

        JButton discord = getDiscordButton(exitCallback, frame);
        JButton close = new JButton("Close");
        close.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                exitCallback.run();
            }
        });

        JOptionPane pane = new JOptionPane(message, JOptionPane.ERROR_MESSAGE, JOptionPane.DEFAULT_OPTION, icon, new JButton[]{discord, close}, discord);
        pane.setInitialValue(discord);
        pane.setComponentOrientation(frame.getComponentOrientation());
        for (Component component : pane.getComponents()) {
            if (component instanceof Container) {
                Container container = (Container) component;

                if (container.getName() == null) continue;
                if (container.getName().equals("OptionPane.buttonArea")) {
                    container.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 4));
                }
            }
        }

        JDialog dialog = pane.createDialog(frame, title);
        dialog.setVisible(true);
        dialog.dispose();
    }

    @NotNull
    private static JButton getDiscordButton(Runnable exitCallback, JFrame frame) {
        JButton discord = new JButton("Join our Discord");
        discord.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                try {
                    Desktop.getDesktop().browse(new URI(DISCORD_URL));
                } catch (Exception e) {
                    log.error("Failed to open Discord URL: " + DISCORD_URL, e);
                    int res = JOptionPane.showConfirmDialog(frame, "Failed to open Discord URL: " + DISCORD_URL + ".\nDo you want to copy it to your clipboard?", "Error", JOptionPane.YES_NO_OPTION);
                    if (res == JOptionPane.YES_OPTION) {
                        try {
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(DISCORD_URL), null);
                        } catch (Exception e1) {
                            log.error("Failed to copy Discord URL in user's clipboard.", e1);
                            JOptionPane.showMessageDialog(frame, "Failed to copy to clipboard.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
                exitCallback.run();
            }
        });
        return discord;
    }

    private static void setSwingStyle() {
        try {
            // Try and default to Metal UI
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Throwable e) {
            LogManager.getLogger(ErrorHandler.class).error("Failed to set Metal UI.", e);
        }

        UIManager.put("OptionPane.background", GRAY_900);
        UIManager.put("Panel.background", GRAY_900);
        UIManager.put("OptionPane.messageForeground", WHITE_80);
        UIManager.put("Button.background", PRIMARY_500);
        UIManager.put("Button.select", PRIMARY_500_80);
        UIManager.put("Button.foreground", WHITE_80);
        UIManager.put("Button.focus", TRANSPARENT);
    }

    /**
     * Exits the JVM with the given error code, escaping any SecurityManager
     * in place (looking at you Froge).
     *
     * @param errorCode the error code to exit with
     */
    public static void exit(int errorCode) {
        try {
            Class<?> clazz = Class.forName("java.lang.Shutdown");
            Method m_exit = clazz.getDeclaredMethod("exit", int.class);
            m_exit.setAccessible(true);
            m_exit.invoke(null, errorCode);
        } catch (Throwable e) {
            try {
                Runtime.getRuntime().exit(errorCode);
            } catch (Throwable e1) {
                if (getJavaVersion() <= 19) { // beware of class removal
                    exitPrivileged(errorCode);
                } else {
                    // this'll exit alright, but it's not pretty
                    throw new RuntimeException("Exitting the JVM, no errors to report here.", e1);
                }
            }
        }
    }

    @SuppressWarnings({"removal", "RedundantSuppression"})
    private static void exitPrivileged(int errorCode) {
        java.security.AccessController.doPrivileged((java.security.PrivilegedAction<Void>) () -> {
            Runtime.getRuntime().exit(errorCode);
            return null;
        });
    }

    private static int getJavaVersion() {
        String version = System.getProperty("java.version", "1.6.0");
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        return Integer.parseInt(version);
    }
}
