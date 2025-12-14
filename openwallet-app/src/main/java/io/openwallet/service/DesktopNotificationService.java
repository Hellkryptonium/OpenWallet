package io.openwallet.service;

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

/**
 * Best-effort desktop notifications.
 *
 * Uses {@link SystemTray} notifications when supported.
 * If the system tray isn't available (headless, restricted runtime), calls become no-ops.
 */
public class DesktopNotificationService {

    public enum Level {
        INFO,
        WARNING,
        ERROR
    }

    private final String appName;
    private final String trayIconResourcePath;

    private volatile boolean initAttempted;
    private volatile TrayIcon trayIcon;

    public DesktopNotificationService(String appName, String trayIconResourcePath) {
        this.appName = appName == null ? "OpenWallet" : appName;
        this.trayIconResourcePath = trayIconResourcePath;
    }

    public void notify(Level level, String title, String message) {
        String safeTitle = title == null || title.isBlank() ? appName : title;
        String safeMessage = message == null ? "" : message;

        ensureInitialized();
        TrayIcon icon = trayIcon;
        if (icon == null) {
            return;
        }

        TrayIcon.MessageType type = switch (level == null ? Level.INFO : level) {
            case ERROR -> TrayIcon.MessageType.ERROR;
            case WARNING -> TrayIcon.MessageType.WARNING;
            case INFO -> TrayIcon.MessageType.INFO;
        };

        EventQueue.invokeLater(() -> {
            try {
                icon.displayMessage(safeTitle, safeMessage, type);
            } catch (Exception ignored) {
            }
        });
    }

    public void info(String title, String message) {
        notify(Level.INFO, title, message);
    }

    public void warning(String title, String message) {
        notify(Level.WARNING, title, message);
    }

    public void error(String title, String message) {
        notify(Level.ERROR, title, message);
    }

    public void shutdown() {
        TrayIcon icon = trayIcon;
        if (icon == null) {
            return;
        }
        EventQueue.invokeLater(() -> {
            try {
                SystemTray.getSystemTray().remove(icon);
            } catch (Exception ignored) {
            }
        });
        trayIcon = null;
    }

    private void ensureInitialized() {
        if (trayIcon != null || initAttempted) {
            return;
        }

        synchronized (this) {
            if (trayIcon != null || initAttempted) {
                return;
            }
            initAttempted = true;

            try {
                if (!SystemTray.isSupported()) {
                    return;
                }

                Image img = loadTrayImage();
                TrayIcon icon = new TrayIcon(img, appName);
                icon.setImageAutoSize(true);
                SystemTray.getSystemTray().add(icon);
                trayIcon = icon;
            } catch (AWTException | RuntimeException ignored) {
                trayIcon = null;
            }
        }
    }

    private Image loadTrayImage() {
        // Use the app icon when available; else provide a tiny placeholder.
        if (trayIconResourcePath != null && !trayIconResourcePath.isBlank()) {
            try (InputStream in = DesktopNotificationService.class.getResourceAsStream(trayIconResourcePath)) {
                if (in != null) {
                    BufferedImage img = ImageIO.read(in);
                    if (img != null) {
                        return img;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    }
}
