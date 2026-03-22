package com.example.pdf_editor.demo.repository;

import com.example.pdf_editor.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByMobile(String mobile);

    Optional<User> findByUserId(String userId);

    Optional<User> findByName(String name);

    // 🔥 IMPORTANT (for login with any field)
    Optional<User> findByEmailOrMobileOrName(String email, String mobile, String name);
}