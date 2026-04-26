package ru.uncledrema.gateway.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import ru.uncledrema.gateway.config.DegradationProperties;
import ru.uncledrema.gateway.config.RouteRule;
import ru.uncledrema.gateway.config.RouteServiceRule;
import ru.uncledrema.gateway.dto.ErrorDto;
import ru.uncledrema.gateway.dto.PrivilegeShortInfoDto;
import ru.uncledrema.gateway.dto.TicketDto;
import ru.uncledrema.gateway.dto.UserInfoDto;
import ru.uncledrema.gateway.services.CircuitBreakerService;
import ru.uncledrema.gateway.services.PrivilegeClient;
import ru.uncledrema.gateway.services.TicketClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ProxyController {
    private static final String API_PREFIX = "/api/v1";

    private final RestTemplate restTemplate;
    private final PrivilegeClient privilegeClient;
    private final TicketClient ticketClient;
    private final DegradationProperties degradationProperties;
    private final CircuitBreakerService circuitBreakerService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BlockingQueue<RequestTask> retryQueue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Value("${downstream.flights:http://localhost:8060}")
    private String flightsBase;

    @Value("${downstream.tickets:http://localhost:8070}")
    private String ticketsBase;

    @Value("${downstream.privileges:http://localhost:8050}")
    private String privilegesBase;

    @Value("${downstream.statistics:http://localhost:8091}")
    private String statisticsBase;

    {
        scheduler.scheduleWithFixedDelay(this::processQueueOnce, 0, 1, TimeUnit.SECONDS);
    }

    @GetMapping("/test-token")
    public ResponseEntity<String> testToken(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(usernameFrom(jwt));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal Jwt jwt) {
        usernameFrom(jwt);
        String routeKey = "/api/v1/me";

        ServiceResult<PrivilegeShortInfoDto> privRes = getWithDegradation(
                routeKey,
                "privileges",
                () -> privilegeClient.getPrivilegeForUser().orElse(null),
                new TypeReference<>() {
                }
        );

        if (privRes.criticalUnavailable) {
            return unavailable("privileges");
        }

        ServiceResult<List<TicketDto>> ticketsRes = getWithDegradation(
                routeKey,
                "tickets",
                () -> ticketClient.getTickets().orElse(null),
                new TypeReference<>() {
                }
        );

        if (ticketsRes.criticalUnavailable) {
            return unavailable("tickets");
        }

        return ResponseEntity.ok(new UserInfoDto(ticketsRes.value, privRes.value));
    }

    @RequestMapping(path = "/**", method = {
            RequestMethod.GET,
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.DELETE,
            RequestMethod.PATCH
    })
    public ResponseEntity<?> proxyAll(
            HttpServletRequest request,
            @RequestHeader(value = "X-Service-Key", required = false) String service,
            @RequestBody(required = false) byte[] body
    ) {
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        String fullPath = request.getRequestURI();
        String forwardPath = fullPath.startsWith(API_PREFIX) ? fullPath.substring(API_PREFIX.length()) : fullPath;
        String query = request.getQueryString();
        String targetBase = selectTarget(forwardPath);
        String serviceKey = service == null ? serviceKeyFor(forwardPath) : service;
        String routeKey = API_PREFIX + forwardPath;

        if (targetBase == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(("No downstream service for path: " + forwardPath).getBytes(StandardCharsets.UTF_8));
        }

        RouteRule routeRule = degradationProperties.getRouteRuleByPath(method, routeKey);
        RouteServiceRule routeServiceRule = routeRule.getRuleFor(serviceKey);

        try {
            circuitBreakerService.beforeCall(serviceKey);
        } catch (CircuitOpenException ignored) {
            return degradedResponse(forwardPath, serviceKey, routeServiceRule);
        }

        String url = targetBase + forwardPath + (query != null ? "?" + query : "");
        HttpHeaders headers = copyRequestHeaders(request);
        HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<byte[]> resp = restTemplate.exchange(URI.create(url), method, entity, byte[].class);
            circuitBreakerService.recordSuccess(serviceKey);
            return new ResponseEntity<>(resp.getBody(), filterResponseHeaders(resp.getHeaders()), resp.getStatusCode());
        } catch (HttpStatusCodeException ex) {
            circuitBreakerService.recordFailure(serviceKey);
            return new ResponseEntity<>(
                    ex.getResponseBodyAsByteArray(),
                    filterResponseHeaders(ex.getResponseHeaders()),
                    ex.getStatusCode()
            );
        } catch (Exception ex) {
            log.warn("Downstream call failed for {} {}: {}", method, url, ex.getMessage());
            circuitBreakerService.recordFailure(serviceKey);
            if (!routeServiceRule.isCritical() && routeServiceRule.isEnqueueOnFailure()) {
                retryQueue.offer(new RequestTask(url, method, copyRequestHeaders(request), body, serviceKey));
            }
            return degradedResponse(forwardPath, serviceKey, routeServiceRule);
        }
    }

    private <T> ServiceResult<T> getWithDegradation(
            String routeKey,
            String serviceKey,
            Supplier<T> supplier,
            TypeReference<T> typeRef
    ) {
        RouteRule routeRule = degradationProperties.getRouteRuleByPath(HttpMethod.GET, routeKey);
        RouteServiceRule routeServiceRule = routeRule.getRuleFor(serviceKey);

        try {
            circuitBreakerService.beforeCall(serviceKey);
            T val = supplier.get();
            circuitBreakerService.recordSuccess(serviceKey);
            return ServiceResult.ok(val);
        } catch (CircuitOpenException ex) {
            return fallbackOrCritical(routeKey, serviceKey, routeServiceRule, typeRef);
        } catch (Exception ex) {
            circuitBreakerService.recordFailure(serviceKey);
            return fallbackOrCritical(routeKey, serviceKey, routeServiceRule, typeRef);
        }
    }

    private <T> ServiceResult<T> fallbackOrCritical(
            String routeKey,
            String serviceKey,
            RouteServiceRule routeServiceRule,
            TypeReference<T> typeRef
    ) {
        if (routeServiceRule.isCritical()) {
            return ServiceResult.criticalUnavailable();
        }
        String fallback = routeServiceRule.getFallback();
        if (fallback == null || fallback.isBlank()) {
            return ServiceResult.fallback(null);
        }
        try {
            return ServiceResult.fallback(objectMapper.readValue(fallback, typeRef));
        } catch (Exception e) {
            log.warn("Failed to parse fallback for {}/{}: {}", routeKey, serviceKey, e.getMessage());
            return ServiceResult.fallback(null);
        }
    }

    private ResponseEntity<?> degradedResponse(String forwardPath, String serviceKey, RouteServiceRule routeServiceRule) {
        if (routeServiceRule.isCritical()) {
            return unavailable(serviceKeyFor(forwardPath));
        }
        String fallback = routeServiceRule.getFallback();
        byte[] bodyBytes = fallback != null ? fallback.getBytes(StandardCharsets.UTF_8) : new byte[0];
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(bodyBytes, headers, HttpStatus.OK);
    }

    private ResponseEntity<ErrorDto> unavailable(String serviceKey) {
        String msg = getServiceDisplayName(serviceKey) + " unavailable";
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorDto(msg));
    }

    private void processQueueOnce() {
        RequestTask task = retryQueue.poll();
        if (task == null) {
            return;
        }
        try {
            HttpEntity<byte[]> entity = new HttpEntity<>(task.body, task.headers);
            restTemplate.exchange(URI.create(task.url), task.method, entity, byte[].class);
            circuitBreakerService.recordSuccess(task.serviceKey);
        } catch (Exception e) {
            log.warn("Retry task failed for url={}, service={}, will retry in 5s: {}", task.url, task.serviceKey, e.getMessage());
            circuitBreakerService.recordFailure(task.serviceKey);
            scheduler.schedule(() -> retryQueue.offer(task), 5, TimeUnit.SECONDS);
        }
    }

    private String serviceKeyFor(String forwardPath) {
        String p = forwardPath.toLowerCase();
        if (p.startsWith("/flights") || p.startsWith("/airports")) {
            return "flights";
        }
        if (p.startsWith("/tickets")) {
            return "tickets";
        }
        if (p.startsWith("/privilege") || p.startsWith("/privileges")) {
            return "privileges";
        }
        if (p.startsWith("/statistics")) {
            return "statistics";
        }
        return "unknown";
    }

    @Nullable
    private String selectTarget(String forwardPath) {
        String p = forwardPath.toLowerCase();
        if (p.startsWith("/flights") || p.startsWith("/airports")) {
            return flightsBase;
        }
        if (p.startsWith("/tickets")) {
            return ticketsBase;
        }
        if (p.startsWith("/privilege") || p.startsWith("/privileges")) {
            return privilegesBase;
        }
        if (p.startsWith("/statistics")) {
            return statisticsBase;
        }
        return null;
    }

    private String getServiceDisplayName(String serviceKey) {
        var rule = degradationProperties.getServiceRule(serviceKey);
        if (rule != null && rule.getDisplayName() != null && !rule.getDisplayName().isBlank()) {
            return rule.getDisplayName();
        }
        if (serviceKey == null || serviceKey.isBlank()) {
            return "Service";
        }
        return Character.toUpperCase(serviceKey.charAt(0)) + serviceKey.substring(1);
    }

    private HttpHeaders copyRequestHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            if ("host".equalsIgnoreCase(name) || "content-length".equalsIgnoreCase(name)) {
                continue;
            }
            Enumeration<String> values = request.getHeaders(name);
            while (values.hasMoreElements()) {
                headers.add(name, values.nextElement());
            }
        }
        return headers;
    }

    private HttpHeaders filterResponseHeaders(@Nullable HttpHeaders in) {
        HttpHeaders out = new HttpHeaders();
        if (in == null) {
            return out;
        }
        in.forEach((k, v) -> {
            if (!"transfer-encoding".equalsIgnoreCase(k) && !"connection".equalsIgnoreCase(k)) {
                out.put(k, v);
            }
        });
        return out;
    }

    private String usernameFrom(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return username != null && !username.isBlank() ? username : jwt.getSubject();
    }

    private static class ServiceResult<T> {
        final boolean criticalUnavailable;
        final T value;

        private ServiceResult(boolean criticalUnavailable, T value) {
            this.criticalUnavailable = criticalUnavailable;
            this.value = value;
        }

        static <T> ServiceResult<T> ok(T v) {
            return new ServiceResult<>(false, v);
        }

        static <T> ServiceResult<T> fallback(T v) {
            return new ServiceResult<>(false, v);
        }

        static <T> ServiceResult<T> criticalUnavailable() {
            return new ServiceResult<>(true, null);
        }
    }

    private static class RequestTask {
        final String url;
        final HttpMethod method;
        final HttpHeaders headers;
        final byte[] body;
        final String serviceKey;

        RequestTask(String url, HttpMethod method, HttpHeaders headers, byte[] body, String serviceKey) {
            this.url = url;
            this.method = method;
            this.headers = headers;
            this.body = body;
            this.serviceKey = serviceKey;
        }
    }
}
