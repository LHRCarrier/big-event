package com.bubbles.server.service;

import com.bubbles.pojo.entity.User;

public interface UserService {
    void register(User user);

    User search(String username);
}
