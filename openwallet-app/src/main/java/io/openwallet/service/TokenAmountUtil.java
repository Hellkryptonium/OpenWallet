package io.openwallet.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public final class TokenAmountUtil {

    private TokenAmountUtil() {
    }

    public static BigInteger toRaw(BigDecimal amount, int decimals) {
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        if (decimals < 0) {
            throw new IllegalArgumentException("decimals must be >= 0");
        }
        BigDecimal scaled = amount.movePointRight(decimals);
        if (scaled.scale() > 0) {
            scaled = scaled.setScale(0, RoundingMode.DOWN);
        }
        return scaled.toBigIntegerExact();
    }

    public static BigDecimal fromRaw(BigInteger raw, int decimals) {
        if (raw == null) {
            throw new IllegalArgumentException("raw is required");
        }
        if (decimals < 0) {
            throw new IllegalArgumentException("decimals must be >= 0");
        }
        return new BigDecimal(raw).movePointLeft(decimals);
    }
}
