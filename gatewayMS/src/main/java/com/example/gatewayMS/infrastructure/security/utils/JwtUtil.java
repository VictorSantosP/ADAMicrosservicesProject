package com.example.gatewayMS.infrastructure.security.utils;


import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Service;

@Service
public class JwtUtil {

    private static ReactiveJwtDecoder decoder;

    public JwtUtil(ReactiveJwtDecoder decoder){
        JwtUtil.decoder = decoder;
    }

    public static boolean validate(String token){
        try{
            decoder.decode(token).block();
            return true;
        } catch (JwtException | IllegalArgumentException e ) {
            return false;
        }
    }
}
