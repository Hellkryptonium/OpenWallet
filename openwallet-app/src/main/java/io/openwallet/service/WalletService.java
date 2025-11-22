package io.openwallet.service;

import io.openwallet.crypto.CryptoUtil;
import io.openwallet.db.DatabaseConfig;
import io.openwallet.db.TransactionLogDao;
import io.openwallet.db.WalletDao;
import io.openwallet.exception.AuthenticationException;
import io.openwallet.exception.InsufficientFundsException;
import io.openwallet.exception.OpenWalletException;
import io.openwallet.model.TransactionLog;
import io.openwallet.model.WalletProfile;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.Bip44WalletUtils;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.MnemonicUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class WalletService {

    private final WalletDao walletDao;
    private final TransactionLogDao transactionLogDao;
    private final Web3j web3j;

    public WalletService(WalletDao walletDao, TransactionLogDao transactionLogDao) {
        this.walletDao = walletDao;
        this.transactionLogDao = transactionLogDao;
        this.web3j = Web3j.build(new HttpService(DatabaseConfig.getRpcUrl()));
    }

    /**
     * Generates a new BIP-39 mnemonic.
     * @return 12-word mnemonic string.
     */
    public String generateMnemonic() {
        byte[] initialEntropy = new byte[16];
        new SecureRandom().nextBytes(initialEntropy);
        return MnemonicUtils.generateMnemonic(initialEntropy);
    }

    /**
     * Creates a new wallet, encrypts the private key, and saves it to the DB.
     * @param profileName User defined name for the wallet.
     * @param password Password to encrypt the private key.
     * @return The generated mnemonic (user must save this!).
     */
    public String createWallet(String profileName, String password) throws Exception {
        String mnemonic = generateMnemonic();
        importWallet(profileName, mnemonic, password);
        return mnemonic;
    }

    /**
     * Imports an existing wallet using mnemonic.
     */
    public void importWallet(String profileName, String mnemonic, String password) throws Exception {
        // 1. Validate Mnemonic
        if (!MnemonicUtils.validateMnemonic(mnemonic)) {
            throw new IllegalArgumentException("Invalid mnemonic phrase");
        }

        // 2. Derive Private Key (BIP-44: m/44'/60'/0'/0/0)
        // Seed generation
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, null);
        Bip32ECKeyPair masterKey = Bip32ECKeyPair.generateKeyPair(seed);
        
        // m/44'/60'/0'/0/0
        final int[] path = {44 | Bip32ECKeyPair.HARDENED_BIT, 60 | Bip32ECKeyPair.HARDENED_BIT, 0 | Bip32ECKeyPair.HARDENED_BIT, 0, 0};
        Bip32ECKeyPair derivedKey = Bip32ECKeyPair.deriveKeyPair(masterKey, path);
        
        Credentials credentials = Credentials.create(derivedKey);
        String privateKey = derivedKey.getPrivateKey().toString(16);
        String address = credentials.getAddress();

        // 3. Encrypt Private Key
        String encryptedJson = CryptoUtil.encrypt(privateKey, password);

        // 4. Save to DB
        WalletProfile profile = new WalletProfile(profileName, address, encryptedJson);
        walletDao.save(profile);
    }

    /**
     * Loads a wallet's private key by decrypting it.
     */
    public String getPrivateKey(String profileName, String password) throws Exception {
        Optional<WalletProfile> profileOpt = walletDao.findByProfileName(profileName);
        if (profileOpt.isEmpty()) {
            throw new IllegalArgumentException("Wallet profile not found: " + profileName);
        }

        WalletProfile profile = profileOpt.get();
        try {
            return CryptoUtil.decrypt(profile.getEncryptedJson(), password);
        } catch (Exception e) {
            throw new AuthenticationException("Incorrect password or corrupted key.");
        }
    }

    /**
     * Fetches the ETH balance for a given address from the Sepolia network.
     * @param address Ethereum address.
     * @return Balance in ETH as BigDecimal.
     */
    public CompletableFuture<BigDecimal> getBalance(String address) {
        return web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST)
                .sendAsync()
                .thenApply(ethGetBalance -> {
                    BigDecimal wei = new BigDecimal(ethGetBalance.getBalance());
                    return Convert.fromWei(wei, Convert.Unit.ETHER);
                });
    }

    /**
     * Sends ETH from the specified wallet profile to a recipient address.
     * @param profileName The name of the sender's wallet profile.
     * @param password The password to decrypt the private key.
     * @param toAddress The recipient's Ethereum address.
     * @param amount The amount of ETH to send.
     * @return The transaction hash.
     */
    public String sendTransaction(String profileName, String password, String toAddress, BigDecimal amount) throws Exception {
        // 1. Get Private Key
        String privateKey = getPrivateKey(profileName, password);
        Credentials credentials = Credentials.create(privateKey);

        // 2. Check Balance (Optional but good practice)
        // Note: This is async, so we block here for simplicity in this method, or we could chain futures.
        // For now, we let the node reject if insufficient funds, but we catch the exception.

        try {
            // 3. Send Transaction
            org.web3j.protocol.core.methods.response.TransactionReceipt receipt = 
                org.web3j.tx.Transfer.sendFunds(
                    web3j, 
                    credentials, 
                    toAddress, 
                    amount, 
                    Convert.Unit.ETHER
                ).send();

            String txHash = receipt.getTransactionHash();

            // 4. Log Transaction
            TransactionLog log = new TransactionLog(
                credentials.getAddress(),
                txHash,
                amount,
                "ETH",
                receipt.isStatusOK() ? "SUCCESS" : "FAILED"
            );
            transactionLogDao.save(log);

            return txHash;
        } catch (Exception e) {
            // Check for specific Web3j errors if possible
            if (e.getMessage().contains("insufficient funds")) {
                throw new InsufficientFundsException("Insufficient funds for transaction + gas.");
            }
            throw new OpenWalletException("Transaction failed: " + e.getMessage(), e);
        }
    }
}
