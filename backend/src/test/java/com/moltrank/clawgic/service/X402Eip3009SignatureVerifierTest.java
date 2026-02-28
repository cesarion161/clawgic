package com.moltrank.clawgic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.moltrank.clawgic.config.X402Properties;
import com.moltrank.clawgic.web.X402PaymentRequestException;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.crypto.StructuredDataEncoder;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class X402Eip3009SignatureVerifierTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String PRIVATE_KEY_HEX =
            "0x59c6995e998f97a5a0044976f4f3e6f7f2ee8f87f3d4f3f9127b8fcdab8f5b7d";
    private static final ECKeyPair SIGNER_KEY_PAIR =
            ECKeyPair.create(Numeric.hexStringToByteArray(PRIVATE_KEY_HEX));
    private static final String SIGNER_WALLET =
            "0x" + Keys.getAddress(SIGNER_KEY_PAIR.getPublicKey());
    private static final String TOKEN_ADDRESS = "0x0000000000000000000000000000000000000a11";
    private static final String SETTLEMENT_ADDRESS = "0x0000000000000000000000000000000000000b22";

    @Test
    void verifiesKnownGoodFixture() throws Exception {
        X402Eip3009SignatureVerifier verifier = new X402Eip3009SignatureVerifier(baseProperties());
        long now = Instant.now().getEpochSecond();

        ObjectNode header = buildSignedHeader(
                "req-c33-unit-ok-01",
                "idem-c33-unit-ok-01",
                now - 60,
                now + 600,
                "0x1111111111111111111111111111111111111111111111111111111111111111"
        );

        X402Eip3009SignatureVerifier.VerifiedTransferWithAuthorization verified =
                verifier.verify(header, SIGNER_WALLET, new BigDecimal("5.000000"));

        assertEquals(SIGNER_WALLET, verified.from());
        assertEquals(SETTLEMENT_ADDRESS, verified.to());
        assertEquals(new BigDecimal("5.000000"), verified.amountUsdc());
        assertEquals(84532L, verified.chainId());
        assertEquals(TOKEN_ADDRESS, verified.verifyingContract());
    }

    @Test
    void rejectsBadSignatureFixture() throws Exception {
        X402Eip3009SignatureVerifier verifier = new X402Eip3009SignatureVerifier(baseProperties());
        long now = Instant.now().getEpochSecond();

        ObjectNode header = buildSignedHeader(
                "req-c33-unit-bad-01",
                "idem-c33-unit-bad-01",
                now - 60,
                now + 600,
                "0x2222222222222222222222222222222222222222222222222222222222222222"
        );

        ObjectNode tampered = header.deepCopy();
        ObjectNode authorization = (ObjectNode) tampered.path("payload").path("authorization");
        String signature = authorization.path("signature").asText();
        int tamperIndex = Math.min(10, signature.length() - 1);
        char currentChar = signature.charAt(tamperIndex);
        authorization.put(
                "signature",
                signature.substring(0, tamperIndex)
                        + (currentChar == 'a' ? 'b' : 'a')
                        + signature.substring(tamperIndex + 1)
        );

        X402PaymentRequestException ex = assertThrows(
                X402PaymentRequestException.class,
                () -> verifier.verify(tampered, SIGNER_WALLET, new BigDecimal("5.000000"))
        );

        assertEquals("x402_verification_failed", ex.getCode());
    }

    private static X402Properties baseProperties() {
        X402Properties properties = new X402Properties();
        properties.setEnabled(true);
        properties.setDevBypassEnabled(false);
        properties.setChainId(84532L);
        properties.setTokenAddress(TOKEN_ADDRESS);
        properties.setSettlementAddress(SETTLEMENT_ADDRESS);
        properties.setEip3009DomainName("USD Coin");
        properties.setEip3009DomainVersion("2");
        properties.setTokenDecimals(6);
        return properties;
    }

    private static ObjectNode buildSignedHeader(
            String requestNonce,
            String idempotencyKey,
            long validAfter,
            long validBefore,
            String nonceHex
    ) throws Exception {
        BigInteger valueBaseUnits = BigInteger.valueOf(5_000_000L);
        String signatureHex = signTransferWithAuthorization(
                SIGNER_WALLET,
                SETTLEMENT_ADDRESS,
                valueBaseUnits,
                validAfter,
                validBefore,
                nonceHex
        );

        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("requestNonce", requestNonce);
        root.put("idempotencyKey", idempotencyKey);

        ObjectNode payload = root.putObject("payload");
        payload.put("authorizationNonce", nonceHex);

        ObjectNode domain = payload.putObject("domain");
        domain.put("name", "USD Coin");
        domain.put("version", "2");
        domain.put("chainId", 84532L);
        domain.put("verifyingContract", TOKEN_ADDRESS);

        ObjectNode authorization = payload.putObject("authorization");
        authorization.put("from", SIGNER_WALLET);
        authorization.put("to", SETTLEMENT_ADDRESS);
        authorization.put("value", valueBaseUnits.toString());
        authorization.put("validAfter", validAfter);
        authorization.put("validBefore", validBefore);
        authorization.put("nonce", nonceHex);
        authorization.put("signature", signatureHex);
        return root;
    }

    private static String signTransferWithAuthorization(
            String from,
            String to,
            BigInteger value,
            long validAfter,
            long validBefore,
            String nonceHex
    ) throws Exception {
        ObjectNode typedData = OBJECT_MAPPER.createObjectNode();
        ObjectNode types = typedData.putObject("types");

        ArrayNode eip712Domain = types.putArray("EIP712Domain");
        eip712Domain.add(typeField("name", "string"));
        eip712Domain.add(typeField("version", "string"));
        eip712Domain.add(typeField("chainId", "uint256"));
        eip712Domain.add(typeField("verifyingContract", "address"));

        ArrayNode transfer = types.putArray("TransferWithAuthorization");
        transfer.add(typeField("from", "address"));
        transfer.add(typeField("to", "address"));
        transfer.add(typeField("value", "uint256"));
        transfer.add(typeField("validAfter", "uint256"));
        transfer.add(typeField("validBefore", "uint256"));
        transfer.add(typeField("nonce", "bytes32"));

        typedData.put("primaryType", "TransferWithAuthorization");

        ObjectNode domain = typedData.putObject("domain");
        domain.put("name", "USD Coin");
        domain.put("version", "2");
        domain.put("chainId", 84532L);
        domain.put("verifyingContract", TOKEN_ADDRESS);

        ObjectNode message = typedData.putObject("message");
        message.put("from", from);
        message.put("to", to);
        message.put("value", value.toString());
        message.put("validAfter", String.valueOf(validAfter));
        message.put("validBefore", String.valueOf(validBefore));
        message.put("nonce", nonceHex);

        StructuredDataEncoder encoder = new StructuredDataEncoder(
                OBJECT_MAPPER.writeValueAsString(typedData)
        );
        byte[] digest = encoder.hashStructuredData();
        Sign.SignatureData signatureData = Sign.signMessage(digest, SIGNER_KEY_PAIR, false);
        return signatureDataToHex(signatureData);
    }

    private static ObjectNode typeField(String name, String type) {
        ObjectNode field = OBJECT_MAPPER.createObjectNode();
        field.put("name", name);
        field.put("type", type);
        return field;
    }

    private static String signatureDataToHex(Sign.SignatureData signatureData) {
        byte[] signature = new byte[65];
        System.arraycopy(signatureData.getR(), 0, signature, 0, 32);
        System.arraycopy(signatureData.getS(), 0, signature, 32, 32);
        signature[64] = signatureData.getV()[0];
        return Numeric.toHexString(signature);
    }
}
