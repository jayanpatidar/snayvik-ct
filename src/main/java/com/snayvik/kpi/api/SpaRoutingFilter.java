package com.snayvik.kpi.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SpaRoutingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (shouldForwardToSpa(request)) {
            request.getRequestDispatcher("/index.html").forward(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldForwardToSpa(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String contextPath = request.getContextPath();
        String path = request.getRequestURI().substring(contextPath.length());

        if (path.startsWith("/api") || path.startsWith("/webhooks") || path.startsWith("/actuator") || path.startsWith("/error")) {
            return false;
        }

        return !path.contains(".");
    }
}
