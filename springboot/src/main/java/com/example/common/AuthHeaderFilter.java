package com.example.common;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
@Component
public class AuthHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        

        httpResponse.setHeader("Access-Control-Expose-Headers", "Authorization");

        chain.doFilter(request, response);

        String authHeader = httpResponse.getHeader("Authorization");
        if (authHeader != null) {
            httpResponse.setHeader("Access-Control-Expose-Headers", "Authorization");
        }
    }
} 