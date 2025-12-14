package io.openwallet.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NetworkConfig {
    private String id;
    private String name;
    private String rpcUrl;
    private Long chainId;
    private String bridgeUrl;
    private List<BridgeLink> bridges;
    private Map<String, String> chainlinkFeeds;

    public NetworkConfig() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRpcUrl() {
        return rpcUrl;
    }

    public void setRpcUrl(String rpcUrl) {
        this.rpcUrl = rpcUrl;
    }

    public Long getChainId() {
        return chainId;
    }

    public void setChainId(Long chainId) {
        this.chainId = chainId;
    }

    public String getBridgeUrl() {
        return bridgeUrl;
    }

    public void setBridgeUrl(String bridgeUrl) {
        this.bridgeUrl = bridgeUrl;
    }

    public List<BridgeLink> getBridges() {
        return bridges;
    }

    public void setBridges(List<BridgeLink> bridges) {
        this.bridges = bridges;
    }

    /**
     * Returns a non-null list of bridge links.
     * If "bridges" is empty/missing, falls back to legacy "bridgeUrl".
     */
    public List<BridgeLink> getBridgeLinks() {
        if (bridges != null && !bridges.isEmpty()) {
            return bridges;
        }
        List<BridgeLink> fallback = new ArrayList<>();
        if (bridgeUrl != null && !bridgeUrl.isBlank()) {
            fallback.add(new BridgeLink("Bridge", bridgeUrl));
        }
        return fallback;
    }

    public Map<String, String> getChainlinkFeeds() {
        return chainlinkFeeds;
    }

    public void setChainlinkFeeds(Map<String, String> chainlinkFeeds) {
        this.chainlinkFeeds = chainlinkFeeds;
    }

    @Override
    public String toString() {
        return name != null ? name : id;
    }
}
