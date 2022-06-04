package com.example.demo.utility.oAtuh2;

import com.auth0.jwt.JWT;
import com.example.demo.entity.member.User;
import com.example.demo.repository.member.UserRepository;
import com.auth0.jwt.algorithms.Algorithm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    private UserRepository userRepository;

    //  성공시 토큰 생성, 쿠키생성
    @Autowired
    OAuth2AuthenticationSuccessHandler(HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository,
                                       UserRepository userRepository) {
        this.httpCookieOAuth2AuthorizationRequestRepository = httpCookieOAuth2AuthorizationRequestRepository;
        this.userRepository = userRepository;
    }


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException, IOException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Optional<String> redirectUri = CookieUtils.getCookie(request, "redirect_uri")
                .map(Cookie::getValue);

        System.out.println("리다이렉트 uri" + redirectUri);

        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());

        System.out.println(authentication.getPrincipal().toString());

        String name = authentication.getName();

        Optional<User> memberInfo = userRepository.findByEmail(name);

        List<String> authority = authentication.getAuthorities()
                .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());

        System.out.println("권한 설정 여부 " + authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));

        Algorithm algorithm = Algorithm.HMAC256("urunner".getBytes());

        String access_token = JWT.create()
                .withSubject(name)
                .withExpiresAt(new Date(System.currentTimeMillis() + 10 * 60 * 10000)) // 10^-3 초
                .withIssuer(request.getRequestURI().toString())
                .withClaim("roles", authority)
                .withClaim("name", memberInfo.get().getName())
                .withClaim("email", memberInfo.get().getEmail())
                .sign(algorithm);

        System.out.println(access_token);

        Map<String, String> tokens = new HashMap<>();


        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("access_token", access_token)
                .build().toUriString();
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequest(request, response);
    }

}
