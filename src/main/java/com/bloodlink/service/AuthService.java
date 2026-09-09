package com.bloodlink.service;

import com.bloodlink.model.*;
import com.bloodlink.repository.UserRepository;

import java.util.Optional;

public final class AuthService {

    private final UserRepository repo;
    private final AuditService audit;

    public AuthService(UserRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    /**
     * Both outcomes are audited. Failed sign in attempts matter as much as
     * successful ones, since they are the first sign of a compromised account.
     */
    public Optional<User> login(String email, char[] password) {
        if (email == null || email.isBlank() || password.length == 0) return Optional.empty();

        Optional<User> user = repo.authenticate(email.trim(), password);
        if (user.isPresent()) {
            audit.setActor(user.get());
            audit.recordAs(user.get().getName(), user.get().getRole(), AuditAction.LOGIN_SUCCESS,
                    "Signed in as " + email.trim());
        } else {
            audit.recordAs(email.trim(), Role.STAFF, AuditAction.LOGIN_FAILED,
                    "Rejected sign in attempt for " + email.trim());
        }
        return user;
    }
}
