package io.openwallet.controller;

import io.openwallet.MainApp;
import io.openwallet.db.TransactionLogDao;
import io.openwallet.db.WalletDao;
import io.openwallet.model.TransactionLog;
import io.openwallet.model.WalletProfile;
import io.openwallet.service.WalletService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.Optional;

public class DashboardController {

    @FXML private Label walletNameLabel;
    @FXML private Label addressLabel;
    @FXML private Label balanceLabel;
    @FXML private TableView<TransactionLog> txTable;

    private MainApp mainApp;
    private WalletDao walletDao;
    private TransactionLogDao transactionLogDao;
    private WalletService walletService;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        this.walletDao = mainApp.getWalletDao();
        this.transactionLogDao = mainApp.getTransactionLogDao(); // Need to add getter in MainApp
        this.walletService = mainApp.getWalletService();
        setupTable();
    }

    private void setupTable() {
        TableColumn<TransactionLog, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getCreatedAt() != null ? 
                cellData.getValue().getCreatedAt().toString().replace("T", " ").substring(0, 16) : ""
        ));

        TableColumn<TransactionLog, String> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));

        TableColumn<TransactionLog, String> tokenCol = new TableColumn<>("Token");
        tokenCol.setCellValueFactory(new PropertyValueFactory<>("tokenSymbol"));

        TableColumn<TransactionLog, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<TransactionLog, String> hashCol = new TableColumn<>("Tx Hash");
        hashCol.setCellValueFactory(new PropertyValueFactory<>("txHash"));

        txTable.getColumns().addAll(dateCol, amountCol, tokenCol, statusCol, hashCol);
    }

    public void loadWallet(String profileName) {
        Optional<WalletProfile> profileOpt = walletDao.findByProfileName(profileName);
        if (profileOpt.isPresent()) {
            WalletProfile profile = profileOpt.get();
            walletNameLabel.setText(profile.getProfileName());
            addressLabel.setText(profile.getWalletAddress());
            
            // Load Transactions
            loadTransactions(profile.getWalletAddress());

            // Fetch real balance
            balanceLabel.setText("Loading...");
            walletService.getBalance(profile.getWalletAddress())
                    .thenAccept(balance -> {
                        Platform.runLater(() -> {
                            balanceLabel.setText(String.format("%.4f ETH", balance));
                        });
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            balanceLabel.setText("Error");
                            ex.printStackTrace();
                        });
                        return null;
                    });
        }
    }

    private void loadTransactions(String address) {
        // Run in background to avoid freezing UI
        new Thread(() -> {
            List<TransactionLog> logs = transactionLogDao.findByWalletAddress(address);
            // Sort by ID desc (newest first) - simple way since ID is auto-increment
            logs.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
            
            Platform.runLater(() -> {
                txTable.setItems(FXCollections.observableArrayList(logs));
            });
        }).start();
    }

    @FXML
    private void handleLogout() {
        mainApp.showLogin();
    }

    @FXML
    private void handleSend() {
        // Get current profile name from label or store it in a field
        String profileName = walletNameLabel.getText();
        mainApp.showSendTransaction(profileName);
    }

    @FXML
    private void handleReceive() {
        String profileName = walletNameLabel.getText();
        String address = addressLabel.getText();
        mainApp.showReceive(profileName, address);
    }
}
