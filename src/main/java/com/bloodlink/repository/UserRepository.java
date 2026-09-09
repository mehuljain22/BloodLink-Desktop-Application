package com.bloodlink.repository;
import com.bloodlink.model.User;
import java.util.Optional;
public interface UserRepository { Optional<User> authenticate(String email, char[] password); }
