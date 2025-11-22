package io.openwallet.db;

import io.openwallet.model.TransactionLog;
import java.util.List;

public interface TransactionLogDao extends Dao<TransactionLog> {
    List<TransactionLog> findByWalletAddress(String walletAddress);
}
