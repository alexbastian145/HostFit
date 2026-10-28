package com.hostfit.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Adds the current path to every model so the navigation can highlight the active page. */
@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class WebAdvice {

    @ModelAttribute("path")
    public String path(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
