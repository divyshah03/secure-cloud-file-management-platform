package com.cloudfilemanager.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RateLimitFilterTest {

    // Regression test: RateLimitFilter must always be constructible and safe to disable via
    // its own "enabled" flag, NOT via @ConditionalOnProperty - SecurityFilterChainConfig has
    // a hard, unconditional constructor dependency on this bean, so making the bean itself
    // conditionally absent (as an earlier version of this filter did) crashes the entire
    // application on startup whenever rate limiting is turned off. Caught live via
    // docker-compose before it reached Phase 7's load-testing runs.

    @Test
    void disabledFilterPassesRequestThroughWithoutRateLimiting() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(false, 1);
        HttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/files/stats");
        HttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // Limit is 1/minute; disabled filter must let far more than 1 request through.
        for (int i = 0; i < 5; i++) {
            filter.doFilter(request, response, chain);
        }

        verify(chain, org.mockito.Mockito.times(5)).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    @Test
    void enabledFilterBlocksRequestsOverTheLimit() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, 2);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletResponse lastResponse = null;
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/files/stats");
            request.setRemoteAddr("10.0.0.1");
            lastResponse = new MockHttpServletResponse();
            filter.doFilter(request, lastResponse, chain);
        }

        assertThat(lastResponse.getStatus()).isEqualTo(429);
    }
}
