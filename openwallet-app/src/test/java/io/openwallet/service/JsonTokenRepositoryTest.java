package io.openwallet.service;

import io.openwallet.model.TokenMeta;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JsonTokenRepositoryTest {

    @Test
    void addAndFind_persistsToCustomFile() throws Exception {
        Path dir = Files.createTempDirectory("openwallet-test");
        Path file = dir.resolve("tokens.json");

        JsonTokenRepository repo = new JsonTokenRepository(file);

        TokenMeta token = new TokenMeta("sepolia", "0xabc0000000000000000000000000000000000000", "Test", "TST", 18);
        repo.add(token);

        assertTrue(repo.find("sepolia", token.getAddress()).isPresent());

        JsonTokenRepository repoReloaded = new JsonTokenRepository(file);
        assertTrue(repoReloaded.find("sepolia", token.getAddress()).isPresent());
    }
}
