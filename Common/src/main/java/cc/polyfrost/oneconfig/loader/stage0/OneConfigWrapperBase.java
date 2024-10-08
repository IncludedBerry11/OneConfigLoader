package cc.polyfrost.oneconfig.loader.stage0;

import cc.polyfrost.oneconfig.loader.stage0.ssl.SSLStore;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * This abstract base class is used to implement the OneConfig wrapper. It allows
 * mod developers to implement their own additional logic along with loading OneConfig.
 * 
 * For example, if you wanted to load an additional library or add your own SSLStore
 * logic, this allows for that to be done along with loading OneConfig normally.
 * 
 * @see cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker
 */
public abstract class OneConfigWrapperBase {
    // TODO: move this into another common class, maybe polyui if we migrate loader ui to it.
    public static final Color GRAY_900 = new Color(13, 14, 15, 255);
    public static final Color GRAY_700 = new Color(34, 35, 38);
    public static final Color PRIMARY_500 = new Color(26, 103, 255);
    public static final Color PRIMARY_500_80 = new Color(26, 103, 204);
    public static final Color WHITE_80 = new Color(255, 255, 255, 204);
    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    public OneConfigWrapperBase() {
    }

    protected static class LoaderInfo {
        public String stageLoading;
        public String mcVersion;
        public String modLoader;
        public String platformName;

        public LoaderInfo(String stageLoading, String mcVersion, String modLoader, String platformName) {
            this.stageLoading = stageLoading;
            this.mcVersion = mcVersion;
            this.modLoader = modLoader;
            this.platformName = platformName;
        }
    }

    protected boolean shouldSSLStore() {
        return false;
    }

    protected abstract LoaderInfo provideLoaderInfo();

    protected abstract File provideFile(LoaderInfo loaderInfo);

    protected boolean shouldUpdate() {
        return true;
    }

    protected static class JsonInfo {
        public String checksum;
        public String downloadUrl;
        public boolean success;

        public JsonInfo(String checksum, String downloadUrl, boolean success) {
            this.checksum = checksum;
            this.downloadUrl = downloadUrl;
            this.success = success;
        }
    }

    protected abstract JsonInfo provideJsonInfo(JsonObject object, LoaderInfo loaderInfo);

    protected abstract void addToClasspath(File file);

    protected abstract boolean isInitialized(File file);

    protected abstract boolean getNextInstance();

    protected void downloadFile(String url, File location) {
        try {
            URLConnection con = new URL(url).openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36 OneConfigWrapper");
            con.setConnectTimeout(15000);
            con.setReadTimeout(15000);
            InputStream in = con.getInputStream();
            Files.copy(in, location.toPath(), StandardCopyOption.REPLACE_EXISTING);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static JsonElement getRequest(String site) {
        return null;
    }

    protected String getChecksum(File file) {
        try (FileInputStream in = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[1024];
            int count;
            while ((count = in.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
            }
            byte[] digested = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digested) {
                sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    protected void showErrorScreen() {
        showErrorScreen("OneConfig has failed to download!", "OneConfig has failed to download!\n" +
                "Please join our discord server at https://polyfrost.org/discord\n" +
                "for support, or try again later.");
    }

    protected void showErrorScreen(String title, String message) {
        try {
            Icon icon = null;
            try {
                icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/assets/oneconfig-loader/oneconfig-icon.png")));
            } catch (Exception ignored) {

            }
            UIManager.put("OptionPane.background", GRAY_900);
            UIManager.put("Panel.background", GRAY_900);
            UIManager.put("OptionPane.messageForeground", WHITE_80);
            UIManager.put("Button.background", PRIMARY_500);
            UIManager.put("Button.select", PRIMARY_500_80);
            UIManager.put("Button.foreground", WHITE_80);
            UIManager.put("Button.focus", TRANSPARENT);
            int response = JOptionPane.showOptionDialog(
                    null,
                    message,
                    title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE, icon,
                    new Object[]{"Join Discord", "Close"}, "Join Discord"
            );
            if (response == 0) {
                if (!browse(new URI("https://polyfrost.org/discord"))) {
                    System.out.println("Failed to open browser.");
                }
            }
        } catch (Exception ignored) {
        } finally {
            try {
                Method exit = Class.forName("java.lang.Shutdown").getDeclaredMethod("exit", int.class);
                exit.setAccessible(true);
                exit.invoke(null, 1);
            } catch (Exception e) {
                System.exit(1);
            }
        }
    }

    private boolean browse(URI uri) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(uri);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void addSSLStore() {
        try {
            SSLStore sslStore = new SSLStore();
            System.out.println("Attempting to load Polyfrost certificate.");
            sslStore = sslStore.load("/ssl/polyfrost.der");
            SSLContext context = sslStore.finish();
            SSLContext.setDefault(context);
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to add Polyfrost certificate to keystore.");
        }
    }
}
