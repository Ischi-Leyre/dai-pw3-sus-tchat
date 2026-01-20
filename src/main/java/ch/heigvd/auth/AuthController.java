package ch.heigvd.auth;

import ch.heigvd.users.User;
import ch.heigvd.messages.Message;

import io.javalin.http.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class AuthController {
    private final ConcurrentMap<Integer, User> users;
    private final ConcurrentMap<Integer, Message> messages;
    private final ConcurrentMap<String, Integer> cookies;

    public AuthController(ConcurrentMap<Integer, User> users, ConcurrentMap<Integer, Message> messages, ConcurrentMap<String, Integer> cookies) {
        this.users = users;
        this.messages = messages;
        this.cookies = cookies;
    }

    public void login(Context ctx) {
        // ------------------------------------------------ LOGIN VALIDATION -------------------------------------------
        String session = ctx.cookie("session_id");
        if (session != null && cookies.containsKey(session)) {
            throw new BadRequestResponse("User is already logged in");
        }

        // ------------------------------------------------ BODY VALIDATION --------------------------------------------
        Map<?, ?> raw = ctx.bodyAsClass(Map.class);
        if (raw == null || raw.isEmpty()) {
            throw new BadRequestResponse("Request body is missing or empty");
        }

        List<String> allowed = List.of("username", "email", "password");
        for (Object k : raw.keySet()) {
            if (!(k instanceof String key)) {
                throw new BadRequestResponse("Invalid field type");
            }
            if (!allowed.contains(key)) {
                throw new BadRequestResponse("Unexpected field: " + key);
            }
        }

        // ------------------------------------------------- EXTRACT FIELDS --------------------------------------------
        String username = raw.get("username") instanceof String ? ((String) raw.get("username")).trim() : null;
        String email = raw.get("email") instanceof String ? ((String) raw.get("email")).trim() : null;
        String password = raw.get("password") instanceof String ? (String) raw.get("password") : null;

        if ((username == null || username.isBlank()) && (email == null || email.isBlank())) {
            throw new BadRequestResponse("Missing username or email");
        }
        if (password == null) {
            throw new BadRequestResponse("Missing password");
        }

        // ------------------------------------------------- CONNECT USER ----------------------------------------------
        for (User user : users.values()) {
            boolean identityMatches =
                    (username != null && !username.isBlank() && user.username() != null && user.username().equalsIgnoreCase(username))
                    || (email != null && !email.isBlank() && user.email() != null && user.email().equalsIgnoreCase(email));

            if (identityMatches && user.password().equals(password)) {
                String sessionId = (Instant.now().toString() + user.userId()).hashCode() + "";
                ctx.cookie("session_id", sessionId);
                cookies.put(sessionId, user.userId());
                ctx.status(HttpStatus.NO_CONTENT);
                return;
            }
        }

        throw new UnauthorizedResponse();
    }

    public void logout(Context ctx) {
        // ------------------------------------------------ COOKIE VALIDATION ------------------------------------------
        String cookie = verifySessionId(ctx.cookie("session_id"));

        // -------------------------------------------------- REMOVE COOKIE --------------------------------------------
        cookies.remove(cookie);

        // -------------------------------------------------- RESPONSE -------------------------------------------------
        ctx.removeCookie("session_id");
        ctx.status(HttpStatus.NO_CONTENT);
    }

    public void profile(Context ctx) {
        // ------------------------------------------------ COOKIE VALIDATION ------------------------------------------
        String session = verifySessionId(ctx.cookie("session_id"));
        Integer userId = getUserIdFromSession(session);

        // -------------------------------------------------- FETCH USER -----------------------------------------------
        User user = users.get(userId);

        if (user == null) {
            throw new NotFoundResponse();
        }

        // -------------------------------------------------- COUNT MESSAGES -------------------------------------------
        int count = 0;
        for (Message message : messages.values()) {
            if (message.userId().equals(userId)) {
                count++;
            }
        }

        // --------------------------------------------------- RESPONSE ------------------------------------------------
        ctx.status(HttpStatus.OK);
        ctx.json(user.toProfileMap(count));
    }

    // Function utils

    // Validate sessionId
    String verifySessionId(String session) {
        if (session == null || !cookies.containsKey(session)) {
            throw new UnauthorizedResponse("Invalid or missing cookie");
        }
        return session;
    }

    // Get userId from sessionId
    Integer getUserIdFromSession(String session) {
        Integer userId = cookies.get(session);
        if (userId == null) {
            throw new UnauthorizedResponse("Invalid session");
        }
        return userId;
    }
}
