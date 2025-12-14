package io.openwallet.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openwallet.db.DatabaseConfig;
import io.openwallet.model.NetworkConfig;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

public class NetworkManager {

    private static final String PREF_ACTIVE_NETWORK_ID = "openwallet.activeNetworkId";
    private static final NetworkManager INSTANCE = new NetworkManager();

    private final ObjectMapper mapper = new ObjectMapper();
    private final Preferences prefs = Preferences.userNodeForPackage(NetworkManager.class);

    private volatile List<NetworkConfig> networks = List.of();
    private volatile String activeNetworkId;

    private final Object web3jLock = new Object();
    private volatile Web3j web3j;
    private volatile String web3jRpcUrl;

    private NetworkManager() {
        reloadNetworks();
        this.activeNetworkId = prefs.get(PREF_ACTIVE_NETWORK_ID, defaultNetworkId());
    }

    public static NetworkManager getInstance() {
        return INSTANCE;
    }

    public void reloadNetworks() {
        List<NetworkConfig> loaded = new ArrayList<>();
        try (InputStream in = NetworkManager.class.getClassLoader().getResourceAsStream("networks.json")) {
            if (in != null) {
                loaded = mapper.readValue(in, new TypeReference<List<NetworkConfig>>() {});
            }
        } catch (Exception ignored) {
            // fall back below
        }

        if (loaded == null || loaded.isEmpty()) {
            NetworkConfig fallback = new NetworkConfig();
            fallback.setId("default");
            fallback.setName("Default");
            fallback.setRpcUrl(DatabaseConfig.getRpcUrl());
            loaded = List.of(fallback);
        }

        this.networks = Collections.unmodifiableList(loaded);
    }

    public List<NetworkConfig> getNetworks() {
        return networks;
    }

    public NetworkConfig getActiveNetwork() {
        String id = activeNetworkId;
        Optional<NetworkConfig> match = networks.stream().filter(n -> n.getId() != null && n.getId().equals(id)).findFirst();
        return match.orElseGet(() -> networks.isEmpty() ? null : networks.get(0));
    }

    public String getActiveNetworkId() {
        NetworkConfig active = getActiveNetwork();
        return active != null ? active.getId() : null;
    }

    public void setActiveNetwork(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        this.activeNetworkId = id;
        prefs.put(PREF_ACTIVE_NETWORK_ID, id);
        synchronized (web3jLock) {
            if (web3j != null) {
                try {
                    web3j.shutdown();
                } catch (Exception ignored) {
                }
            }
            web3j = null;
            web3jRpcUrl = null;
        }
    }

    public String getRpcUrl() {
        // env override for quick switching
        String env = System.getenv("OPENWALLET_RPC_URL");
        if (env != null && !env.isBlank()) {
            return env;
        }
        NetworkConfig active = getActiveNetwork();
        if (active != null && active.getRpcUrl() != null && !active.getRpcUrl().isBlank()) {
            return active.getRpcUrl();
        }
        return DatabaseConfig.getRpcUrl();
    }

    public Long getChainId() {
        String env = System.getenv("OPENWALLET_CHAIN_ID");
        if (env != null && !env.isBlank()) {
            try {
                return Long.parseLong(env.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        NetworkConfig active = getActiveNetwork();
        return active != null ? active.getChainId() : null;
    }

    public Web3j getWeb3j() {
        String rpcUrl = getRpcUrl();
        if (web3j != null && rpcUrl != null && rpcUrl.equals(web3jRpcUrl)) {
            return web3j;
        }

        synchronized (web3jLock) {
            rpcUrl = getRpcUrl();
            if (web3j != null && rpcUrl != null && rpcUrl.equals(web3jRpcUrl)) {
                return web3j;
            }
            web3j = Web3j.build(new HttpService(rpcUrl));
            web3jRpcUrl = rpcUrl;
            return web3j;
        }
    }

    private String defaultNetworkId() {
        if (networks.isEmpty()) {
            return "default";
        }
        String id = networks.get(0).getId();
        return id != null ? id : "default";
    }
}
