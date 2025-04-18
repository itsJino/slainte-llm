package com.example.slainte;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Main test suite for the Slainte application.
 *
 * This class serves as both a test suite definition using JUnit 5's @Suite annotation
 * and includes the basic Spring Boot context loading test.
 */
@Suite
@SelectPackages({
    "com.example.slainte.controller",
    "com.example.slainte.service"
})
@IncludeClassNamePatterns({
    ".*Test",
    ".*Tests"
})
@SuiteDisplayName("Slainte Application Test Suite")
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class SlainteApplicationTests {

    /**
     * Simple test to verify that the Spring application context loads successfully.
     */
    @Test
    void contextLoads() {
        // This test will pass if the Spring application context loads successfully
    }
    
    /**
     * Additional test to ensure that basic application configurations are loaded.
     * This is useful for catching configuration-related issues early.
     */
    @Test
    void configurationLoads() {
        // This test would typically check that key configuration properties 
        // are loaded correctly but is empty for now
    }
}