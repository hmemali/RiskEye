package com.org.pfe.account_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping()
public class TestGateway {

    @GetMapping("/test")
    public String testgateway() {
        return "hello world";
    }
}