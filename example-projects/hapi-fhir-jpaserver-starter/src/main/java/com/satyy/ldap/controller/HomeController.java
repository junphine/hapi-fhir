package com.satyy.ldap.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 
 */
@RestController
public class HomeController {

    @GetMapping("/home")
    public String index() {
        return "Welcome to the home page!";
    }
    
    @GetMapping("/admin")
    public String admin() {
        return "Welcome to the admin page!";
    }
}
