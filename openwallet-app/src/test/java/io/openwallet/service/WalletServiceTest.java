package io.openwallet.service;

import io.openwallet.db.WalletDao;
import io.openwallet.model.WalletProfile;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.web3j.crypto.MnemonicUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WalletServiceTest {

    @Test
    void testGenerateMnemonic() {
        WalletDao mockDao = mock(WalletDao.class);
        WalletService service = new WalletService(mockDao);
        
        String mnemonic = service.generateMnemonic();
        assertNotNull(mnemonic);
        assertTrue(mnemonic.split(" ").length == 12);
        assertTrue(MnemonicUtils.validateMnemonic(mnemonic));
    }

    // Note: Full integration test would require a real DB or in-memory DB.
    // For unit test, we mock the DAO.
}
