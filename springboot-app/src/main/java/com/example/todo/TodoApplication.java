package com.example.todo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The main entry point for the Spring Boot Todo Application.
 *
 * <p><strong>Python/FastAPI Comparison:</strong>
 * In Python/FastAPI, application startup is typically handled by running a server command
 * like {@code uvicorn main:app --reload} or invoking a Python script that instantiates
 * {@code FastAPI()} directly. In Spring Boot, the application structure is class-based and compiled.
 * The {@link SpringApplication#run(Class, String...)} method starts the Spring IoC container,
 * performs classpath scanning, sets up auto-configurations, and starts the embedded Tomcat server
 * on port 8080 by default.
 * </p>
 */
@SpringBootApplication
public class TodoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TodoApplication.class, args);
    }
}
