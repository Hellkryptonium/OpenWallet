package io.openwallet.controller;

import io.openwallet.MainApp;
import io.openwallet.model.WalletProfile;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.util.StringConverter;

import java.util.List;

public class LoginController {

    private MainApp mainApp;

    @FXML
    private ComboBox<WalletProfile> profileSelector;

    @FXML
    private PasswordField passwordField;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        loadProfiles();
    }

    private void loadProfiles() {
        List<WalletProfile> profiles = mainApp.getWalletDao().getAll();
        profileSelector.getItems().addAll(profiles);

        profileSelector.setConverter(new StringConverter<WalletProfile>() {
            @Override
            public String toString(WalletProfile object) {
                return object.getProfileName();
            }

            @Override
            public WalletProfile fromString(String string) {
                return profileSelector.getItems().stream()
                        .filter(p -> p.getProfileName().equals(string))
                        .findFirst().orElse(null);
            }
        });

        if (!profiles.isEmpty()) {
            profileSelector.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void handleLogin() {
        WalletProfile selectedProfile = profileSelector.getSelectionModel().getSelectedItem();
        String password = passwordField.getText();

        if (selectedProfile == null) {
            showAlert("Error", "Please select a wallet profile.");
            return;
        }

        if (password == null || password.isEmpty()) {
            showAlert("Error", "Please enter your password.");
            return;
        }

        try {
            // Attempt to decrypt private key to verify password
            mainApp.getWalletService().getPrivateKey(selectedProfile.getProfileName(), password);
            
            // If successful, load dashboard
            mainApp.showDashboard(selectedProfile.getProfileName());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Login Failed", "Invalid password or corrupted wallet data.");
        }
    }

    @FXML
    private void handleCreateNew() {
        mainApp.showCreateWallet();
    }

    @FXML
    private void handleImport() {
        mainApp.showImportWallet();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
