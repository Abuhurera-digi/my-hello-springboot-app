package com.example.hello_world;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {

    @GetMapping("/")

    public String helloWorld() {
        return "Hello World! today is monday..... abuhureravivek";
    }
      @GetMapping("/health")
    public String healthCheck() {
        return "Application is running";
    }
}



