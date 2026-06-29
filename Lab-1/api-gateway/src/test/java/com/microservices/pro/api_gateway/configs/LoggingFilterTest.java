package com.microservices.pro.api_gateway.configs;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoggingFilterTest {

    private LoggingFilter loggingFilter;

    @Mock
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        loggingFilter = new LoggingFilter();
    }

    @Test
    void filter_shouldLogMethodAndPath() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/products").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        Logger logger = (Logger) LoggerFactory.getLogger(LoggingFilter.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        loggingFilter.filter(exchange, chain).block();

        assertThat(listAppender.list).hasSize(1);
        assertThat(listAppender.list.get(0).getFormattedMessage())
                .contains("[GATEWAY]")
                .contains("GET")
                .contains("/api/products");

        logger.detachAppender(listAppender);
    }

    @Test
    void filter_shouldDelegateToChain() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/orders").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(loggingFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void getOrder_shouldReturnHighestPrecedence() {
        assertThat(loggingFilter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }
}
