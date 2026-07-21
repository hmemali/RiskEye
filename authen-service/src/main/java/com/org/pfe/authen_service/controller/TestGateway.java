package com.org.pfe.authen_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping()
public class TestGateway {

    @GetMapping("/teste")
    public String testgateway() {
        return "hello world";
    }
}