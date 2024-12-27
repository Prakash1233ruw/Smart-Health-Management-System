package com.pp.smarthealth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "Smart Health Management System API", 
version = "1.0",
description = "An API for Smart Health Management System"),
servers = @Server(url = "http://localhost:8989/SmartHealthApi",
		            description = " This system aims to streamline the management of patient records, appointments, and health statistics for healthcare providers"))
public class SmartHealthManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartHealthManagementSystemApplication.class, args);
	}

}
