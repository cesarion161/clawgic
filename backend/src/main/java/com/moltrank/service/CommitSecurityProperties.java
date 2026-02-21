package com.moltrank.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "moltrank.commit-security")
public class CommitSecurityProperties {

    private static final String DEFAULT_STORAGE_KEY_BASE64 = "bW9sdHJhbmstY29tbWl0LWtleS12MS0zMi1ieXRlcyE=";

    private long maxSignatureAgeSeconds = 300;
    private long maxFutureSkewSeconds = 60;
    private long replayWindowSeconds = 900;
    private boolean allowLegacyUnsignedCommits = false;
    private boolean allowLegacyRevealDecode = true;
    private String activeStorageKeyId = "v1";
    private Map<String, String> storageKeys = new HashMap<>(Map.of("v1", DEFAULT_STORAGE_KEY_BASE64));

}
