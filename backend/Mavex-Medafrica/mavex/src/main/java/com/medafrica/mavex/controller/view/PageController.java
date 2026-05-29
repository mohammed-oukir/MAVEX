package com.medafrica.mavex.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller Thymeleaf — sert les pages HTML.
 * La sécurité est gérée côté client (vérification JWT dans localStorage).
 * Les API REST sont protégées par JwtAuthenticationFilter.
 */
@Controller
public class PageController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/shipments")
    public String shipments() {
        return "shipments";
    }

    @GetMapping("/shipments/{id}")
    public String shipmentDetail() {
        return "shipment-detail";
    }

    @GetMapping("/orders")
    public String orders() {
        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail() {
        return "order-detail";
    }

    @GetMapping("/clients")
    public String clients() {
        return "clients";
    }

    @GetMapping("/shippers")
    public String shippers() {
        return "shippers";
    }

    @GetMapping("/imports")
    public String imports() {
        return "imports";
    }

    @GetMapping("/users")
    public String users() {
        return "users";
    }
}