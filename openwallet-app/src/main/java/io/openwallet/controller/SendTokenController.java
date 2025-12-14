package io.openwallet.controller;

import io.openwallet.MainApp;
import io.openwallet.model.TokenMeta;
import io.openwallet.service.DesktopNotificationService;
import io.openwallet.service.NetworkManager;
import io.openwallet.service.TokenRepository;
import io.openwallet.service.TokenService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.util.List;

public class SendTokenController {

    @FXML private ComboBox<TokenMeta> tokenSelector;
    @FXML private TextField recipientField;
    @FXML private TextField amountField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Button sendBtn;

    private MainApp mainApp;
    private NetworkManager networkManager;
    private TokenRepository tokenRepository;
    private TokenService tokenService;

    private String profileName;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        this.networkManager = mainApp.getNetworkManager();
        this.tokenRepository = mainApp.getTokenRepository();
        this.tokenService = mainApp.getTokenService();
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
        loadTokens();
    }

    private void loadTokens() {
        String networkId = networkManager.getActiveNetworkId();
        List<TokenMeta> tokens = tokenRepository.listByNetwork(networkId);
        tokenSelector.getItems().setAll(tokens);
        if (!tokens.isEmpty()) {
            tokenSelector.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void handleSend() {
        TokenMeta token = tokenSelector.getSelectionModel().getSelectedItem();
        String recipient = recipientField.getText() != null ? recipientField.getText().trim() : "";
        String amountStr = amountField.getText() != null ? amountField.getText().trim() : "";
        String password = passwordField.getText();

        if (token == null) {
            statusLabel.setText("Select a token.");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }
        if (recipient.isEmpty() || amountStr.isEmpty() || password == null || password.isEmpty()) {
            statusLabel.setText("Please fill all fields.");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }
        if (!recipient.startsWith("0x") || recipient.length() != 42) {
            statusLabel.setText("Invalid Ethereum address.");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                statusLabel.setText("Amount must be > 0.");
                statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                return;
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid amount.");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        sendBtn.setDisable(true);
        statusLabel.setText("Sending token transfer...");
        statusLabel.setStyle("-fx-text-fill: #3498db;");

        final BigDecimal finalAmount = amount;
        Platform.runLater(() -> {});

        new Thread(() -> {
            try {
                String txHash = tokenService.sendToken(profileName, password, token, recipient, finalAmount);
                Platform.runLater(() -> {
                    statusLabel.setText("Submitted! Tx Hash: " + txHash);
                    statusLabel.setStyle("-fx-text-fill: #2ecc71;");
                    sendBtn.setDisable(false);
                    if (mainApp.getNotificationService() != null) {
                        String symbol = token.getSymbol() != null ? token.getSymbol() : "TOKEN";
                        mainApp.getNotificationService().info(
                                "Token transfer sent",
                                finalAmount.toPlainString() + " " + symbol + " → " + shortenAddress(recipient) + "\n" + txHash
                        );
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + ex.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                    sendBtn.setDisable(false);
                    if (mainApp.getNotificationService() != null) {
                        mainApp.getNotificationService().error(
                                "Token transfer failed",
                                ex.getMessage()
                        );
                    }
                });
            }
        }).start();
    }

    private String shortenAddress(String addr) {
        if (addr == null) return "";
        String a = addr.trim();
        if (a.length() <= 12) return a;
        return a.substring(0, 6) + "…" + a.substring(a.length() - 4);
    }

    @FXML
    private void handleBack() {
        mainApp.showTokens(profileName);
    }
}
