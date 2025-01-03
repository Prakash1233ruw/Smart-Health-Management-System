package com.pp.smarthealth.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.pp.smarthealth.model.Admin;

import com.pp.smarthealth.service.AdminService;



@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @PostMapping
    public ResponseEntity<Admin> createUser(@RequestBody Admin admin) {
        return ResponseEntity.ok(adminService.register(admin));
    }

    @GetMapping
    public ResponseEntity<List<Admin>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

   

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}