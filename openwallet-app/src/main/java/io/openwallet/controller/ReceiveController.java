package io.openwallet.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import io.openwallet.MainApp;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.awt.image.BufferedImage;

public class ReceiveController {

    @FXML private ImageView qrCodeImageView;
    @FXML private Label addressLabel;
    @FXML private Label statusLabel;

    private MainApp mainApp;
    private String walletAddress;
    private String profileName;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setWalletData(String profileName, String address) {
        this.profileName = profileName;
        this.walletAddress = address;
        addressLabel.setText(address);
        generateQRCode(address);
    }

    private void generateQRCode(String text) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 200, 200);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            Image image = SwingFXUtils.toFXImage(bufferedImage, null);
            qrCodeImageView.setImage(image);
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error generating QR code");
        }
    }

    @FXML
    private void handleCopy() {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(walletAddress);
        clipboard.setContent(content);
        statusLabel.setText("Address copied to clipboard!");
    }

    @FXML
    private void handleBack() {
        mainApp.showDashboard(profileName);
    }
}
