package com.pp.smarthealth.controller;

import com.pp.smarthealth.dto.UserRegistrationDTO;
import com.pp.smarthealth.payload.request.AuthRequest;
import com.pp.smarthealth.service.impl.AuthService;
import com.pp.smarthealth.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private AuthService authService;
    
   

//    @PostMapping("/login")
//    public String generateToken(@RequestBody AuthRequest authRequest) throws Exception {
//        try {
//            authenticationManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
//            );
//        } catch (AuthenticationException e) {
//            throw new Exception("Invalid username or password", e);
//        }
//        return jwtUtil.generateToken(authRequest.getUsername());
//    }
    @PostMapping("/login")
    public ResponseEntity<String> authenticate(
          @RequestBody  AuthRequest authRequest) {
        try {
            String token = authService.verify(authRequest);
            return ResponseEntity.ok("JWT Token: " + token);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }


    
}