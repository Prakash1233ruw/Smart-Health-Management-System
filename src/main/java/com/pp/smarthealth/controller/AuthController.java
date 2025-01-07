package com.pp.smarthealth.controller;

import com.pp.smarthealth.payload.request.AuthRequest;
import com.pp.smarthealth.service.impl.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

  
    @Autowired
    private AuthService authService;
    
   

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