package ru.practicum.main.service;

import ru.practicum.main.dto.UserDto;

import java.util.List;

public interface UserService {
    UserDto addUser(UserDto dto);

    void deleteUserById(Long userId);

    List<UserDto> getAllUsers(List<Long> ids, Integer from, Integer size);
}
