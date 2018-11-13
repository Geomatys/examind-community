package org.constellation.engine.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.userdetails.UserDetails;

public interface UserDetailsExtractor {


    UserDetails userDetails(HttpServletRequest request, HttpServletResponse response);

}
