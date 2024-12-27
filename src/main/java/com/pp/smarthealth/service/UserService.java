package com.pp.smarthealth.service;

import com.pp.smarthealth.dto.UserDTO;
import com.pp.smarthealth.model.User;

import java.util.List;

public interface UserService {
    UserDTO saveUser(User user);
    UserDTO getUserById(Long id);
    List<UserDTO> getAllUsers();
    UserDTO updateUser(Long id, User userDetails);
    void deleteUser(Long id);
}
