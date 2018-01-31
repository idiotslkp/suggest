package com.massestech.core.base;

import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *
 */
@Component
@WebFilter(filterName="simpleCORSFilter",urlPatterns="/*")
public final class SimpleCORSFilter implements Filter {

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        response.setHeader("Access-Control-Allow-Methods", "POST, PUT, GET, OPTIONS, DELETE");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, userName, requestId, Authorization");
        HttpServletRequest request = (HttpServletRequest) req;

        if (request.getMethod().equals("OPTIONS")) {
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
            return ;
        }

        chain.doFilter(req, res);
    }

    public void init(FilterConfig filterConfig) {

    }

    public void destroy() {

    }
}


