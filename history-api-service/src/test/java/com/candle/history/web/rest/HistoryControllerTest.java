package com.candle.history.web.rest;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.candle.history.service.HistoryService;
import com.candle.history.web.dto.TvHistoryResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(controllers = HistoryController.class)
class HistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HistoryService historyService;

    @Test
    @DisplayName("TestGetHistoryOkCallsServiceWithConvertedInstants")
    void testGetHistoryOkCallsServiceWithConvertedInstants() throws Exception {
        final var response = TvHistoryResponse.ok(
                List.of(1L),
                List.of(1.0),
                List.of(1.0),
                List.of(1.0),
                List.of(1.0),
                List.of(10L)
        );

        when(historyService.getHistory(
                any(String.class),
                any(String.class),
                any(Instant.class),
                any(Instant.class),
                anyInt()
        )).thenReturn(response);

        mockMvc.perform(get("/api/v1/history/candles")
                        .queryParam("symbol", "BTCUSDT")
                        .queryParam("interval", "1m")
                        .queryParam("from", "100")
                        .queryParam("to", "200")
                        .queryParam("limit", "500")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.s").value("ok"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.t[0]").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.v[0]").value(10));

        verify(historyService).getHistory(
                any(String.class),
                any(String.class),
                any(Instant.class),
                any(Instant.class),
                anyInt()
        );
    }

    @Test
    @DisplayName("TestGetHistoryInvalidRequestReturns400")
    void testGetHistoryInvalidRequestReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/history/candles")
                        .queryParam("symbol", "BTCUSDT")
                        .queryParam("interval", "2m")
                        .queryParam("from", "100")
                        .queryParam("to", "200")
                        .queryParam("limit", "500")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }
}