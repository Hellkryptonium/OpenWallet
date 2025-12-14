package io.openwallet.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenMeta {
    private String networkId;
    private String address;
    private String name;
    private String symbol;
    private Integer decimals;

    public TokenMeta() {
    }

    public TokenMeta(String networkId, String address, String name, String symbol, Integer decimals) {
        this.networkId = networkId;
        this.address = address;
        this.name = name;
        this.symbol = symbol;
        this.decimals = decimals;
    }

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Integer getDecimals() {
        return decimals;
    }

    public void setDecimals(Integer decimals) {
        this.decimals = decimals;
    }

    @Override
    public String toString() {
        if (symbol != null && !symbol.isBlank()) {
            return symbol + " (" + address + ")";
        }
        return address;
    }
}
