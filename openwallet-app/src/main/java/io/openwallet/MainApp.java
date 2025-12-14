package io.openwallet;

import io.openwallet.controller.CreateWalletController;
import io.openwallet.controller.DashboardController;
import io.openwallet.controller.StartupController;
import io.openwallet.db.MySQLDatabaseConnection;
import io.openwallet.db.MySQLTransactionLogDao;
import io.openwallet.db.MySQLWalletDao;
import io.openwallet.db.TransactionLogDao;
import io.openwallet.db.WalletDao;
import io.openwallet.service.JsonTokenRepository;
import io.openwallet.service.DesktopNotificationService;
import io.openwallet.service.NetworkManager;
import io.openwallet.service.NftService;
import io.openwallet.service.PriceService;
import io.openwallet.service.TokenRepository;
import io.openwallet.service.TokenService;
import io.openwallet.service.WalletService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Main JavaFX entry point.
 */
public class MainApp extends Application {

    private Stage primaryStage;
    private WalletService walletService;
    private WalletDao walletDao;
    private TransactionLogDao transactionLogDao;
    private NetworkManager networkManager;
    private TokenRepository tokenRepository;
    private TokenService tokenService;
    private PriceService priceService;
    private NftService nftService;
    private DesktopNotificationService notificationService;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("OpenWallet");

        // Set application icon
        try {
            InputStream iconStream = getClass().getResourceAsStream("/io/openwallet/images/icon.png");
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception e) {
            System.out.println("Could not load application icon.");
        }

        // Initialize Services
        this.networkManager = NetworkManager.getInstance();
        this.tokenRepository = new JsonTokenRepository();
        MySQLDatabaseConnection dbConnection = MySQLDatabaseConnection.getInstance();
        this.walletDao = new MySQLWalletDao(dbConnection);
        this.transactionLogDao = new MySQLTransactionLogDao(dbConnection);
        this.walletService = new WalletService(walletDao, transactionLogDao, networkManager);
        this.tokenService = new TokenService(networkManager, walletService, transactionLogDao);
        this.priceService = new PriceService(networkManager);
        this.nftService = new NftService(networkManager);
        this.notificationService = new DesktopNotificationService("OpenWallet", "/io/openwallet/images/icon.png");

        if (walletDao.getAll().isEmpty()) {
            showStartup();
        } else {
            showLogin();
        }
    }

    @Override
    public void stop() {
        try {
            if (notificationService != null) {
                notificationService.shutdown();
            }
        } catch (Exception ignored) {
        }
    }

    private void applyStyles(Scene scene) {
        URL cssUrl = MainApp.class.getResource("/io/openwallet/view/styles.css");
        if (cssUrl == null) {
            System.out.println("Error loading styles.css: resource not found at /io/openwallet/view/styles.css");
            return;
        }

        String css = cssUrl.toExternalForm();
        scene.getStylesheets().setAll(css);
    }

    public void showLogin() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/Login.fxml"));
            Pane root = loader.load();

            io.openwallet.controller.LoginController controller = loader.getController();
            controller.setMainApp(this);

            Scene scene = new Scene(root, 600, 400);
            applyStyles(scene);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showImportWallet() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/ImportWallet.fxml"));
            Pane root = loader.load();

            io.openwallet.controller.ImportWalletController controller = loader.getController();
            controller.setMainApp(this);

            Scene scene = new Scene(root, 600, 500);
            applyStyles(scene);
            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showStartup() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/Startup.fxml"));
            Pane root = loader.load();

            StartupController controller = loader.getController();
            controller.setMainApp(this);

            Scene scene = new Scene(root, 600, 400);
            applyStyles(scene);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showCreateWallet() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/CreateWallet.fxml"));
            Pane root = loader.load();

            CreateWalletController controller = loader.getController();
            controller.setMainApp(this);

            Scene scene = new Scene(root, 600, 500);
            applyStyles(scene);
            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showDashboard(String profileName) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/Dashboard.fxml"));
            Pane root = loader.load();

            DashboardController controller = loader.getController();
            controller.setMainApp(this);
            controller.loadWallet(profileName);

            Scene scene = new Scene(root, 800, 600);
            applyStyles(scene);
            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showTokens(String profileName) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/Tokens.fxml"));
            Pane root = loader.load();

            io.openwallet.controller.TokensController controller = loader.getController();
            controller.setMainApp(this);
            controller.setProfileName(profileName);

            Scene scene = new Scene(root, 800, 600);
            applyStyles(scene);
            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showAddToken(String profileName) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/AddToken.fxml"));
            Pane root = loader.load();

            io.openwallet.controller.AddTokenController controller = loader.getController();
            controller.setMainApp(this);
            controller.setProfileName(profileName);

            Scene scene = new Scene(root, 600, 400);
            applyStyles(scene);
            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showSendToken(String profileName) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/SendToken.fxml"));
            Pane root = loader.load();

            io.openwallet.controller.SendTokenController controller = loader.getController();
            controller.setMainApp(this);
            controller.setProfileName(profileName);

            Scene scene = new Scene(root, 600, 520);
            applyStyles(scene);
            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showBridge(String profileName) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/Bridge.fxml"));
            Pane root = loader.load();

            io.openwallet.controller.BridgeController controller = loader.getController();
            controller.setMainApp(this);
            controller.setProfileName(profileName);

            Scene scene = new Scene(root, 700, 520);
            applyStyles(scene);
            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showNftGallery(String profileName) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/NftGallery.fxml"));
            Pane root = loader.load();

            io.openwallet.controller.NftGalleryController controller = loader.getController();
            controller.setMainApp(this);
            controller.setProfileName(profileName);

            Scene scene = new Scene(root, 800, 600);
            applyStyles(scene);
            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showSettings(String profileName) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/Settings.fxml"));
            Pane root = loader.load();

            io.openwallet.controller.SettingsController controller = loader.getController();
            controller.setMainApp(this);
            controller.setProfileName(profileName);

            Scene scene = new Scene(root, 600, 420);
            applyStyles(scene);
            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showSendTransaction(String profileName) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/SendTransaction.fxml"));
            Pane root = loader.load();

            io.openwallet.controller.SendTransactionController controller = loader.getController();
            controller.setMainApp(this);
            controller.setProfileName(profileName);

            Scene scene = new Scene(root, 600, 500);
            applyStyles(scene);
            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showReceive(String profileName, String address) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/Receive.fxml"));
            Pane root = loader.load();

            io.openwallet.controller.ReceiveController controller = loader.getController();
            controller.setMainApp(this);
            controller.setWalletData(profileName, address);

            Scene scene = new Scene(root, 600, 500);
            applyStyles(scene);
            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public WalletService getWalletService() {
        return walletService;
    }

    public NetworkManager getNetworkManager() {
        return networkManager;
    }

    public TokenRepository getTokenRepository() {
        return tokenRepository;
    }

    public TokenService getTokenService() {
        return tokenService;
    }

    public PriceService getPriceService() {
        return priceService;
    }

    public NftService getNftService() {
        return nftService;
    }

    public DesktopNotificationService getNotificationService() {
        return notificationService;
    }

    public void openExternalUrl(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        getHostServices().showDocument(url);
    }

    public WalletDao getWalletDao() {
        return walletDao;
    }

    public TransactionLogDao getTransactionLogDao() {
        return transactionLogDao;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
