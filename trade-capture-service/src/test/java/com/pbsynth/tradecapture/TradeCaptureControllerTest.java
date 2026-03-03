package com.pbsynth.tradecapture;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TradeCaptureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void captureTradeRequiresHeadersAndValidBody() throws Exception {
        String body = """
                {
                  "sourceSystem":"AMS",
                  "tradeExternalId":"TRD-CNTR-1",
                  "accountId":"ACC1",
                  "bookId":"BOOK1",
                  "securityId":"AAPL",
                  "direction":"BUY",
                  "quantity":1000,
                  "price":120.5,
                  "tradeDate":"2026-01-15"
                }
                """;
        mockMvc.perform(post("/trade-capture-service/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "hdr-idem-1")
                        .header("X-Correlation-Id", "hdr-corr-1")
                        .content(body))
                .andExpect(status().isAccepted());
    }
}
