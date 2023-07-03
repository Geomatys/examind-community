package org.constellation.engine.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface UnauthorizedHandler {
    /**
     * Handles unauthoriszed request.
     * @param httpServletRequest
     * @param httpServletResponse
     * @return true if response is redirected (ended).
     */
    boolean onUnauthorized(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse);
}
