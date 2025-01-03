package com.pp.smarthealth.service;


import java.util.List;

import com.pp.smarthealth.model.Admin;


public interface AdminService {

    
    String verify(Admin admin);
	Admin register(Admin admin);
	void deleteUser(Long id);
	List<Admin> getAllUsers();
}
