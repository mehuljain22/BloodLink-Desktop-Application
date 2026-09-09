package com.bloodlink.repository.memory;

import com.bloodlink.model.*;
import com.bloodlink.repository.UserRepository;
import java.util.Optional;

public final class MemoryUserRepository implements UserRepository {
    @Override public Optional<User> authenticate(String email, char[] password) {
        String p = new String(password);
        if ("admin@bloodlink.local".equalsIgnoreCase(email) && "Admin@123".equals(p))
            return Optional.of(new Admin(1, "BloodLink Administrator", email));
        if ("staff@bloodlink.local".equalsIgnoreCase(email) && "Staff@123".equals(p))
            return Optional.of(new Staff(2, "Operations Staff", email));
        return Optional.empty();
    }
}
