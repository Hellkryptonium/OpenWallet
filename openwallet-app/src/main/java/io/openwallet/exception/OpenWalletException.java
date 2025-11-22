package io.openwallet.exception;

public class OpenWalletException extends Exception {
    public OpenWalletException(String message) {
        super(message);
    }

    public OpenWalletException(String message, Throwable cause) {
        super(message, cause);
    }
}
