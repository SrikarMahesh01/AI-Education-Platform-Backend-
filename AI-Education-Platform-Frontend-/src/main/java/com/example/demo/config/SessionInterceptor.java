package com.example.demo.config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

public class SessionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Add cache control headers
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        // Get current authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Check if user is authenticated
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            HttpSession session = request.getSession(false);
            
            // Check if session exists and is valid
            if (session == null) {
                response.sendRedirect(request.getContextPath() + "/login?expired");
                return false;
            }

            // Check if session has been inactive for too long
            long lastAccessedTime = session.getLastAccessedTime();
            long currentTime = System.currentTimeMillis();
            long maxInactiveInterval = session.getMaxInactiveInterval() * 1000L;

            if ((currentTime - lastAccessedTime) > maxInactiveInterval) {
                session.invalidate();
                response.sendRedirect(request.getContextPath() + "/login?timeout");
                return false;
            }

            // Update session last accessed time
            session.setAttribute("lastAccessTime", System.currentTimeMillis());
            return true;
        }

        return true;
    }
}
