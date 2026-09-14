package com.ridelink.account.security;

import com.ridelink.account.exception.GlobalExceptionHandler.ErrorResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class SecurityErrorHandlers {

    private final ObjectMapper objectMapper;

    public SecurityErrorHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuthenticationEntryPoint unauthorized() {
        return (request, response, ex) ->
                write(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED",
                        "Authentication is required or the token is invalid");
    }

    public AccessDeniedHandler forbidden() {
        return (request, response, ex) ->
                write(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN",
                        "You do not have permission to perform this action");
    }

    private void write(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                new ErrorResponse(code, message, List.of()));
    }
}