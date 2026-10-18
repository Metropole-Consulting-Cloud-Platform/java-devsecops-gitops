package com.asimshahibm.devsecops.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GreetingControllerTest {

    @Test
    void returnsGreeting() {
        GreetingController.Greeting greeting = new GreetingController().hello();

        assertEquals("Hello from Java DevSecOps", greeting.message());
        assertEquals("java-devsecops-api", greeting.service());
    }
}
