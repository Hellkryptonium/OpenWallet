package io.openwallet.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openwallet.db.DatabaseConfig;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.utils.Numeric;

import java.net.URI;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.math.BigInteger;
import java.util.Base64;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Read-only NFT listing using Alchemy's NFT API for Ethereum Sepolia.
 */
public class NftService {

    private final ObjectMapper mapper;
    private final NetworkManager networkManager;
    private final HttpClient httpClient;

    public NftService(NetworkManager networkManager) {
        this(networkManager, new ObjectMapper());
    }

    public NftService(NetworkManager networkManager, ObjectMapper mapper) {
        this.networkManager = networkManager;
        this.mapper = mapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public CompletableFuture<List<NftItem>> getOwnedNftsSepolia(String ownerAddress) {
        if (ownerAddress == null || ownerAddress.isBlank()) {
            return CompletableFuture.completedFuture(List.<NftItem>of());
        }
        String owner = ownerAddress.trim();
        if (!owner.startsWith("0x") || owner.length() != 42) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid owner address"));
        }

        CompletableFuture<List<NftItem>> alchemy = getOwnedNftsSepoliaAlchemy(owner)
            .exceptionally(ex -> List.<NftItem>of());

        return alchemy.thenCompose(nfts -> {
            if (nfts != null && !nfts.isEmpty()) {
                // Alchemy NFT API sometimes returns items without image URLs / metadata.
                // Enrich missing pieces from on-chain tokenURI metadata.
                return enrichOwnedNftsFromOnChainIfNeeded(owner, nfts);
            }
            // Fallback: on-chain scan of known ERC-721 contracts (Transfer logs).
            return getOwnedNftsSepoliaOnChain(owner);
        });
    }

    private CompletableFuture<List<NftItem>> enrichOwnedNftsFromOnChainIfNeeded(String owner, List<NftItem> nfts) {
        if (nfts == null || nfts.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        Web3j web3j = networkManager != null ? networkManager.getWeb3j() : null;
        if (web3j == null) {
            return CompletableFuture.completedFuture(nfts);
        }

        List<CompletableFuture<NftItem>> enriched = nfts.stream()
                .map(i -> enrichOneIfNeeded(web3j, owner, i))
                .collect(Collectors.toList());

        CompletableFuture<Void> all = CompletableFuture.allOf(enriched.toArray(new CompletableFuture[0]));
        return all.thenApply(v -> enriched.stream().map(CompletableFuture::join).collect(Collectors.toList()));
    }

    private CompletableFuture<NftItem> enrichOneIfNeeded(Web3j web3j, String owner, NftItem item) {
        if (item == null) {
            return CompletableFuture.completedFuture(null);
        }

        String contract = item.getContractAddress();
        String tokenIdStr = item.getTokenId();
        if (contract == null || contract.isBlank() || !contract.startsWith("0x") || contract.length() != 42) {
            return CompletableFuture.completedFuture(item);
        }
        if (tokenIdStr == null || tokenIdStr.isBlank()) {
            return CompletableFuture.completedFuture(item);
        }

        boolean needsImage = item.getImageUrl() == null || item.getImageUrl().isBlank();
        boolean nameLooksPlaceholder = item.getName() == null || item.getName().isBlank() || item.getName().equals(tokenIdStr);

        if (!needsImage && !nameLooksPlaceholder) {
            return CompletableFuture.completedFuture(item);
        }

        BigInteger tokenId;
        try {
            tokenId = Numeric.toBigInt(tokenIdStr);
        } catch (Exception e) {
            try {
                tokenId = new BigInteger(tokenIdStr);
            } catch (Exception ignored) {
                return CompletableFuture.completedFuture(item);
            }
        }

        return callTokenUri(web3j, contract, owner, tokenId)
                .thenCompose(this::resolveTokenUriMetadataAsync)
                .thenApply(meta -> {
                    String name = item.getName();
                    String imageUrl = item.getImageUrl();

                    if (nameLooksPlaceholder && meta != null && meta.name != null && !meta.name.isBlank()) {
                        name = meta.name;
                    }

                    if (needsImage && meta != null && meta.image != null && !meta.image.isBlank()) {
                        imageUrl = normalizeImageUrl(meta.image);
                    }

                    return new NftItem(
                            safeLower(contract),
                            tokenIdStr,
                            name,
                            item.getCollectionName(),
                            imageUrl
                    );
                })
                .exceptionally(ex -> item);
    }

    private CompletableFuture<List<NftItem>> getOwnedNftsSepoliaAlchemy(String owner) {
        String apiKey = resolveAlchemyApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "Alchemy API key not configured. Set OPENWALLET_ALCHEMY_API_KEY (or ALCHEMY_API_KEY), or use an Alchemy RPC URL in db.properties."));
        }

        // Alchemy NFT API v3
        String base = "https://eth-sepolia.g.alchemy.com/nft/v3/" + apiKey;
        String url = base + "/getNFTsForOwner?owner=" + urlEncode(owner) + "&withMetadata=true&pageSize=100";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        return httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                        throw new RuntimeException("Alchemy NFT API request failed: HTTP " + resp.statusCode());
                    }
                    return resp.body();
                })
                .thenApply(this::parseOwnedNfts);
    }

    private CompletableFuture<List<NftItem>> getOwnedNftsSepoliaOnChain(String owner) {
        List<String> contracts = resolveFallbackContractAddresses();
        if (contracts.isEmpty()) {
            return CompletableFuture.completedFuture(List.<NftItem>of());
        }

        Web3j web3j = networkManager != null ? networkManager.getWeb3j() : null;
        if (web3j == null) {
            return CompletableFuture.completedFuture(List.<NftItem>of());
        }

        String rpcUrl = networkManager != null ? networkManager.getRpcUrl() : DatabaseConfig.getRpcUrl();
        boolean isAlchemyRpc = isAlchemyRpcUrl(rpcUrl);

        if (isAlchemyRpc) {
            // IMPORTANT: Alchemy Free tier blocks eth_getLogs beyond a 10-block range.
            // Use Alchemy's indexed asset transfers API to discover ERC-721 tokenIds instead.
            return fetchErc721TokenRefsViaAlchemyAssetTransfers(owner, contracts)
                    .thenCompose(tokenRefs -> buildItemsFromTokenRefs(web3j, owner, tokenRefs));
        }

        // Non-Alchemy RPCs can use eth_getLogs directly (and also avoids Alchemy DNS issues).
        return fetchErc721TokenRefsViaEthGetLogs(web3j, owner, contracts)
                .thenCompose(tokenRefs -> buildItemsFromTokenRefs(web3j, owner, tokenRefs));
    }

    private CompletableFuture<List<NftItem>> buildItemsFromTokenRefs(Web3j web3j, String owner, Set<TokenRef> tokenRefs) {
        if (tokenRefs == null || tokenRefs.isEmpty()) {
            return CompletableFuture.completedFuture(List.<NftItem>of());
        }

        List<CompletableFuture<NftItem>> perToken = tokenRefs.stream()
                .map(ref -> buildOwnedTokenItem(web3j, ref.contractAddress, owner, ref.tokenId))
                .collect(Collectors.toList());

        CompletableFuture<Void> all = CompletableFuture.allOf(perToken.toArray(new CompletableFuture[0]));
        return all.thenApply(v -> perToken.stream()
                .map(CompletableFuture::join)
                .filter(i -> i != null)
                .collect(Collectors.toList()));
    }

    private boolean isAlchemyRpcUrl(String rpcUrl) {
        if (rpcUrl == null) return false;
        String s = rpcUrl.toLowerCase(Locale.ROOT);
        return s.contains("alchemy.com") || s.contains("alchemyapi.io") || s.contains("g.alchemy.com");
    }

    private CompletableFuture<Set<TokenRef>> fetchErc721TokenRefsViaEthGetLogs(Web3j web3j, String owner, List<String> contractAddresses) {
        if (web3j == null) {
            return CompletableFuture.completedFuture(Set.of());
        }

        return web3j.ethBlockNumber().sendAsync()
                .thenApply(EthBlockNumber::getBlockNumber)
                .thenCompose(latest -> {
                    BigInteger lookback = BigInteger.valueOf(200_000L);
                    BigInteger fromBlockComputed = latest.subtract(lookback);
                    if (fromBlockComputed.signum() < 0) {
                        fromBlockComputed = BigInteger.ZERO;
                    }
                    final BigInteger fromBlock = fromBlockComputed;

                    List<CompletableFuture<Set<TokenRef>>> perContract = contractAddresses.stream()
                            .map(c -> scanTransfersToOwnerViaLogs(web3j, owner, c, fromBlock, latest))
                            .collect(Collectors.toList());

                    CompletableFuture<Void> all = CompletableFuture.allOf(perContract.toArray(new CompletableFuture[0]));
                    return all.thenApply(v -> {
                        Set<TokenRef> merged = new HashSet<>();
                        for (CompletableFuture<Set<TokenRef>> f : perContract) {
                            merged.addAll(f.join());
                        }
                        return merged;
                    });
                })
                .exceptionally(ex -> Set.of());
    }

    private CompletableFuture<Set<TokenRef>> scanTransfersToOwnerViaLogs(Web3j web3j, String owner, String contractAddress, BigInteger fromBlock, BigInteger toBlock) {
        String contract = safeLower(contractAddress);
        if (contract == null || !contract.startsWith("0x") || contract.length() != 42) {
            return CompletableFuture.completedFuture(Set.of());
        }

        String transferTopic = Hash.sha3String("Transfer(address,address,uint256)");
        String toTopic = addressToTopic(owner);

        // Chunk ranges to avoid provider limits.
        BigInteger step = BigInteger.valueOf(5_000L);
        List<CompletableFuture<Set<TokenRef>>> chunks = new ArrayList<>();

        BigInteger start = fromBlock;
        while (start.compareTo(toBlock) <= 0) {
            BigInteger end = start.add(step);
            if (end.compareTo(toBlock) > 0) {
                end = toBlock;
            }
            final BigInteger s = start;
            final BigInteger e = end;

            EthFilter filter = new EthFilter(
                    new DefaultBlockParameterNumber(s),
                    new DefaultBlockParameterNumber(e),
                    contract
            );
            filter.addSingleTopic(transferTopic);
            filter.addNullTopic();
            filter.addSingleTopic(toTopic);

            CompletableFuture<Set<TokenRef>> fut = web3j.ethGetLogs(filter).sendAsync()
                    .thenApply(resp -> {
                        Set<TokenRef> out = new HashSet<>();
                        List<EthLog.LogResult> results = resp.getLogs();
                        if (results == null || results.isEmpty()) {
                            return out;
                        }
                        for (EthLog.LogResult r : results) {
                            Object obj = r.get();
                            if (!(obj instanceof Log log)) {
                                continue;
                            }
                            List<String> topics = log.getTopics();
                            if (topics == null || topics.size() < 4) {
                                continue;
                            }
                            try {
                                BigInteger tokenId = Numeric.toBigInt(topics.get(3));
                                out.add(new TokenRef(contract, tokenId));
                            } catch (Exception ignored) {
                            }
                        }
                        return out;
                    })
                    .exceptionally(ex -> Set.of());

            chunks.add(fut);
            start = end.add(BigInteger.ONE);
        }

        CompletableFuture<Void> all = CompletableFuture.allOf(chunks.toArray(new CompletableFuture[0]));
        return all.thenApply(v -> {
            Set<TokenRef> merged = new HashSet<>();
            for (CompletableFuture<Set<TokenRef>> f : chunks) {
                merged.addAll(f.join());
            }
            return merged;
        });
    }

    private String addressToTopic(String address) {
        String a = address == null ? "" : address.trim();
        if (a.startsWith("0x")) {
            a = a.substring(2);
        }
        a = a.toLowerCase(Locale.ROOT);
        if (a.length() > 40) {
            a = a.substring(a.length() - 40);
        }
        String padded = "0".repeat(Math.max(0, 64 - a.length())) + a;
        return "0x" + padded;
    }

    private static class TokenRef {
        final String contractAddress;
        final BigInteger tokenId;

        private TokenRef(String contractAddress, BigInteger tokenId) {
            this.contractAddress = contractAddress;
            this.tokenId = tokenId;
        }
    }

    private CompletableFuture<Set<TokenRef>> fetchErc721TokenRefsViaAlchemyAssetTransfers(String owner, List<String> contractAddresses) {
        String rpcUrl = networkManager != null ? networkManager.getRpcUrl() : DatabaseConfig.getRpcUrl();
        if (rpcUrl == null || rpcUrl.isBlank()) {
            return CompletableFuture.completedFuture(Set.of());
        }

        // This method only exists on Alchemy endpoints.
        // If not supported, return empty and let the caller show "no NFTs".
        return fetchAssetTransfersPage(owner, contractAddresses, null, 0, Set.of());
    }

    private CompletableFuture<Set<TokenRef>> fetchAssetTransfersPage(
            String owner,
            List<String> contractAddresses,
            String pageKey,
            int page,
            Set<TokenRef> acc
    ) {
        // Keep it bounded; NFTs are small in our demo.
        if (page >= 5) {
            return CompletableFuture.completedFuture(acc);
        }

        String rpcUrl = networkManager != null ? networkManager.getRpcUrl() : DatabaseConfig.getRpcUrl();
        if (rpcUrl == null || rpcUrl.isBlank()) {
            return CompletableFuture.completedFuture(acc);
        }

        try {
            var params = mapper.createObjectNode();
            params.put("fromBlock", "0x0");
            params.put("toBlock", "latest");
            params.put("toAddress", owner);
            params.put("withMetadata", false);
            params.put("excludeZeroValue", false);
            params.put("maxCount", "0x3e8");

            var cats = params.putArray("category");
            cats.add("erc721");

            if (contractAddresses != null && !contractAddresses.isEmpty()) {
                var arr = params.putArray("contractAddresses");
                for (String c : contractAddresses) {
                    if (c != null && !c.isBlank()) {
                        arr.add(c.trim());
                    }
                }
            }

            if (pageKey != null && !pageKey.isBlank()) {
                params.put("pageKey", pageKey);
            }

            var body = mapper.createObjectNode();
            body.put("jsonrpc", "2.0");
            body.put("id", 1);
            body.put("method", "alchemy_getAssetTransfers");
            var p = body.putArray("params");
            p.add(params);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(rpcUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            return httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenApply(resp -> {
                        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                            throw new RuntimeException("Alchemy RPC request failed: HTTP " + resp.statusCode());
                        }
                        return resp.body();
                    })
                    .thenApply(this::parseAssetTransfersTokenRefs)
                    .thenCompose(parsed -> {
                        Set<TokenRef> merged = new HashSet<>(acc);
                        merged.addAll(parsed.items);
                        if (parsed.nextPageKey == null || parsed.nextPageKey.isBlank()) {
                            return CompletableFuture.completedFuture(merged);
                        }
                        return fetchAssetTransfersPage(owner, contractAddresses, parsed.nextPageKey, page + 1, merged);
                    })
                    .exceptionally(ex -> acc);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(acc);
        }
    }

    private static class AssetTransferParseResult {
        final Set<TokenRef> items;
        final String nextPageKey;

        private AssetTransferParseResult(Set<TokenRef> items, String nextPageKey) {
            this.items = items;
            this.nextPageKey = nextPageKey;
        }
    }

    private AssetTransferParseResult parseAssetTransfersTokenRefs(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            if (root.has("error")) {
                // Method not supported or plan limitation.
                return new AssetTransferParseResult(Set.of(), null);
            }
            JsonNode result = root.get("result");
            if (result == null || result.isNull()) {
                return new AssetTransferParseResult(Set.of(), null);
            }
            String nextPageKey = result.hasNonNull("pageKey") ? result.get("pageKey").asText(null) : null;
            JsonNode transfers = result.get("transfers");
            if (transfers == null || !transfers.isArray()) {
                return new AssetTransferParseResult(Set.of(), nextPageKey);
            }

            Set<TokenRef> out = new HashSet<>();
            for (JsonNode t : transfers) {
                if (t == null || t.isNull()) continue;

                String contract = null;
                JsonNode rawContract = t.get("rawContract");
                if (rawContract != null && rawContract.hasNonNull("address")) {
                    contract = rawContract.get("address").asText(null);
                }
                if (contract == null || contract.isBlank()) continue;

                String tokenIdStr = null;
                if (t.hasNonNull("erc721TokenId")) {
                    tokenIdStr = t.get("erc721TokenId").asText(null);
                } else if (t.hasNonNull("tokenId")) {
                    tokenIdStr = t.get("tokenId").asText(null);
                }
                if (tokenIdStr == null || tokenIdStr.isBlank()) continue;

                BigInteger tokenId;
                try {
                    tokenId = Numeric.toBigInt(tokenIdStr);
                } catch (Exception ignored) {
                    try {
                        tokenId = new BigInteger(tokenIdStr);
                    } catch (Exception ignored2) {
                        continue;
                    }
                }

                out.add(new TokenRef(safeLower(contract), tokenId));
            }

            return new AssetTransferParseResult(out, nextPageKey);
        } catch (Exception e) {
            return new AssetTransferParseResult(Set.of(), null);
        }
    }

    private CompletableFuture<NftItem> buildOwnedTokenItem(Web3j web3j, String contract, String owner, BigInteger tokenId) {
        return callOwnerOf(web3j, contract, owner, tokenId)
                .thenCompose(currentOwner -> {
                    if (currentOwner == null || !currentOwner.equalsIgnoreCase(owner)) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return callTokenUri(web3j, contract, owner, tokenId)
                            .thenCompose(this::resolveTokenUriMetadataAsync)
                            .thenApply(meta -> {
                                String name = meta.name != null && !meta.name.isBlank() ? meta.name : ("Token #" + tokenId);
                                String imageUrl = meta.image != null ? normalizeImageUrl(meta.image) : null;
                                return new NftItem(
                                        safeLower(contract),
                                        tokenId.toString(),
                                        name,
                                        contract,
                                        imageUrl
                                );
                            });
                })
                .exceptionally(ex -> null);
    }

    private CompletableFuture<String> callOwnerOf(Web3j web3j, String contract, String from, BigInteger tokenId) {
        Function f = new Function(
                "ownerOf",
                List.of(new Uint256(tokenId)),
                List.of(new TypeReference<Address>() {})
        );
        return callString(web3j, contract, from, f);
    }

    private CompletableFuture<String> callTokenUri(Web3j web3j, String contract, String from, BigInteger tokenId) {
        Function f = new Function(
                "tokenURI",
                List.of(new Uint256(tokenId)),
                List.of(new TypeReference<Utf8String>() {})
        );
        return callString(web3j, contract, from, f);
    }

    private CompletableFuture<String> callString(Web3j web3j, String contract, String from, Function f) {
        String data = FunctionEncoder.encode(f);
        Transaction tx = Transaction.createEthCallTransaction(from, contract, data);
        return web3j.ethCall(tx, DefaultBlockParameterName.LATEST).sendAsync()
                .thenApply(EthCall::getValue)
                .thenApply(value -> {
                    if (value == null || value.equals("0x")) {
                        return null;
                    }
                    List<Type> decoded = FunctionReturnDecoder.decode(value, f.getOutputParameters());
                    if (decoded == null || decoded.isEmpty() || decoded.get(0) == null) {
                        return null;
                    }
                    Object v = decoded.get(0).getValue();
                    return v != null ? v.toString() : null;
                });
    }

    private static class NftMetadata {
        final String name;
        final String image;

        private NftMetadata(String name, String image) {
            this.name = name;
            this.image = image;
        }
    }

    private CompletableFuture<NftMetadata> resolveTokenUriMetadataAsync(String tokenUri) {
        if (tokenUri == null || tokenUri.isBlank()) {
            return CompletableFuture.completedFuture(new NftMetadata(null, null));
        }

        String uri = tokenUri.trim();
        // 1) data:application/json;base64,<base64>
        String base64Prefix = "data:application/json;base64,";
        if (uri.startsWith(base64Prefix)) {
            return CompletableFuture.completedFuture(parseJsonMetadataFromBase64(uri.substring(base64Prefix.length())));
        }

        // 2) data:application/json[;charset=utf-8],<urlencoded json>
        String dataJsonPrefix = "data:application/json";
        if (uri.startsWith(dataJsonPrefix)) {
            int comma = uri.indexOf(',');
            if (comma > 0 && comma + 1 < uri.length()) {
                String payload = uri.substring(comma + 1);
                try {
                    String decoded = URLDecoder.decode(payload, StandardCharsets.UTF_8);
                    return CompletableFuture.completedFuture(parseJsonMetadata(decoded));
                } catch (Exception ignored) {
                    return CompletableFuture.completedFuture(new NftMetadata(null, null));
                }
            }
            return CompletableFuture.completedFuture(new NftMetadata(null, null));
        }

        // 3) http(s) JSON metadata
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            return fetchAndParseJsonMetadata(uri);
        }

        // 4) ipfs JSON metadata
        if (uri.startsWith("ipfs://")) {
            String url = normalizeIpfsToGateway(uri);
            return fetchAndParseJsonMetadata(url);
        }

        return CompletableFuture.completedFuture(new NftMetadata(null, null));
    }

    private NftMetadata parseJsonMetadataFromBase64(String base64) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            String json = new String(decoded, StandardCharsets.UTF_8);
            return parseJsonMetadata(json);
        } catch (Exception ignored) {
            return new NftMetadata(null, null);
        }
    }

    private NftMetadata parseJsonMetadata(String json) {
        try {
            JsonNode node = mapper.readTree(json);
            String name = firstNonBlank(
                    node.hasNonNull("name") ? node.get("name").asText(null) : null,
                    node.hasNonNull("title") ? node.get("title").asText(null) : null
            );

            String image = firstNonBlank(
                    node.hasNonNull("image") ? node.get("image").asText(null) : null,
                    node.hasNonNull("image_url") ? node.get("image_url").asText(null) : null,
                    node.hasNonNull("imageUrl") ? node.get("imageUrl").asText(null) : null,
                    node.hasNonNull("image_uri") ? node.get("image_uri").asText(null) : null,
                    node.hasNonNull("imageURI") ? node.get("imageURI").asText(null) : null
            );

            // Some collections provide SVG content in image_data.
            if ((image == null || image.isBlank()) && node.hasNonNull("image_data")) {
                String svg = node.get("image_data").asText(null);
                if (svg != null && !svg.isBlank()) {
                    String b64 = Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
                    image = "data:image/svg+xml;base64," + b64;
                }
            }
            return new NftMetadata(name, image);
        } catch (Exception ignored) {
            return new NftMetadata(null, null);
        }
    }

    private CompletableFuture<NftMetadata> fetchAndParseJsonMetadata(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "OpenWallet")
                    .GET()
                    .build();

            return httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenApply(resp -> {
                        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                            return null;
                        }
                        return resp.body();
                    })
                    .thenApply(body -> body == null ? new NftMetadata(null, null) : parseJsonMetadata(body))
                    .exceptionally(ex -> new NftMetadata(null, null));
        } catch (Exception ignored) {
            return CompletableFuture.completedFuture(new NftMetadata(null, null));
        }
    }

    private String normalizeIpfsToGateway(String uri) {
        String u = uri.trim();
        if (u.startsWith("ipfs://")) {
            return "https://ipfs.io/ipfs/" + u.substring("ipfs://".length());
        }
        return u;
    }

    private List<String> resolveFallbackContractAddresses() {
        List<String> out = new ArrayList<>();

        String env = System.getenv("OPENWALLET_NFT_CONTRACTS");
        if (env != null && !env.isBlank()) {
            for (String part : env.split(",")) {
                String v = part != null ? part.trim() : "";
                if (!v.isBlank()) {
                    out.add(v);
                }
            }
        }

        // Local dev convenience: read sepolia-nft/.deployed-address if present.
        for (Path p : deployedAddressCandidates()) {
            String addr = readSingleLineIfExists(p);
            if (addr != null && !addr.isBlank()) {
                out.add(addr.trim());
            }
        }

        return out.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<Path> deployedAddressCandidates() {
        List<Path> paths = new ArrayList<>();
        paths.add(Paths.get("sepolia-nft", ".deployed-address"));
        paths.add(Paths.get("..", "sepolia-nft", ".deployed-address"));
        paths.add(Paths.get("..", "..", "sepolia-nft", ".deployed-address"));
        return paths;
    }

    private String readSingleLineIfExists(Path path) {
        try {
            if (path == null || !Files.exists(path)) {
                return null;
            }
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            if (lines == null || lines.isEmpty()) {
                return null;
            }
            return lines.get(0);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<NftItem> parseOwnedNfts(String json) {
        try {
            OwnedNftsResponse response = mapper.readValue(json, OwnedNftsResponse.class);
            List<NftItem> out = new ArrayList<>();
            if (response == null || response.ownedNfts == null) {
                return out;
            }

            for (OwnedNft n : response.ownedNfts) {
                if (n == null) continue;

                String contract = n.contract != null ? n.contract.address : null;
                String tokenId = n.tokenId;

                String title = firstNonBlank(n.name, n.title, tokenId);
                String collection = n.collection != null ? n.collection.name : null;

                String imageUrl = bestImageUrl(n);
                if (imageUrl != null) {
                    imageUrl = normalizeImageUrl(imageUrl);
                }

                out.add(new NftItem(
                        safeLower(contract),
                        tokenId,
                        title,
                        collection,
                        imageUrl
                ));
            }

            return out;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse NFT response", e);
        }
    }

    private String bestImageUrl(OwnedNft n) {
        if (n.image != null) {
            if (isNonBlank(n.image.cachedUrl)) return n.image.cachedUrl;
            if (isNonBlank(n.image.thumbnailUrl)) return n.image.thumbnailUrl;
            if (isNonBlank(n.image.pngUrl)) return n.image.pngUrl;
            if (isNonBlank(n.image.originalUrl)) return n.image.originalUrl;
        }
        // Some older payloads include media[]
        if (n.media != null && !n.media.isEmpty()) {
            Media m = n.media.get(0);
            if (m != null) {
                if (isNonBlank(m.gateway)) return m.gateway;
                if (isNonBlank(m.raw)) return m.raw;
            }
        }
        return null;
    }

    private String normalizeImageUrl(String url) {
        String u = url.trim();
        if (u.startsWith("ipfs://")) {
            // Simple default gateway
            String path = u.substring("ipfs://".length());
            if (path.startsWith("ipfs/")) {
                path = path.substring("ipfs/".length());
            }
            return "https://ipfs.io/ipfs/" + path;
        }
        if (u.startsWith("ar://")) {
            return "https://arweave.net/" + u.substring("ar://".length());
        }
        return u;
    }

    private String resolveAlchemyApiKey() {
        // 1) explicit env overrides
        String env = System.getenv("OPENWALLET_ALCHEMY_API_KEY");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        env = System.getenv("ALCHEMY_API_KEY");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }

        // 2) attempt to extract from an Alchemy RPC URL
        String rpcUrl = networkManager != null ? networkManager.getRpcUrl() : DatabaseConfig.getRpcUrl();
        return extractAlchemyKeyFromRpcUrl(rpcUrl).orElse("");
    }

    private Optional<String> extractAlchemyKeyFromRpcUrl(String rpcUrl) {
        if (rpcUrl == null) {
            return Optional.empty();
        }
        String s = rpcUrl.trim();
        String marker = "/v2/";
        int idx = s.indexOf(marker);
        if (idx < 0) {
            return Optional.empty();
        }
        String after = s.substring(idx + marker.length());
        int q = after.indexOf('?');
        if (q >= 0) {
            after = after.substring(0, q);
        }
        after = after.trim();
        if (after.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(after);
    }

    private String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private String safeLower(String s) {
        return s == null ? null : s.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isNonBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OwnedNftsResponse {
        public List<OwnedNft> ownedNfts;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OwnedNft {
        public String tokenId;
        public String name;
        public String title;
        public Contract contract;
        public Collection collection;
        public Image image;
        public List<Media> media;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Contract {
        public String address;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Collection {
        public String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Image {
        public String cachedUrl;
        public String thumbnailUrl;
        public String pngUrl;
        public String originalUrl;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Media {
        public String gateway;
        public String raw;
    }

    public static class NftItem {
        private final String contractAddress;
        private final String tokenId;
        private final String name;
        private final String collectionName;
        private final String imageUrl;

        public NftItem(String contractAddress, String tokenId, String name, String collectionName, String imageUrl) {
            this.contractAddress = contractAddress;
            this.tokenId = tokenId;
            this.name = name;
            this.collectionName = collectionName;
            this.imageUrl = imageUrl;
        }

        public String getContractAddress() {
            return contractAddress;
        }

        public String getTokenId() {
            return tokenId;
        }

        public String getName() {
            return name;
        }

        public String getCollectionName() {
            return collectionName;
        }

        public String getImageUrl() {
            return imageUrl;
        }
    }
}
