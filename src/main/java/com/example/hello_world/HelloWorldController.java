package com.example.hello_world;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {

    @GetMapping("/")
    public String helloWorld() {
        return "Hello World! how are you raaj and everything is fine  and dee eeansssd wiwwddne anddddd dine....kkhjsssfdfdfhksdssssdd";
    }
}
