package io.openwallet.controller;

import io.openwallet.MainApp;
import io.openwallet.exception.InsufficientFundsException;
import io.openwallet.exception.OpenWalletException;
import io.openwallet.service.DesktopNotificationService;
import io.openwallet.service.WalletService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public class SendTransactionController {

    @FXML private TextField recipientField;
    @FXML private TextField amountField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Button sendBtn;

    private MainApp mainApp;
    private WalletService walletService;
    private String currentProfileName;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        this.walletService = mainApp.getWalletService();
    }

    public void setProfileName(String profileName) {
        this.currentProfileName = profileName;
    }

    @FXML
    private void handleSend() {
        String recipient = recipientField.getText().trim();
        String amountStr = amountField.getText().trim();
        String password = passwordField.getText();

        if (recipient.isEmpty() || amountStr.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please fill all fields.");
            return;
        }

        if (!recipient.startsWith("0x") || recipient.length() != 42) {
            statusLabel.setText("Invalid Ethereum address.");
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                statusLabel.setText("Amount must be greater than 0.");
                return;
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid amount.");
            return;
        }

        statusLabel.setText("Sending transaction... Please wait.");
        statusLabel.setStyle("-fx-text-fill: #3498db;");
        sendBtn.setDisable(true);

        CompletableFuture.runAsync(() -> {
            try {
                String txHash = walletService.sendTransaction(currentProfileName, password, recipient, amount);
                Platform.runLater(() -> {
                    statusLabel.setText("Success! Tx Hash: " + txHash);
                    statusLabel.setStyle("-fx-text-fill: #2ecc71;");
                    sendBtn.setDisable(false);
                    if (mainApp.getNotificationService() != null) {
                        mainApp.getNotificationService().info(
                                "Transaction sent",
                                amount.toPlainString() + " ETH → " + shortenAddress(recipient) + "\n" + txHash
                        );
                    }
                    // Optionally clear fields or navigate back
                });
            } catch (InsufficientFundsException e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: Insufficient funds.");
                    statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                    sendBtn.setDisable(false);
                    if (mainApp.getNotificationService() != null) {
                        mainApp.getNotificationService().error(
                                "Transaction failed",
                                "Insufficient funds."
                        );
                    }
                });
            } catch (OpenWalletException e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                    sendBtn.setDisable(false);
                    if (mainApp.getNotificationService() != null) {
                        mainApp.getNotificationService().error(
                                "Transaction failed",
                                e.getMessage()
                        );
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                    sendBtn.setDisable(false);
                    e.printStackTrace();
                    if (mainApp.getNotificationService() != null) {
                        mainApp.getNotificationService().error(
                                "Transaction failed",
                                e.getMessage()
                        );
                    }
                });
            }
        });
    }

    private String shortenAddress(String addr) {
        if (addr == null) return "";
        String a = addr.trim();
        if (a.length() <= 12) return a;
        return a.substring(0, 6) + "…" + a.substring(a.length() - 4);
    }

    @FXML
    private void handleCancel() {
        mainApp.showDashboard(currentProfileName);
    }
}
