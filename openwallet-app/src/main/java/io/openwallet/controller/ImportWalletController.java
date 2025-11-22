package io.openwallet.controller;

import io.openwallet.MainApp;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ImportWalletController {

    private MainApp mainApp;

    @FXML
    private TextArea mnemonicField;

    @FXML
    private TextField profileNameField;

    @FXML
    private PasswordField passwordField;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void handleImport() {
        String mnemonic = mnemonicField.getText().trim();
        String profileName = profileNameField.getText().trim();
        String password = passwordField.getText();

        if (mnemonic.isEmpty() || profileName.isEmpty() || password.isEmpty()) {
            showAlert("Error", "All fields are required.");
            return;
        }

        try {
            mainApp.getWalletService().importWallet(profileName, mnemonic, password);
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Wallet imported successfully!");
            alert.showAndWait();

            mainApp.showDashboard(profileName);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Import Failed", "Could not import wallet: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        // If there are wallets, go to Login, else go to Startup
        if (!mainApp.getWalletDao().getAll().isEmpty()) {
            mainApp.showLogin();
        } else {
            mainApp.showStartup();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
