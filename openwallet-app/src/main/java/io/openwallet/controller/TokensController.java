package io.openwallet.controller;

import io.openwallet.MainApp;
import io.openwallet.model.TokenMeta;
import io.openwallet.model.WalletProfile;
import io.openwallet.service.NetworkManager;
import io.openwallet.service.TokenRepository;
import io.openwallet.service.TokenService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class TokensController {

    @FXML private Label networkLabel;
    @FXML private Label walletAddressLabel;
    @FXML private TableView<TokenRow> tokenTable;

    private MainApp mainApp;
    private NetworkManager networkManager;
    private TokenRepository tokenRepository;
    private TokenService tokenService;

    private String profileName;
    private String walletAddress;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        this.networkManager = mainApp.getNetworkManager();
        this.tokenRepository = mainApp.getTokenRepository();
        this.tokenService = mainApp.getTokenService();
        setupTable();
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
        Optional<WalletProfile> profile = mainApp.getWalletDao().findByProfileName(profileName);
        this.walletAddress = profile.map(WalletProfile::getWalletAddress).orElse(null);
        if (walletAddressLabel != null) {
            walletAddressLabel.setText(walletAddress != null ? walletAddress : "");
        }
        if (networkLabel != null && networkManager.getActiveNetwork() != null) {
            networkLabel.setText(networkManager.getActiveNetwork().getName());
        }
        refresh();
    }

    private void setupTable() {
        TableColumn<TokenRow, String> symbolCol = new TableColumn<>("Token");
        symbolCol.setCellValueFactory(c -> c.getValue().symbolProperty());

        TableColumn<TokenRow, String> balanceCol = new TableColumn<>("Balance");
        balanceCol.setCellValueFactory(c -> c.getValue().balanceProperty());

        TableColumn<TokenRow, String> addressCol = new TableColumn<>("Contract");
        addressCol.setCellValueFactory(c -> c.getValue().addressProperty());

        tokenTable.getColumns().addAll(symbolCol, balanceCol, addressCol);
    }

    private void refresh() {
        if (walletAddress == null) {
            tokenTable.setItems(FXCollections.observableArrayList());
            return;
        }

        String networkId = networkManager.getActiveNetworkId();
        List<TokenMeta> tokens = tokenRepository.listByNetwork(networkId);

        ObservableList<TokenRow> rows = FXCollections.observableArrayList();
        for (TokenMeta token : tokens) {
            TokenRow row = new TokenRow(token);
            rows.add(row);
            row.setBalance("Loading...");

            tokenService.getTokenBalance(token, walletAddress)
                    .thenAccept(bal -> Platform.runLater(() -> row.setBalance(formatBalance(bal, token))))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> row.setBalance("Error"));
                        return null;
                    });
        }

        tokenTable.setItems(rows);
    }

    private String formatBalance(BigDecimal balance, TokenMeta token) {
        String symbol = token.getSymbol() != null ? token.getSymbol() : "TOKEN";
        return String.format("%.4f %s", balance, symbol);
    }

    @FXML
    private void handleAddToken() {
        mainApp.showAddToken(profileName);
    }

    @FXML
    private void handleSendToken() {
        mainApp.showSendToken(profileName);
    }

    @FXML
    private void handleBack() {
        mainApp.showDashboard(profileName);
    }

    public static class TokenRow {
        private final TokenMeta token;
        private final SimpleStringProperty symbol = new SimpleStringProperty();
        private final SimpleStringProperty balance = new SimpleStringProperty();
        private final SimpleStringProperty address = new SimpleStringProperty();

        public TokenRow(TokenMeta token) {
            this.token = token;
            symbol.set(token.getSymbol() != null ? token.getSymbol() : "Token");
            address.set(token.getAddress());
            balance.set("");
        }

        public TokenMeta getToken() {
            return token;
        }

        public SimpleStringProperty symbolProperty() {
            return symbol;
        }

        public SimpleStringProperty balanceProperty() {
            return balance;
        }

        public SimpleStringProperty addressProperty() {
            return address;
        }

        public void setBalance(String value) {
            balance.set(value);
        }
    }
}
