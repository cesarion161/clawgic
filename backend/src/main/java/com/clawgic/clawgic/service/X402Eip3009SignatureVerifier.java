package com.clawgic.clawgic.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.clawgic.clawgic.config.X402Properties;
import com.clawgic.clawgic.web.X402PaymentRequestException;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Minimal EIP-3009 TransferWithAuthorization verifier for x402 tournament entry.
 */
@Component
public class X402Eip3009SignatureVerifier {

    private static final byte[] EIP712_DOMAIN_TYPEHASH = keccakUtf8(
            "EIP712Domain(string name,string version,uint256 chainId,address verifyingContract)"
    );
    private static final byte[] TRANSFER_WITH_AUTHORIZATION_TYPEHASH = keccakUtf8(
            "TransferWithAuthorization(address from,address to,uint256 value,uint256 validAfter,uint256 validBefore,bytes32 nonce)"
    );
    private static final Pattern HEX_40 = Pattern.compile("^[0-9a-fA-F]{40}$");
    private static final Pattern HEX_64 = Pattern.compile("^[0-9a-fA-F]{64}$");

    private final X402Properties x402Properties;

    public X402Eip3009SignatureVerifier(X402Properties x402Properties) {
        this.x402Properties = x402Properties;
    }

    public VerifiedTransferWithAuthorization verify(
            JsonNode paymentHeaderJson,
            String expectedWalletAddress,
            BigDecimal expectedAmountUsdc
    ) {
        JsonNode root = requireObject(paymentHeaderJson, "X-PAYMENT header JSON object is required");
        JsonNode payload = objectNode(root.path("payload"));
        JsonNode authorization = resolveAuthorizationNode(root, payload);
        JsonNode domain = resolveDomainNode(root, payload);

        String from = readRequiredAddress(authorization, "from");
        String to = readRequiredAddress(authorization, "to", "recipient");
        BigInteger value = readRequiredUnsignedInteger(authorization, "value", "amount");
        long validAfter = readRequiredEpochSeconds(authorization, "validAfter");
        long validBefore = readRequiredEpochSeconds(authorization, "validBefore");
        byte[] nonceBytes = readRequiredBytes32(authorization, "nonce", "authorizationNonce");
        Sign.SignatureData signatureData = readRequiredSignature(authorization, payload, root);

        String domainName = firstNonBlankString(domain, "name");
        if (domainName == null) {
            domainName = x402Properties.getEip3009DomainName();
        }

        String domainVersion = firstNonBlankString(domain, "version");
        if (domainVersion == null) {
            domainVersion = x402Properties.getEip3009DomainVersion();
        }

        Long parsedChainId = firstLong(domain, "chainId");
        if (parsedChainId == null) {
            parsedChainId = firstLong(payload, "chainId");
        }
        if (parsedChainId == null) {
            parsedChainId = firstLong(root, "chainId");
        }
        long chainId = parsedChainId != null ? parsedChainId : x402Properties.getChainId();

        String verifyingContract = firstAddress(domain, "verifyingContract", "tokenAddress");
        if (verifyingContract == null) {
            verifyingContract = firstAddress(payload, "tokenAddress");
        }
        if (verifyingContract == null) {
            verifyingContract = firstAddress(root, "tokenAddress");
        }
        if (verifyingContract == null) {
            verifyingContract = normalizeAddress(
                    x402Properties.getTokenAddress(),
                    "x402.token-address"
            );
        }

        String expectedSigner = normalizeAddress(expectedWalletAddress, "walletAddress");
        String expectedRecipient = normalizeAddress(
                x402Properties.getSettlementAddress(),
                "x402.settlement-address"
        );
        String expectedTokenAddress = normalizeAddress(
                x402Properties.getTokenAddress(),
                "x402.token-address"
        );

        if (!from.equals(expectedSigner)) {
            throw X402PaymentRequestException.verificationFailed(
                    "EIP-3009 signer does not match agent wallet"
            );
        }
        if (!to.equals(expectedRecipient)) {
            throw X402PaymentRequestException.verificationFailed(
                    "EIP-3009 recipient does not match configured settlement address"
            );
        }
        if (chainId != x402Properties.getChainId()) {
            throw X402PaymentRequestException.verificationFailed(
                    "EIP-3009 chainId does not match configured chain"
            );
        }
        if (!verifyingContract.equals(expectedTokenAddress)) {
            throw X402PaymentRequestException.verificationFailed(
                    "EIP-3009 verifying contract does not match configured token address"
            );
        }

        BigInteger expectedValue = toTokenUnits(expectedAmountUsdc, x402Properties.getTokenDecimals());
        if (!value.equals(expectedValue)) {
            throw X402PaymentRequestException.verificationFailed(
                    "EIP-3009 transfer value does not match tournament entry fee"
            );
        }
        if (validBefore <= validAfter) {
            throw X402PaymentRequestException.verificationFailed(
                    "EIP-3009 validity window is invalid"
            );
        }

        long nowEpochSeconds = Instant.now().getEpochSecond();
        if (nowEpochSeconds <= validAfter) {
            throw X402PaymentRequestException.verificationFailed(
                    "EIP-3009 authorization is not yet valid"
            );
        }
        if (nowEpochSeconds >= validBefore) {
            throw X402PaymentRequestException.verificationFailed(
                    "EIP-3009 authorization has expired"
            );
        }

        byte[] domainSeparator = Hash.sha3(concat(
                EIP712_DOMAIN_TYPEHASH,
                keccakUtf8(domainName),
                keccakUtf8(domainVersion),
                encodeUint256(BigInteger.valueOf(chainId)),
                encodeAddress(verifyingContract)
        ));

        byte[] transferHash = Hash.sha3(concat(
                TRANSFER_WITH_AUTHORIZATION_TYPEHASH,
                encodeAddress(from),
                encodeAddress(to),
                encodeUint256(value),
                encodeUint256(BigInteger.valueOf(validAfter)),
                encodeUint256(BigInteger.valueOf(validBefore)),
                nonceBytes
        ));

        byte[] digest = Hash.sha3(concat(
                new byte[]{0x19, 0x01},
                domainSeparator,
                transferHash
        ));

        String recoveredSigner = recoverSignerAddress(digest, signatureData);
        if (!recoveredSigner.equals(from)) {
            throw X402PaymentRequestException.verificationFailed(
                    "EIP-3009 signature recovery failed"
            );
        }

        return new VerifiedTransferWithAuthorization(
                from,
                to,
                value,
                toUsdc(value, x402Properties.getTokenDecimals()),
                validAfter,
                validBefore,
                Numeric.toHexString(nonceBytes).toLowerCase(Locale.ROOT),
                chainId,
                verifyingContract,
                domainName,
                domainVersion,
                recoveredSigner
        );
    }

    private JsonNode resolveAuthorizationNode(JsonNode root, JsonNode payload) {
        JsonNode[] candidates = {
                objectNode(payload.path("authorization")),
                objectNode(payload.path("transferWithAuthorization")),
                objectNode(root.path("authorization")),
                objectNode(root.path("transferWithAuthorization")),
                objectNode(payload),
                objectNode(root)
        };

        for (JsonNode candidate : candidates) {
            if (candidate != null && hasAuthorizationShape(candidate)) {
                return candidate;
            }
        }

        throw X402PaymentRequestException.malformedHeader(
                "X-PAYMENT payload must include TransferWithAuthorization fields"
        );
    }

    private JsonNode resolveDomainNode(JsonNode root, JsonNode payload) {
        JsonNode payloadDomain = objectNode(payload.path("domain"));
        if (payloadDomain != null) {
            return payloadDomain;
        }
        JsonNode rootDomain = objectNode(root.path("domain"));
        if (rootDomain != null) {
            return rootDomain;
        }
        return MissingNode.getInstance();
    }

    private boolean hasAuthorizationShape(JsonNode node) {
        return firstNonBlankString(node, "from") != null
                || firstNonBlankString(node, "value", "amount") != null
                || firstNonBlankString(node, "signature") != null;
    }

    private String recoverSignerAddress(byte[] digest, Sign.SignatureData signatureData) {
        try {
            BigInteger recoveredKey = Sign.signedMessageHashToKey(digest, signatureData);
            return "0x" + Keys.getAddress(recoveredKey).toLowerCase(Locale.ROOT);
        } catch (SignatureException | RuntimeException ex) {
            throw X402PaymentRequestException.verificationFailed("Invalid EIP-3009 signature");
        }
    }

    private Sign.SignatureData readRequiredSignature(JsonNode... nodes) {
        String signatureHex = firstNonBlankString(nodes, "signature");
        if (signatureHex == null) {
            throw X402PaymentRequestException.malformedHeader(
                    "X-PAYMENT signature is required"
            );
        }

        String normalized = Numeric.cleanHexPrefix(signatureHex.trim());
        if (normalized.length() != 130 || !normalized.matches("^[0-9a-fA-F]{130}$")) {
            throw X402PaymentRequestException.malformedHeader(
                    "X-PAYMENT.signature must be a 65-byte hex string"
            );
        }

        byte[] allBytes = Numeric.hexStringToByteArray("0x" + normalized);
        byte[] r = Arrays.copyOfRange(allBytes, 0, 32);
        byte[] s = Arrays.copyOfRange(allBytes, 32, 64);
        byte v = allBytes[64];
        if (v < 27) {
            v = (byte) (v + 27);
        }
        if (v != 27 && v != 28) {
            throw X402PaymentRequestException.malformedHeader(
                    "X-PAYMENT.signature recovery id must be 27/28 or 0/1"
            );
        }

        return new Sign.SignatureData(v, r, s);
    }

    private String readRequiredAddress(JsonNode node, String... fieldNames) {
        String raw = readRequiredString(node, fieldNames);
        return normalizeAddress(raw, "X-PAYMENT." + fieldNames[0]);
    }

    private String firstAddress(JsonNode node, String... fieldNames) {
        String value = firstNonBlankString(node, fieldNames);
        if (value == null) {
            return null;
        }
        return normalizeAddress(value, "X-PAYMENT." + fieldNames[0]);
    }

    private String normalizeAddress(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw X402PaymentRequestException.malformedHeader(fieldName + " is required");
        }
        String clean = Numeric.cleanHexPrefix(value.trim());
        if (!HEX_40.matcher(clean).matches()) {
            throw X402PaymentRequestException.malformedHeader(fieldName + " must be a 20-byte hex address");
        }
        return "0x" + clean.toLowerCase(Locale.ROOT);
    }

    private BigInteger readRequiredUnsignedInteger(JsonNode node, String... fieldNames) {
        String raw = readRequiredString(node, fieldNames);
        try {
            BigInteger parsed = new BigInteger(raw);
            if (parsed.signum() < 0) {
                throw X402PaymentRequestException.malformedHeader(
                        "X-PAYMENT." + fieldNames[0] + " must be non-negative"
                );
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw X402PaymentRequestException.malformedHeader(
                    "X-PAYMENT." + fieldNames[0] + " must be an unsigned integer"
            );
        }
    }

    private long readRequiredEpochSeconds(JsonNode node, String fieldName) {
        BigInteger raw = readRequiredUnsignedInteger(node, fieldName);
        if (raw.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
            throw X402PaymentRequestException.malformedHeader(
                    "X-PAYMENT." + fieldName + " is too large"
            );
        }
        return raw.longValueExact();
    }

    private byte[] readRequiredBytes32(JsonNode node, String... fieldNames) {
        String raw = readRequiredString(node, fieldNames);
        String clean = Numeric.cleanHexPrefix(raw);
        if (!HEX_64.matcher(clean).matches()) {
            throw X402PaymentRequestException.malformedHeader(
                    "X-PAYMENT." + fieldNames[0] + " must be a 32-byte hex value"
            );
        }
        return Numeric.hexStringToByteArray("0x" + clean);
    }

    private String readRequiredString(JsonNode node, String... fieldNames) {
        String value = firstNonBlankString(node, fieldNames);
        if (value == null) {
            throw X402PaymentRequestException.malformedHeader(
                    "X-PAYMENT." + fieldNames[0] + " is required"
            );
        }
        return value;
    }

    private Long firstLong(JsonNode node, String... fieldNames) {
        String raw = firstNonBlankString(node, fieldNames);
        if (raw == null) {
            return null;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            throw X402PaymentRequestException.malformedHeader(
                    "X-PAYMENT." + fieldNames[0] + " must be an integer"
            );
        }
    }

    private static JsonNode requireObject(JsonNode node, String message) {
        JsonNode objectNode = objectNode(node);
        if (objectNode == null) {
            throw X402PaymentRequestException.malformedHeader(message);
        }
        return objectNode;
    }

    private static JsonNode objectNode(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (!node.isObject()) {
            return null;
        }
        return node;
    }

    private static String firstNonBlankString(JsonNode node, String... fieldNames) {
        return firstNonBlankString(new JsonNode[]{node}, fieldNames);
    }

    private static String firstNonBlankString(JsonNode[] nodes, String... fieldNames) {
        for (JsonNode node : nodes) {
            if (node == null || !node.isObject()) {
                continue;
            }
            for (String fieldName : fieldNames) {
                JsonNode fieldNode = node.path(fieldName);
                if (fieldNode.isMissingNode() || fieldNode.isNull()) {
                    continue;
                }
                if (fieldNode.isTextual()) {
                    String trimmed = fieldNode.asText().trim();
                    if (!trimmed.isEmpty()) {
                        return trimmed;
                    }
                    continue;
                }
                if (fieldNode.isNumber()) {
                    return fieldNode.asText();
                }
                throw X402PaymentRequestException.malformedHeader(
                        "X-PAYMENT." + fieldName + " must be a string or number"
                );
            }
        }
        return null;
    }

    private static BigInteger toTokenUnits(BigDecimal amountUsdc, int tokenDecimals) {
        if (tokenDecimals < 0) {
            throw X402PaymentRequestException.verificationFailed(
                    "x402.token-decimals must be non-negative"
            );
        }
        try {
            return amountUsdc
                    .setScale(tokenDecimals, RoundingMode.UNNECESSARY)
                    .movePointRight(tokenDecimals)
                    .toBigIntegerExact();
        } catch (ArithmeticException ex) {
            throw X402PaymentRequestException.verificationFailed(
                    "Configured tournament fee cannot be represented in token base units"
            );
        }
    }

    private static BigDecimal toUsdc(BigInteger baseUnits, int tokenDecimals) {
        return new BigDecimal(baseUnits)
                .movePointLeft(tokenDecimals)
                .setScale(tokenDecimals, RoundingMode.UNNECESSARY);
    }

    private static byte[] encodeUint256(BigInteger value) {
        return Numeric.toBytesPadded(value, 32);
    }

    private static byte[] encodeAddress(String normalizedAddress) {
        String clean = Numeric.cleanHexPrefix(normalizedAddress);
        return Numeric.toBytesPadded(new BigInteger(clean, 16), 32);
    }

    private static byte[] keccakUtf8(String value) {
        return Hash.sha3(value.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] concat(byte[]... arrays) {
        int totalLength = 0;
        for (byte[] array : arrays) {
            totalLength += array.length;
        }

        byte[] out = new byte[totalLength];
        int offset = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, out, offset, array.length);
            offset += array.length;
        }
        return out;
    }

    public record VerifiedTransferWithAuthorization(
            String from,
            String to,
            BigInteger valueBaseUnits,
            BigDecimal amountUsdc,
            long validAfter,
            long validBefore,
            String nonceHex,
            long chainId,
            String verifyingContract,
            String domainName,
            String domainVersion,
            String recoveredSigner
    ) {
    }
}
