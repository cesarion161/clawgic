package com.clawgic.clawgic.web;

import com.clawgic.clawgic.config.X402Properties;
import com.clawgic.clawgic.controller.ClawgicTournamentController;
import com.clawgic.clawgic.dto.ClawgicTournamentResponses;
import com.clawgic.clawgic.model.ClawgicTournamentEntryStatus;
import com.clawgic.clawgic.service.ClawgicTournamentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClawgicTournamentController.class)
@Import({X402PaymentRequiredInterceptorConfig.class, X402Properties.class})
class X402PaymentRequiredInterceptorWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private X402Properties x402Properties;

    @MockitoBean
    private ClawgicTournamentService clawgicTournamentService;

    @BeforeEach
    void setUpDefaults() {
        x402Properties.setEnabled(false);
        x402Properties.setDevBypassEnabled(true);
        x402Properties.setNetwork("base-sepolia");
        x402Properties.setChainId(84532L);
        x402Properties.setSettlementAddress("0x0000000000000000000000000000000000000000");
        x402Properties.setTokenAddress("0x0000000000000000000000000000000000000000");
        x402Properties.setDefaultEntryFeeUsdc(new BigDecimal("5.00"));
        x402Properties.setPaymentHeaderName("X-PAYMENT");
        x402Properties.setNonceTtlSeconds(300L);
    }

    @Test
    void unpaidEntryRequestReturnsStructuredPaymentRequiredChallenge() throws Exception {
        UUID tournamentId = UUID.fromString("00000000-0000-0000-0000-000000000501");

        x402Properties.setEnabled(true);
        x402Properties.setDevBypassEnabled(false);
        x402Properties.setSettlementAddress("0x9999999999999999999999999999999999999999");
        x402Properties.setDefaultEntryFeeUsdc(new BigDecimal("7.25"));

        mockMvc.perform(post("/api/clawgic/tournaments/{tournamentId}/enter", tournamentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "00000000-0000-0000-0000-000000000511"
                                }
                                """))
                .andExpect(status().isPaymentRequired())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.scheme").value("x402"))
                .andExpect(jsonPath("$.priceUsdc").value(7.25))
                .andExpect(jsonPath("$.recipient").value("0x9999999999999999999999999999999999999999"))
                .andExpect(jsonPath("$.paymentHeader").value("X-PAYMENT"))
                .andExpect(jsonPath("$.nonce").isString())
                .andExpect(jsonPath("$.challengeExpiresAt").isString());

        verify(clawgicTournamentService, never()).enterTournament(any(), any(), anyString());
    }

    @Test
    void bypassModeAllowsEntryRequestThroughToController() throws Exception {
        UUID tournamentId = UUID.fromString("00000000-0000-0000-0000-000000000502");
        UUID agentId = UUID.fromString("00000000-0000-0000-0000-000000000512");

        when(clawgicTournamentService.enterTournament(any(), any(), any()))
                .thenReturn(new ClawgicTournamentResponses.TournamentEntry(
                        UUID.fromString("00000000-0000-0000-0000-000000000513"),
                        tournamentId,
                        agentId,
                        "0x1111111111111111111111111111111111111111",
                        ClawgicTournamentEntryStatus.CONFIRMED,
                        null,
                        1000,
                        OffsetDateTime.parse("2026-06-01T10:00:00Z"),
                        OffsetDateTime.parse("2026-06-01T10:00:00Z")
                ));

        mockMvc.perform(post("/api/clawgic/tournaments/{tournamentId}/enter", tournamentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "00000000-0000-0000-0000-000000000512"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tournamentId").value(tournamentId.toString()))
                .andExpect(jsonPath("$.agentId").value(agentId.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void malformedPaymentHeaderReturnsExplicitErrorPayload() throws Exception {
        UUID tournamentId = UUID.fromString("00000000-0000-0000-0000-000000000503");

        x402Properties.setEnabled(true);
        x402Properties.setDevBypassEnabled(false);

        when(clawgicTournamentService.enterTournament(any(), any(), any()))
                .thenThrow(X402PaymentRequestException.malformedHeader("X-PAYMENT header must be valid JSON"));

        mockMvc.perform(post("/api/clawgic/tournaments/{tournamentId}/enter", tournamentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-PAYMENT", "not-json")
                        .content("""
                                {
                                  "agentId": "00000000-0000-0000-0000-000000000511"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("x402_malformed_payment_header"))
                .andExpect(jsonPath("$.message").value("X-PAYMENT header must be valid JSON"));
    }
}
