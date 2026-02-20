// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.security;

import energy.eddie.regionconnector.shared.exceptions.JwtCreationFailedException;
import energy.eddie.regionconnector.shared.security.JwtUtil;
import jakarta.annotation.Nullable;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;

@Component
@SuppressWarnings("NullableProblems")
public class JwtIssuerFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtIssuerFilter.class);
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;

    public JwtIssuerFilter(ObjectMapper objectMapper, JwtUtil jwtUtil) {
        this.objectMapper = objectMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var requestURI = request.getRequestURI();
        LOGGER.debug("Got hit for {}", requestURI);
        return !requestURI.endsWith(PATH_PERMISSION_REQUEST) || !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        var wrappedResponse = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, wrappedResponse);
        var status = wrappedResponse.getStatus();
        LOGGER.debug("Trying to apply JWT for {} with status {}", request.getRequestURI(), status);

        var rcId = getRegionConnectorId(request);

        if (status != HttpServletResponse.SC_CREATED || rcId == null) {
            wrappedResponse.copyBodyToResponse();
            LOGGER.debug("No JWT was created for {}", request.getRequestURI());
            return;
        }
        LOGGER.info("Creating JWT for {}", request.getRequestURI());

        var objectNode = getJsonNodes(request, wrappedResponse);
        if (objectNode == null) {
            wrappedResponse.copyBodyToResponse();
            return;
        }

        var permissionId = objectNode.get("permissionId");
        if (permissionId != null && permissionId.isString()) {
            try {
                String token = jwtUtil.createJwt(rcId, permissionId.asString());
                objectNode.put("bearerToken", token);
            } catch (JwtCreationFailedException e) {
                throw new ServletException("JWT creation failed", e);
            }
            wrappedResponse.resetBuffer();
            wrappedResponse.getWriter().write(objectMapper.writeValueAsString(objectNode));
        }

        var permissionIds = objectNode.get("permissionIds");
        if (permissionIds != null && permissionIds.isArray()) {
            var ids = new ArrayList<String>();
            for (var node : permissionIds) {
                if (node.isString()) {
                    ids.add(node.asString());
                }
            }

            try {
                String token = jwtUtil.createJwt(rcId, ids.toArray(String[]::new));
                objectNode.put("bearerToken", token);
            } catch (JwtCreationFailedException e) {
                throw new ServletException("JWT creation failed", e);
            }
            wrappedResponse.resetBuffer();
            wrappedResponse.getWriter().write(objectMapper.writeValueAsString(objectNode));
        }

        wrappedResponse.copyBodyToResponse();
    }

    @Nullable
    private ObjectNode getJsonNodes(
            HttpServletRequest request,
            ContentCachingResponseWrapper wrappedResponse
    ) {
        byte[] content = wrappedResponse.getContentAsByteArray();

        if (content.length == 0) {
            LOGGER.warn("Empty response body for {}", request.getRequestURI());
            return null;
        }

        try {
            var createdPermissionRequest = objectMapper.readTree(content);
            if (!(createdPermissionRequest instanceof ObjectNode objectNode)) {
                LOGGER.warn("Invalid response object for endpoint {}", request.getRequestURI());
                return null;
            }
            return objectNode;
        } catch (JacksonException e) {
            LOGGER.warn("Invalid response body for {}", request.getRequestURI(), e);
            return null;
        }
    }

    @Nullable
    private static String getRegionConnectorId(HttpServletRequest request) {
        var uri = request.getRequestURI();
        var path = Path.of(uri);
        var rc = path.getParent();
        if (rc == null || rc.getFileName() == null) {
            LOGGER.warn("No region connector ID found for {}", uri);
            return null;
        }
        return rc.getFileName().toString();
    }
}