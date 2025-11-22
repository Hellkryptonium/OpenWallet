package io.openwallet.controller;

import io.openwallet.MainApp;
import io.openwallet.service.WalletService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class CreateWalletController {

    @FXML private TextArea mnemonicArea;
    @FXML private TextField walletNameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;

    private MainApp mainApp;
    private WalletService walletService;
    private String currentMnemonic;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        this.walletService = mainApp.getWalletService();
        handleGenerate(); // Auto-generate on load
    }

    @FXML
    private void handleGenerate() {
        currentMnemonic = walletService.generateMnemonic();
        mnemonicArea.setText(currentMnemonic);
    }

    @FXML
    private void handleSaveWallet() {
        String name = walletNameField.getText();
        String pass = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (name.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("Please fill all fields.");
            return;
        }

        if (!pass.equals(confirm)) {
            statusLabel.setText("Passwords do not match.");
            return;
        }

        try {
            walletService.importWallet(name, currentMnemonic, pass);
            statusLabel.setText("Wallet created successfully!");
            // Navigate to Dashboard
            mainApp.showDashboard(name);
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        if (mainApp.getWalletDao().getAll().isEmpty()) {
            mainApp.showStartup();
        } else {
            mainApp.showLogin();
        }
    }
}
