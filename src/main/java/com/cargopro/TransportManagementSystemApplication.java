package com.cargopro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application Class
 * 
 * @SpringBootApplication is a convenience annotation that includes:
 * - @Configuration: Marks this as a configuration class
 * - @EnableAutoConfiguration: Enables Spring Boot auto-configuration
 * - @ComponentScan: Scans for components (services, controllers, etc.) in this package and sub-packages
 * 
 * When you run this class, Spring Boot will:
 * 1. Start an embedded Tomcat server
 * 2. Connect to PostgreSQL database
 * 3. Create database tables based on entities
 * 4. Make all REST endpoints available
 */
@SpringBootApplication
public class TransportManagementSystemApplication {

    public static void main(String[] args) {
        // This starts the Spring Boot application
        SpringApplication.run(TransportManagementSystemApplication.class, args);
        System.out.println("\n=========================================");
        System.out.println("Transport Management System is running!");
        System.out.println("API Base URL: http://localhost:8080/api");
        System.out.println("=========================================\n");
    }
}

