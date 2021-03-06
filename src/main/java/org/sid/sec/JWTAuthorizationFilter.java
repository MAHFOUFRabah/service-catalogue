package org.sid.sec;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JWTAuthorizationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Autoriser Angular à envoyer des entêtes
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, PUT, GET, OPTIONS, DELETE");

        response.addHeader("Access-Control-Allow-Headers", "Origin, Accept, X-Request-With, "
                + "Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers, " + "authorization");
        response.addHeader("Access-Control-Expose-Headers",
                " Access-Control-Allow-Origin, Access-Control-Allow-Credentials," + " authorization ");
        if (request.getMethod().equals("OPTIONS")) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else if (request.getRequestURI().equals("/login")) {
            filterChain.doFilter(request, response);
            return;
        } else {

            String jwt = request.getHeader(SecurityParams.HEADER_NAME);
            if (jwt == null || !jwt.startsWith(SecurityParams.HEADER_PREFIX)) {
                filterChain.doFilter(request, response);
                return;
            }
            // On reccupère le secret pour decypter le Token
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SecurityParams.SECRET)).build();

            DecodedJWT decodedJWT = verifier.verify(jwt.substring(SecurityParams.HEADER_PREFIX.length()));
            // get Username and roles
            String username = decodedJWT.getSubject();
            List<String> roles = decodedJWT.getClaims().get("role").asList(String.class);
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            roles.forEach(rn -> {
                authorities.add(new SimpleGrantedAuthority(rn));
            });
            // add the user to the Spring Security Context
            UsernamePasswordAuthenticationToken user = new UsernamePasswordAuthenticationToken(username, null,
                    authorities);
            SecurityContextHolder.getContext().setAuthentication(user);

            filterChain.doFilter(request, response);

        }

    }

}
