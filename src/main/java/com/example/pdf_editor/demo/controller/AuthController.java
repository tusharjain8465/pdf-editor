package com.example.pdf_editor.demo.controller;

import com.example.pdf_editor.demo.model.User;
import com.example.pdf_editor.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@CrossOrigin(origins = "https://pdf-editor-frontend-vercel.vercel.app")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // ================= REGISTER =================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        if (userRepository.findByMobile(user.getMobile()).isPresent()) {
            return ResponseEntity.badRequest().body("Mobile already exists");
        }

        if (userRepository.findByUserId(user.getUserId()).isPresent()) {
            return ResponseEntity.badRequest().body("UniqueId already exists");
        }

        if (userRepository.findByName(user.getName()).isPresent()) {
            return ResponseEntity.badRequest().body("Name already exists");
        }

        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    // ================= LOGIN =================
    @PostMapping("/login")
    public Object login(@RequestBody User request) {

        String input = request.getEmail(); // 👈 reuse field OR create new "username"

        Optional<User> userOpt = userRepository
                .findByEmailOrMobileOrName(input, input, input);

        if (userOpt.isEmpty()) {
            return "User not found";
        }

        User user = userOpt.get();

        if (!user.getPassword().equals(request.getPassword())) {
            return "Invalid password";
        }

        return user;
    }
}
