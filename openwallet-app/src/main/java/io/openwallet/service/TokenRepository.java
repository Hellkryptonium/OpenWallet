package io.openwallet.service;

import io.openwallet.model.TokenMeta;

import java.util.List;
import java.util.Optional;

public interface TokenRepository {
    List<TokenMeta> listByNetwork(String networkId);

    Optional<TokenMeta> find(String networkId, String tokenAddress);

    void add(TokenMeta token);

    void remove(String networkId, String tokenAddress);
}
