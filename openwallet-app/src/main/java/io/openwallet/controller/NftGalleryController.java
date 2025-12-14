package io.openwallet.controller;

import io.openwallet.MainApp;
import io.openwallet.db.WalletDao;
import io.openwallet.model.WalletProfile;
import io.openwallet.service.NftService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class NftGalleryController {

    @FXML private Label statusLabel;
    @FXML private TilePane galleryPane;

    private MainApp mainApp;
    private WalletDao walletDao;
    private NftService nftService;

        private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        private static final String DEFAULT_PLACEHOLDER_IMAGE = "https://placehold.co/512x512.png";

    private enum StatusTone { INFO, SUCCESS, ERROR, MUTED }

    private String profileName;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        this.walletDao = mainApp.getWalletDao();
        this.nftService = mainApp.getNftService();
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
        load();
    }

    @FXML
    private void handleRefresh() {
        load();
    }

    private void load() {
        if (profileName == null || profileName.isBlank()) {
            setStatus(StatusTone.ERROR, "No active wallet profile.");
            return;
        }

        Optional<WalletProfile> profileOpt = walletDao.findByProfileName(profileName);
        if (profileOpt.isEmpty()) {
            setStatus(StatusTone.ERROR, "Wallet profile not found.");
            return;
        }

        String address = profileOpt.get().getWalletAddress();
        setStatus(StatusTone.INFO, "Loading NFTs for " + address + "...");
        galleryPane.getChildren().clear();

        new Thread(() -> {
            try {
                List<NftService.NftItem> nfts = nftService.getOwnedNftsSepolia(address).join();
                Platform.runLater(() -> render(nfts));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus(StatusTone.ERROR, "Error loading NFTs: " + rootMessage(e));
                });
            }
        }).start();
    }

    private void render(List<NftService.NftItem> nfts) {
        galleryPane.getChildren().clear();

        if (nfts == null || nfts.isEmpty()) {
            setStatus(StatusTone.MUTED, "No NFTs found (Sepolia) for this address.");
            return;
        }

        setStatus(StatusTone.SUCCESS, "Found " + nfts.size() + " NFT(s).");

        for (NftService.NftItem item : nfts) {
            galleryPane.getChildren().add(createCard(item));
        }
    }

    private VBox createCard(NftService.NftItem item) {
        VBox card = new VBox(6);
        card.getStyleClass().add("nft-card");
        card.setPrefWidth(248);
        card.setMinWidth(248);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(224);
        imageView.setFitHeight(224);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        String imageUrl = item != null ? item.getImageUrl() : null;
        loadImageInto(imageView, imageUrl);

        Label name = new Label(item != null && item.getName() != null ? item.getName() : "(Unnamed)");
        name.getStyleClass().add("nft-name");
        name.setWrapText(true);

        String collectionText = item != null && item.getCollectionName() != null && !item.getCollectionName().isBlank()
                ? item.getCollectionName()
                : (item != null && item.getContractAddress() != null ? item.getContractAddress() : "");

        Label collection = new Label(collectionText);
        collection.getStyleClass().add("nft-meta");
        collection.setWrapText(true);

        String tokenId = item != null && item.getTokenId() != null ? item.getTokenId() : "";
        Label token = new Label(tokenId.isBlank() ? "" : ("Token ID: " + tokenId));
        token.getStyleClass().add("nft-meta");
        token.setWrapText(true);

        card.getChildren().addAll(imageView, name, collection, token);
        return card;
    }

    private void setStatus(StatusTone tone, String text) {
        statusLabel.setText(text == null ? "" : text);

        statusLabel.getStyleClass().removeAll(
                "status-info",
                "status-success",
                "status-error",
                "status-muted"
        );

        switch (tone) {
            case INFO -> statusLabel.getStyleClass().add("status-info");
            case SUCCESS -> statusLabel.getStyleClass().add("status-success");
            case ERROR -> statusLabel.getStyleClass().add("status-error");
            default -> statusLabel.getStyleClass().add("status-muted");
        }
    }

    private void loadImageInto(ImageView imageView, String imageUrl) {
        if (imageView == null) {
            return;
        }

        if (imageUrl == null || imageUrl.isBlank()) {
            // If metadata doesn't provide an image, show a neutral placeholder.
            setPlaceholder(imageView);
            return;
        }

        final String url = imageUrl.trim();

        // Support data:image/*;base64,... directly
        if (url.startsWith("data:image/") && url.contains(";base64,")) {
            int idx = url.indexOf(";base64,");
            if (idx > 0 && idx + 8 < url.length()) {
                String b64 = url.substring(idx + 8);
                try {
                    byte[] bytes = java.util.Base64.getDecoder().decode(b64);
                    Image img = new Image(new ByteArrayInputStream(bytes));
                    if (img.isError() || img.getWidth() <= 0) {
                        setPlaceholder(imageView);
                    } else {
                        imageView.setImage(img);
                    }
                    return;
                } catch (Exception ignored) {
                    // fall through to HTTP / FX loaders
                }
            }
        }

        try {
            // Try JavaFX's built-in loader first.
            Image fxImage = new Image(url, true);
            imageView.setImage(fxImage);

            fxImage.errorProperty().addListener((obs, wasError, isError) -> {
                if (Boolean.TRUE.equals(isError)) {
                    // Fallback: fetch bytes ourselves (some hosts behave better with explicit UA/redirects).
                    fetchAndSetImageBytes(imageView, url);
                }
            });
        } catch (Exception ignored) {
            fetchAndSetImageBytes(imageView, url);
        }
    }

    private void setPlaceholder(ImageView imageView) {
        try {
            Image img = new Image(DEFAULT_PLACEHOLDER_IMAGE, true);
            imageView.setImage(img);
        } catch (Exception ignored) {
        }
    }

    private void fetchAndSetImageBytes(ImageView imageView, String url) {
        new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(20))
                        .header("User-Agent", "OpenWallet")
                        .GET()
                        .build();

                HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                    Platform.runLater(() -> setPlaceholder(imageView));
                    return;
                }

                byte[] bytes = resp.body();
                if (bytes == null || bytes.length == 0) {
                    Platform.runLater(() -> setPlaceholder(imageView));
                    return;
                }

                Image img = new Image(new ByteArrayInputStream(bytes));
                Platform.runLater(() -> {
                    if (img.isError() || img.getWidth() <= 0) {
                        setPlaceholder(imageView);
                    } else {
                        imageView.setImage(img);
                    }
                });
            } catch (Exception ignored) {
                Platform.runLater(() -> setPlaceholder(imageView));
            }
        }).start();
    }

    private String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }
        return cur.getMessage() != null ? cur.getMessage() : cur.getClass().getSimpleName();
    }

    @FXML
    private void handleBack() {
        mainApp.showDashboard(profileName);
    }
}
