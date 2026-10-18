package com.asimshahibm.devsecops.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class GreetingController {

    @GetMapping("/hello")
    public Greeting hello() {
        return new Greeting("Hello from Java DevSecOps", "java-devsecops-api");
    }

    public record Greeting(String message, String service) {
    }
}
