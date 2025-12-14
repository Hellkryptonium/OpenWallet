package io.openwallet.controller;

import io.openwallet.MainApp;
import io.openwallet.model.TokenMeta;
import io.openwallet.service.NetworkManager;
import io.openwallet.service.TokenRepository;
import io.openwallet.service.TokenService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class AddTokenController {

    @FXML private TextField contractField;
    @FXML private Label statusLabel;
    @FXML private Button addBtn;

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
    }

    @FXML
    private void handleAdd() {
        String address = contractField.getText() != null ? contractField.getText().trim() : "";
        if (!address.startsWith("0x") || address.length() != 42) {
            statusLabel.setText("Invalid contract address.");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        addBtn.setDisable(true);
        statusLabel.setText("Reading token metadata...");
        statusLabel.setStyle("-fx-text-fill: #3498db;");

        String networkId = networkManager.getActiveNetworkId();
        tokenService.fetchTokenMeta(networkId, address)
                .thenAccept(meta -> Platform.runLater(() -> {
                    tokenRepository.add(meta);
                    statusLabel.setText("Added: " + safe(meta.getSymbol()) + " (" + safe(meta.getName()) + ")");
                    statusLabel.setStyle("-fx-text-fill: #2ecc71;");
                    addBtn.setDisable(false);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("Error: " + ex.getMessage());
                        statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                        addBtn.setDisable(false);
                    });
                    return null;
                });
    }

    @FXML
    private void handleBack() {
        mainApp.showTokens(profileName);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
