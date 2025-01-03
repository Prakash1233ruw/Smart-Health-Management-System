package com.pp.smarthealth.security;

import com.pp.smarthealth.model.Admin;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class AdminDetails implements UserDetails {

    private final Admin admin;

    public AdminDetails(Admin admin) {
        this.admin = admin;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // You can add roles or authorities here
        return Collections.singletonList(() -> "ROLE_ADMIN");
    }

    @Override
    public String getPassword() {
        return admin.getPassword(); 
    }

    @Override
    public String getUsername() {
        return admin.getUsername(); 
    }

   
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
