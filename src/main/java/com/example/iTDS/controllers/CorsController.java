package com.example.iTDS.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = { "http://localhost:3000", "https://activustdstest1-kappa.vercel.app/login",
        "https://activustdstest1-shyamyobels-projects.vercel.app/login","https://activus-server-production.up.railway.app" }, methods = RequestMethod.OPTIONS)
public class CorsController {
    @RequestMapping(value = "/api/**", method = RequestMethod.OPTIONS)
    public void corsHeaders() {
        // Handles preflight requests.
    }
}
