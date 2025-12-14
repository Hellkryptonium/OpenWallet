package io.openwallet.controller;

import io.openwallet.MainApp;
import io.openwallet.model.BridgeLink;
import io.openwallet.model.NetworkConfig;
import io.openwallet.model.TokenMeta;
import io.openwallet.service.NetworkManager;
import io.openwallet.service.TokenRepository;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import java.util.List;

public class BridgeController {

    @FXML private ComboBox<NetworkConfig> targetNetworkSelector;
    @FXML private ComboBox<TokenMeta> tokenSelector;
    @FXML private javafx.scene.control.TextField amountField;
    @FXML private ComboBox<BridgeLink> bridgeSelector;
    @FXML private Label statusLabel;

    private MainApp mainApp;
    private NetworkManager networkManager;
    private TokenRepository tokenRepository;

    private String profileName;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        this.networkManager = mainApp.getNetworkManager();
        this.tokenRepository = mainApp.getTokenRepository();

        List<NetworkConfig> networks = networkManager.getNetworks();
        targetNetworkSelector.getItems().setAll(networks);
        if (!networks.isEmpty()) {
            targetNetworkSelector.getSelectionModel().selectFirst();
        }

        targetNetworkSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> refreshBridgeLinks());

        String networkId = networkManager.getActiveNetworkId();
        tokenSelector.getItems().setAll(tokenRepository.listByNetwork(networkId));
        if (!tokenSelector.getItems().isEmpty()) {
            tokenSelector.getSelectionModel().selectFirst();
        }

        NetworkConfig active = networkManager.getActiveNetwork();
        if (active != null && active.getId() != null) {
            // Prefer active network as default selection.
            for (NetworkConfig n : networks) {
                if (active.getId().equals(n.getId())) {
                    targetNetworkSelector.getSelectionModel().select(n);
                    break;
                }
            }
        }

        refreshBridgeLinks();
    }

    private void refreshBridgeLinks() {
        NetworkConfig selected = targetNetworkSelector.getSelectionModel().getSelectedItem();
        List<BridgeLink> links = selected != null ? selected.getBridgeLinks() : List.of();
        bridgeSelector.getItems().setAll(links);
        if (!links.isEmpty()) {
            bridgeSelector.getSelectionModel().selectFirst();
        }
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    @FXML
    private void handleOpenBridge() {
        BridgeLink selected = bridgeSelector.getSelectionModel().getSelectedItem();
        String url = (selected != null && selected.getUrl() != null) ? selected.getUrl().trim() : "";
        if (url.isEmpty()) {
            statusLabel.setText("No bridge URL configured for this network.");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        // Simple UX: open a configured external bridge URL.
        mainApp.openExternalUrl(url);
        statusLabel.setText("Opened bridge in browser.");
        statusLabel.setStyle("-fx-text-fill: #2ecc71;");
    }

    @FXML
    private void handleBack() {
        mainApp.showDashboard(profileName);
    }
}
