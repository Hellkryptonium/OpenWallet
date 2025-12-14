package io.openwallet.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class TokenAmountUtilTest {

    @Test
    void toRawAndFromRaw_roundTrips() {
        BigDecimal amount = new BigDecimal("1.2345");
        int decimals = 6;

        BigInteger raw = TokenAmountUtil.toRaw(amount, decimals);
        assertEquals(new BigInteger("1234500"), raw);

        BigDecimal back = TokenAmountUtil.fromRaw(raw, decimals);
        assertEquals(new BigDecimal("1.2345"), back.stripTrailingZeros());
    }

    @Test
    void toRaw_truncatesExtraPrecision() {
        BigDecimal amount = new BigDecimal("1.23456789");
        int decimals = 6;

        BigInteger raw = TokenAmountUtil.toRaw(amount, decimals);
        assertEquals(new BigInteger("1234567"), raw);
    }
}
