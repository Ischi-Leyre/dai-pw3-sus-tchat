package ch.heigvd.users;

public record User(
        Integer userId,
        String username,
        String email,
        String password,
        Boolean isAdmin
) {}
