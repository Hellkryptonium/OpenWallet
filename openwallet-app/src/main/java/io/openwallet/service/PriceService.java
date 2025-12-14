package io.openwallet.service;

import io.openwallet.model.NetworkConfig;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Int256;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.abi.datatypes.generated.Uint80;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PriceService {

    private final NetworkManager networkManager;

    public PriceService(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    public CompletableFuture<BigDecimal> getUsdPrice(String feedKey) {
        NetworkConfig network = networkManager.getActiveNetwork();
        if (network == null || network.getChainlinkFeeds() == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Chainlink feeds not configured"));
        }
        String feedAddress = network.getChainlinkFeeds().get(feedKey);
        if (feedAddress == null || feedAddress.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Chainlink feed not configured for key: " + feedKey));
        }
        return getLatestPrice(feedAddress);
    }

    public CompletableFuture<BigDecimal> getLatestPrice(String feedAddress) {
        Web3j web3j = networkManager.getWeb3j();
        CompletableFuture<Integer> decimalsF = callUint8(web3j, feedAddress, "decimals");
        CompletableFuture<BigInteger> answerF = callLatestAnswer(web3j, feedAddress);

        return CompletableFuture.allOf(decimalsF, answerF)
                .thenApply(v -> {
                    int decimals = decimalsF.join();
                    BigInteger answer = answerF.join();
                    return new BigDecimal(answer).movePointLeft(decimals);
                });
    }

    private CompletableFuture<Integer> callUint8(Web3j web3j, String contract, String functionName) {
        Function function = new Function(
                functionName,
                Collections.emptyList(),
                List.of(new TypeReference<Uint8>() {})
        );
        return ethCall(web3j, contract, function)
                .thenApply(values -> values.isEmpty() ? 0 : ((BigInteger) values.get(0).getValue()).intValue());
    }

    private CompletableFuture<BigInteger> callLatestAnswer(Web3j web3j, String feedAddress) {
        // latestRoundData() returns (uint80, int256, uint256, uint256, uint80)
        Function function = new Function(
                "latestRoundData",
                Collections.emptyList(),
                List.of(
                        new TypeReference<Uint80>() {},
                        new TypeReference<Int256>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Uint80>() {}
                )
        );

        return ethCall(web3j, feedAddress, function)
                .thenApply(values -> {
                    if (values.size() < 2) {
                        return BigInteger.ZERO;
                    }
                    return (BigInteger) values.get(1).getValue();
                });
    }

    private CompletableFuture<List<Type>> ethCall(Web3j web3j, String contract, Function function) {
        String data = FunctionEncoder.encode(function);
        Transaction tx = Transaction.createEthCallTransaction(null, contract, data);
        return web3j.ethCall(tx, DefaultBlockParameterName.LATEST)
                .sendAsync()
                .thenApply(EthCall::getValue)
                .thenApply(raw -> FunctionReturnDecoder.decode(raw, function.getOutputParameters()));
    }
}
