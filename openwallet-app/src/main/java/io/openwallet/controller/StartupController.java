package io.openwallet.controller;

import io.openwallet.MainApp;
import javafx.fxml.FXML;

public class StartupController {

    private MainApp mainApp;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void handleCreateWallet() {
        mainApp.showCreateWallet();
    }

    @FXML
    private void handleImportWallet() {
        mainApp.showImportWallet();
    }
}
