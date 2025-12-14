package io.openwallet.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openwallet.model.TokenMeta;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class JsonTokenRepository implements TokenRepository {

    private static final String DEFAULT_TOKENS_RESOURCE = "tokens.json";

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path customTokensPath;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private List<TokenMeta> tokens = new ArrayList<>();

    public JsonTokenRepository() {
        this(customPath());
    }

    public JsonTokenRepository(Path customTokensPath) {
        this.customTokensPath = customTokensPath;
        reload();
    }

    public void reload() {
        List<TokenMeta> loaded = new ArrayList<>();

        // defaults
        try (InputStream in = JsonTokenRepository.class.getClassLoader().getResourceAsStream(DEFAULT_TOKENS_RESOURCE)) {
            if (in != null) {
                loaded.addAll(mapper.readValue(in, new TypeReference<List<TokenMeta>>() {}));
            }
        } catch (Exception ignored) {
        }

        // customs
        if (Files.exists(customTokensPath)) {
            try {
                loaded.addAll(mapper.readValue(Files.readString(customTokensPath), new TypeReference<List<TokenMeta>>() {}));
            } catch (Exception ignored) {
            }
        }

        // normalize/dedupe
        loaded = loaded.stream()
                .filter(t -> t.getNetworkId() != null && t.getAddress() != null)
                .map(this::normalize)
                .collect(Collectors.toMap(
                        t -> (t.getNetworkId() + ":" + t.getAddress()).toLowerCase(Locale.ROOT),
                        t -> t,
                        (a, b) -> b
                ))
                .values()
                .stream()
                .sorted(Comparator.comparing(TokenMeta::getNetworkId).thenComparing(TokenMeta::getSymbol, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());

        lock.writeLock().lock();
        try {
            this.tokens = new ArrayList<>(loaded);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<TokenMeta> listByNetwork(String networkId) {
        if (networkId == null) {
            return List.of();
        }
        lock.readLock().lock();
        try {
            return tokens.stream()
                    .filter(t -> networkId.equals(t.getNetworkId()))
                    .map(this::copy)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<TokenMeta> find(String networkId, String tokenAddress) {
        if (networkId == null || tokenAddress == null) {
            return Optional.empty();
        }
        String addr = normalizeAddress(tokenAddress);
        lock.readLock().lock();
        try {
            return tokens.stream()
                    .filter(t -> networkId.equals(t.getNetworkId()) && addr.equals(normalizeAddress(t.getAddress())))
                    .findFirst()
                    .map(this::copy);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void add(TokenMeta token) {
        if (token == null || token.getNetworkId() == null || token.getAddress() == null) {
            return;
        }

        TokenMeta normalized = normalize(token);

        lock.writeLock().lock();
        try {
            List<TokenMeta> next = new ArrayList<>(tokens);
            next.removeIf(t -> normalized.getNetworkId().equals(t.getNetworkId()) &&
                    normalizeAddress(normalized.getAddress()).equals(normalizeAddress(t.getAddress())));
            next.add(normalized);
            next.sort(Comparator.comparing(TokenMeta::getNetworkId).thenComparing(TokenMeta::getSymbol, Comparator.nullsLast(String::compareToIgnoreCase)));
            tokens = next;
        } finally {
            lock.writeLock().unlock();
        }

        persistCustomTokens();
    }

    @Override
    public void remove(String networkId, String tokenAddress) {
        if (networkId == null || tokenAddress == null) {
            return;
        }
        String addr = normalizeAddress(tokenAddress);

        lock.writeLock().lock();
        try {
            List<TokenMeta> next = new ArrayList<>(tokens);
            next.removeIf(t -> networkId.equals(t.getNetworkId()) && addr.equals(normalizeAddress(t.getAddress())));
            tokens = next;
        } finally {
            lock.writeLock().unlock();
        }

        persistCustomTokens();
    }

    private void persistCustomTokens() {
        // Persist only tokens that are NOT in defaults by writing full merged list to custom file.
        // This keeps behavior simple and ensures custom additions survive.
        lock.readLock().lock();
        try {
            Files.createDirectories(customTokensPath.getParent());
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tokens);
            Files.writeString(customTokensPath, json);
        } catch (IOException ignored) {
        } finally {
            lock.readLock().unlock();
        }
    }

    private TokenMeta normalize(TokenMeta token) {
        TokenMeta t = copy(token);
        t.setAddress(normalizeAddress(t.getAddress()));
        return t;
    }

    private String normalizeAddress(String address) {
        return address.trim().toLowerCase(Locale.ROOT);
    }

    private TokenMeta copy(TokenMeta t) {
        return new TokenMeta(t.getNetworkId(), t.getAddress(), t.getName(), t.getSymbol(), t.getDecimals());
    }

    private static Path customPath() {
        String home = System.getProperty("user.home");
        return Paths.get(home, ".openwallet", "tokens.json");
    }
}
