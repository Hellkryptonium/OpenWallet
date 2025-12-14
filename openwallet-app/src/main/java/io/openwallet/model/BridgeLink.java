package io.openwallet.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BridgeLink {
    private String name;
    private String url;

    public BridgeLink() {
    }

    public BridgeLink(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        if (name != null && !name.isBlank()) {
            return name;
        }
        return url != null ? url : "";
    }
}
