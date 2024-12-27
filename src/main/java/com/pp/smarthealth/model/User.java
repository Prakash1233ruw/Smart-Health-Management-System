package com.pp.smarthealth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;

@Entity
@Table(name = "user")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;
    private String role;
	/*
	 * @Enumerated(EnumType.STRING)
	 * 
	 * @Column(name = "role", nullable = false) private Role role;
	 */
    
	
	  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch =FetchType.LAZY) 
	  private Set<Patient> patients = new HashSet<>();
	 
}
