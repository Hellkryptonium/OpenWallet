package io.openwallet.controller;

import io.openwallet.MainApp;
import io.openwallet.model.NetworkConfig;
import io.openwallet.service.NetworkManager;
import io.openwallet.service.WalletService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

public class SettingsController {

    @FXML private ComboBox<NetworkConfig> networkSelector;
    @FXML private Label currentRpcLabel;
    @FXML private Label statusLabel;

    private MainApp mainApp;
    private NetworkManager networkManager;
    private WalletService walletService;

    private String profileName;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        this.networkManager = mainApp.getNetworkManager();
        this.walletService = mainApp.getWalletService();

        List<NetworkConfig> networks = networkManager.getNetworks();
        networkSelector.getItems().setAll(networks);

        NetworkConfig active = networkManager.getActiveNetwork();
        if (active != null) {
            networkSelector.getSelectionModel().select(active);
        } else if (!networks.isEmpty()) {
            networkSelector.getSelectionModel().selectFirst();
        }

        refreshRpcLabel();

        networkSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> refreshRpcLabel());
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    private void refreshRpcLabel() {
        if (currentRpcLabel != null) {
            currentRpcLabel.setText(networkManager.getRpcUrl());
        }
    }

    @FXML
    private void handleSave() {
        NetworkConfig selected = networkSelector.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a network.");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        networkManager.setActiveNetwork(selected.getId());
        statusLabel.setText("Saved. Network is now: " + selected.getName());
        statusLabel.setStyle("-fx-text-fill: #2ecc71;");
        refreshRpcLabel();
    }

    @FXML
    private void handleBack() {
        mainApp.showDashboard(profileName);
    }

    @FXML
    private void handleShowPrivateKey() {
        if (profileName == null || profileName.isBlank()) {
            statusLabel.setText("No active wallet profile.");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        Dialog<String> passwordDialog = new Dialog<>();
        passwordDialog.setTitle("Reveal Private Key");
        passwordDialog.setHeaderText("Enter your wallet password to decrypt the private key.");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Wallet password");
        VBox content = new VBox(10);
        content.getChildren().add(passwordField);
        passwordDialog.getDialogPane().setContent(content);

        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType reveal = new ButtonType("Reveal", ButtonBar.ButtonData.OK_DONE);
        passwordDialog.getDialogPane().getButtonTypes().addAll(cancel, reveal);
        passwordDialog.setResultConverter(btn -> btn == reveal ? passwordField.getText() : null);

        Optional<String> passwordOpt = passwordDialog.showAndWait();
        if (passwordOpt.isEmpty()) {
            return;
        }

        String password = passwordOpt.get();
        if (password == null || password.isBlank()) {
            statusLabel.setText("Password is required.");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        statusLabel.setText("Decrypting private key...");
        statusLabel.setStyle("-fx-text-fill: #3498db;");

        new Thread(() -> {
            try {
                String rawKey = walletService.getPrivateKey(profileName, password);
                String displayKey = normalizePrivateKeyForDisplay(rawKey);

                Platform.runLater(() -> showPrivateKeyDialog(displayKey));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                });
            }
        }).start();
    }

    private void showPrivateKeyDialog(String privateKey) {
        statusLabel.setText("");

        TextArea area = new TextArea(privateKey);
        area.setEditable(false);
        area.setWrapText(true);
        area.setStyle("-fx-font-family: 'Monospaced';");

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Private Key");
        alert.setHeaderText("Keep this private. Anyone with it can control your funds.");
        alert.getDialogPane().setContent(area);

        ButtonType copy = new ButtonType("Copy", ButtonBar.ButtonData.LEFT);
        ButtonType close = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(copy, close);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == copy) {
            ClipboardContent clip = new ClipboardContent();
            clip.putString(privateKey);
            Clipboard.getSystemClipboard().setContent(clip);
            statusLabel.setText("Private key copied to clipboard.");
            statusLabel.setStyle("-fx-text-fill: #2ecc71;");
        }
    }

    private String normalizePrivateKeyForDisplay(String rawKey) {
        if (rawKey == null) {
            return "";
        }

        String key = rawKey.trim();
        if (key.startsWith("0x") || key.startsWith("0X")) {
            key = key.substring(2);
        }

        // Left-pad to 32 bytes (64 hex chars) for readability/interop.
        if (key.length() < 64) {
            key = "0".repeat(64 - key.length()) + key;
        }

        return "0x" + key.toLowerCase();
    }
}
