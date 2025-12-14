package io.openwallet.service;

import io.openwallet.db.TransactionLogDao;
import io.openwallet.exception.OpenWalletException;
import io.openwallet.model.TokenMeta;
import io.openwallet.model.TransactionLog;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.RawTransactionManager;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TokenService {

    private final NetworkManager networkManager;
    private final WalletService walletService;
    private final TransactionLogDao transactionLogDao;

    public TokenService(NetworkManager networkManager, WalletService walletService, TransactionLogDao transactionLogDao) {
        this.networkManager = networkManager;
        this.walletService = walletService;
        this.transactionLogDao = transactionLogDao;
    }

    public CompletableFuture<TokenMeta> fetchTokenMeta(String networkId, String tokenAddress) {
        Web3j web3j = networkManager.getWeb3j();

        CompletableFuture<String> nameF = callString(web3j, tokenAddress, "name");
        CompletableFuture<String> symbolF = callString(web3j, tokenAddress, "symbol");
        CompletableFuture<Integer> decimalsF = callUint8(web3j, tokenAddress, "decimals");

        return CompletableFuture.allOf(nameF, symbolF, decimalsF)
                .thenApply(v -> new TokenMeta(networkId, tokenAddress, nameF.join(), symbolF.join(), decimalsF.join()));
    }

    public CompletableFuture<BigDecimal> getTokenBalance(TokenMeta token, String walletAddress) {
        if (token == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("token is required"));
        }
        int decimals = token.getDecimals() != null ? token.getDecimals() : 18;
        return callBalanceOf(networkManager.getWeb3j(), token.getAddress(), walletAddress)
                .thenApply(raw -> TokenAmountUtil.fromRaw(raw, decimals));
    }

    public String sendToken(String profileName, String password, TokenMeta token, String toAddress, BigDecimal amount) throws Exception {
        if (token == null) {
            throw new IllegalArgumentException("token is required");
        }
        if (token.getDecimals() == null) {
            throw new IllegalArgumentException("token.decimals is required");
        }

        String privateKey = walletService.getPrivateKey(profileName, password);
        Credentials credentials = Credentials.create(privateKey);

        BigInteger rawAmount = TokenAmountUtil.toRaw(amount, token.getDecimals());

        Function function = new Function(
                "transfer",
                List.of(new Address(toAddress), new Uint256(rawAmount)),
                Collections.emptyList()
        );
        String data = FunctionEncoder.encode(function);

        Web3j web3j = networkManager.getWeb3j();
        EthGasPrice gasPriceResp = web3j.ethGasPrice().send();
        BigInteger gasPrice = gasPriceResp.getGasPrice();

        BigInteger gasLimit = estimateGas(web3j, credentials.getAddress(), token.getAddress(), data);

        RawTransactionManager txManager;
        Long chainId = networkManager.getChainId();
        if (chainId != null) {
            txManager = new RawTransactionManager(web3j, credentials, chainId);
        } else {
            txManager = new RawTransactionManager(web3j, credentials);
        }

        EthSendTransaction sent = txManager.sendTransaction(gasPrice, gasLimit, token.getAddress(), data, BigInteger.ZERO);
        if (sent.hasError()) {
            throw new OpenWalletException("Token transfer failed: " + sent.getError().getMessage());
        }

        String txHash = sent.getTransactionHash();

        // Log as PENDING (receipt not awaited)
        TransactionLog log = new TransactionLog(
                credentials.getAddress(),
                txHash,
                amount,
                token.getSymbol() != null ? token.getSymbol() : "TOKEN",
                "PENDING"
        );
        transactionLogDao.save(log);

        return txHash;
    }

    public String approve(String profileName, String password, TokenMeta token, String spenderAddress, BigDecimal amount) throws Exception {
        if (token == null) {
            throw new IllegalArgumentException("token is required");
        }
        if (token.getDecimals() == null) {
            throw new IllegalArgumentException("token.decimals is required");
        }

        String privateKey = walletService.getPrivateKey(profileName, password);
        Credentials credentials = Credentials.create(privateKey);

        BigInteger rawAmount = TokenAmountUtil.toRaw(amount, token.getDecimals());

        Function function = new Function(
                "approve",
                List.of(new Address(spenderAddress), new Uint256(rawAmount)),
                Collections.emptyList()
        );
        String data = FunctionEncoder.encode(function);

        Web3j web3j = networkManager.getWeb3j();
        EthGasPrice gasPriceResp = web3j.ethGasPrice().send();
        BigInteger gasPrice = gasPriceResp.getGasPrice();

        BigInteger gasLimit = estimateGas(web3j, credentials.getAddress(), token.getAddress(), data);

        RawTransactionManager txManager;
        Long chainId = networkManager.getChainId();
        if (chainId != null) {
            txManager = new RawTransactionManager(web3j, credentials, chainId);
        } else {
            txManager = new RawTransactionManager(web3j, credentials);
        }

        EthSendTransaction sent = txManager.sendTransaction(gasPrice, gasLimit, token.getAddress(), data, BigInteger.ZERO);
        if (sent.hasError()) {
            throw new OpenWalletException("Token approval failed: " + sent.getError().getMessage());
        }

        String txHash = sent.getTransactionHash();
        TransactionLog log = new TransactionLog(
                credentials.getAddress(),
                txHash,
                amount,
                token.getSymbol() != null ? token.getSymbol() : "TOKEN",
                "PENDING"
        );
        transactionLogDao.save(log);

        return txHash;
    }

    private CompletableFuture<String> callString(Web3j web3j, String contract, String functionName) {
        Function function = new Function(
                functionName,
                Collections.emptyList(),
                List.of(new TypeReference<Utf8String>() {})
        );
        return ethCall(web3j, contract, function)
                .thenApply(values -> values.isEmpty() ? "" : (String) values.get(0).getValue());
    }

    private CompletableFuture<Integer> callUint8(Web3j web3j, String contract, String functionName) {
        Function function = new Function(
                functionName,
                Collections.emptyList(),
                List.of(new TypeReference<Uint8>() {})
        );
        return ethCall(web3j, contract, function)
                .thenApply(values -> values.isEmpty() ? 18 : ((BigInteger) values.get(0).getValue()).intValue());
    }

    private CompletableFuture<BigInteger> callBalanceOf(Web3j web3j, String contract, String walletAddress) {
        Function function = new Function(
                "balanceOf",
                List.of(new Address(walletAddress)),
                List.of(new TypeReference<Uint256>() {})
        );
        return ethCall(web3j, contract, function)
                .thenApply(values -> values.isEmpty() ? BigInteger.ZERO : (BigInteger) values.get(0).getValue());
    }

    private CompletableFuture<List<Type>> ethCall(Web3j web3j, String contract, Function function) {
        String data = FunctionEncoder.encode(function);
        Transaction tx = Transaction.createEthCallTransaction(null, contract, data);
        return web3j.ethCall(tx, DefaultBlockParameterName.LATEST)
                .sendAsync()
                .thenApply(EthCall::getValue)
                .thenApply(raw -> FunctionReturnDecoder.decode(raw, function.getOutputParameters()));
    }

    private BigInteger estimateGas(Web3j web3j, String from, String to, String data) {
        try {
            Transaction tx = Transaction.createFunctionCallTransaction(from, null, null, null, to, BigInteger.ZERO, data);
            EthEstimateGas est = web3j.ethEstimateGas(tx).send();
            BigInteger amount = est.getAmountUsed();
            if (amount == null || amount.signum() <= 0) {
                return BigInteger.valueOf(150_000);
            }
            // add 20% buffer
            return amount.multiply(BigInteger.valueOf(12)).divide(BigInteger.TEN);
        } catch (Exception e) {
            return BigInteger.valueOf(150_000);
        }
    }
}
