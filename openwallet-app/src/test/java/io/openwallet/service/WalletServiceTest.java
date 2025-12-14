package io.openwallet.service;

import io.openwallet.db.TransactionLogDao;
import io.openwallet.db.WalletDao;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.MnemonicUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WalletServiceTest {

    @Test
    void testGenerateMnemonic() {
        WalletDao mockDao = mock(WalletDao.class);
        TransactionLogDao mockTxDao = mock(TransactionLogDao.class);
        NetworkManager mockNetworkManager = mock(NetworkManager.class);
        WalletService service = new WalletService(mockDao, mockTxDao, mockNetworkManager);
        
        String mnemonic = service.generateMnemonic();
        assertNotNull(mnemonic);
        assertTrue(mnemonic.split(" ").length == 12);
        assertTrue(MnemonicUtils.validateMnemonic(mnemonic));
    }

    // Note: Full integration test would require a real DB or in-memory DB.
    // For unit test, we mock the DAO.
}
