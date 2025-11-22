package io.openwallet.db;

import io.openwallet.model.WalletProfile;
import java.util.Optional;

public interface WalletDao extends Dao<WalletProfile> {
    Optional<WalletProfile> findByProfileName(String profileName);
}
