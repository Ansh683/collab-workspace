package com.ansh.collab.controller;

import com.ansh.collab.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body("Username and password are required");
        }
        if (password.length() < 4) {
            return ResponseEntity.badRequest().body("Password must be at least 4 characters");
        }

        try {
            userService.register(username.trim(), password);
            return ResponseEntity.ok("Registered successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Returns the currently logged-in username (used by frontend on load)
    @GetMapping("/me")
    public ResponseEntity<?> me(java.security.Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Not logged in");
        }
        return ResponseEntity.ok(Map.of("username", principal.getName()));
    }
}