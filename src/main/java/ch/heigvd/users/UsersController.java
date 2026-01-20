package ch.heigvd.users;

import ch.heigvd.messages.Message;

import io.javalin.http.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class UsersController {
    private final ConcurrentMap<Integer, User> users;
    private final ConcurrentMap<Integer, Message> messages;
    private final ConcurrentMap<String, Integer> cookies;

    private final AtomicInteger uniqueId = new AtomicInteger(1);

    public UsersController(ConcurrentMap<Integer, User> users,
                           ConcurrentMap<Integer, Message> messages,
                           ConcurrentMap<String, Integer> cookies) {
        this.users = users;
        this.messages = messages;
        this.cookies = cookies;
    }

    public void create(Context ctx) {
        // ------------------------------------------------ BODY VALIDATION ------------------------------------------
        Map<?, ?> raw = ctx.bodyAsClass(Map.class);
        if (raw == null || raw.isEmpty()) {
            throw new BadRequestResponse("Request body is missing or empty");
        }

        // Champs autorisés
        List<String> allowed = List.of("username", "email", "password");

        for (Object k : raw.keySet()) {
            if (!(k instanceof String key)) {
                throw new BadRequestResponse("Invalid field type");
            }

            if (!allowed.contains(key)) {
                throw new BadRequestResponse("Unexpected field: " + key);
            }
        }

        // ------------------------------------------------- EXTRACT FIELDS ------------------------------------------
        User req =
                ctx.bodyValidator(User.class)
                        .check(obj -> obj.username() != null, "Missing username")
                        .check(obj -> obj.email() != null, "Missing email")
                        .check(obj -> obj.password() != null, "Missing password")
                        .get();

        // ------------------------------------------------- CHECK CONFLICTS -----------------------------------------
        for (User user : users.values()) {
            if (req.email().equalsIgnoreCase(user.email()) ||
            req.username().equalsIgnoreCase(user.username())) {
                throw new ConflictResponse();
            }
        }

        // -------------------------------------------------- CREATE USER --------------------------------------------
        User newUser =
                new User(
                        uniqueId.getAndIncrement(),
                        req.username(),
                        req.email(),
                        req.password(),
                        false);

        users.put(newUser.userId(), newUser);

        // -------------------------------------------------- RESPONSE -----------------------------------------------
        ctx.status(HttpStatus.CREATED);
        ctx.json(newUser.toCreatedMap());
    }

    public void update(Context ctx) {
        // ------------------------------------------------ COOKIE VALIDATION ----------------------------------------
        String session = verifySessionId(ctx.cookie("session_id"));
        Integer userId = getUserIdFromSession(session);

        // ------------------------------------------------ PATH PARAM VALIDATION ------------------------------------
        Integer pathUserId = ctx.pathParamAsClass("userId", Integer.class).get();
        if (!userId.equals(pathUserId)) {
            throw new ForbiddenResponse("Forbidden: You can only update your own user");
        }

        if (!users.containsKey(userId)) {
            throw new NotFoundResponse();
        }

        // ------------------------------------------------- BODY VALIDATION -----------------------------------------
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        if (body == null || body.isEmpty()) {
            throw new BadRequestResponse("Request body is missing or empty");
        }

        // Check for allowed fields
        List<String> allowedFields = List.of("email", "password");
        for (String key : body.keySet()) {
            if (!allowedFields.contains(key)) {
                throw new BadRequestResponse("Unexpected field: " + key);
            }
        }

        // -------------------------------------------------- UPDATE USER --------------------------------------------
        User existingUser = users.get(userId);

        Boolean[] hasFieldsToUpdate;
        hasFieldsToUpdate = new Boolean[] {
            body.containsKey("email"),
            body.containsKey("password")
        };

        String newEmail = hasFieldsToUpdate[0] ? (String) body.get("email") : existingUser.email();
        String newPassword = hasFieldsToUpdate[1] ? (String) body.get("password") : existingUser.password();

        // Check for email conflict
        for (User user : users.values()) {
            if (!existingUser.userId().equals(user.userId()) && newEmail.equalsIgnoreCase(user.email())) {
                throw new ConflictResponse("Conflict: Email already in use");
            }
        }

        User updateUser =
                new User(
                        existingUser.userId(),
                        existingUser.username(),
                        newEmail,
                        newPassword,
                        existingUser.isAdmin());

        users.replace(userId, updateUser);

        // -------------------------------------------------- RESPONSE -----------------------------------------------
        ctx.status(HttpStatus.OK);
        ctx.json(updateUser.toEditedMap(hasFieldsToUpdate));
    }

    public void getOne(Context ctx) {
        // ------------------------------------------------ PATH PARAM VALIDATION ------------------------------------
        // Get id from path
        Integer pathUserId = ctx.pathParamAsClass("userId", Integer.class).get();

        // -------------------------------------------------- FETCH USER ---------------------------------------------
        User user = users.get(pathUserId);

        if (user == null) {
            throw new NotFoundResponse();
        }

        // -------------------------------------------------- RESPONSE -----------------------------------------------
        ctx.status(HttpStatus.OK);
        ctx.json(user.toListMap());
    }

    public void getMany(Context ctx) {
        // ------------------------------------------------ QUERY PARAM VALIDATION -----------------------------------
        // validate that only 'username' query parameter is present
        for (String key : ctx.queryParamMap().keySet()) {
            if (!"username".equals(key)) {
                throw new BadRequestResponse();
            }
        }

        // -------------------------------------------------- FETCH USERS --------------------------------------------
        String username = ctx.queryParam("username");

        List<Map<String,Object>> list = new ArrayList<>();

        for (User user : this.users.values()) {
            if (username != null && !user.username().equalsIgnoreCase(username)) {
                continue;
            }

            list.add(user.toListMap());
        }

        // -------------------------------------------------- RESPONSE -----------------------------------------------
        if (list.isEmpty()) {
            ctx.status(HttpStatus.NO_CONTENT);
        } else {
            ctx.status(HttpStatus.OK);
            ctx.json(list);
        }
    }

    public void delete(Context ctx) {
        // ------------------------------------------------- COOKIE VALIDATION ---------------------------------------
        String session = verifySessionId(ctx.cookie("session_id"));
        Integer userId = getUserIdFromSession(session);

        // ------------------------------------------------ PATH PARAM VALIDATION ------------------------------------
        Integer usrId = ctx.pathParamAsClass("userId", Integer.class).get();

        if (!users.containsKey(usrId)) {
            throw new NotFoundResponse();
        }

        if (!userId.equals(usrId)) {
            throw new ForbiddenResponse("Forbidden: You can only delete your own user");
        }

        // ------------------------------------------------ DELETE ASSOCIATED MESSAGES -------------------------------
        // Delete associated messages: collect keys first to avoid concurrency issues
        List<Integer> messagesToDelete = new ArrayList<>();
        for (Message message : messages.values()) {
            if (message.userId().equals(usrId)) {
                messagesToDelete.add(message.msgId());
            }
        }
        for (Integer msgId : messagesToDelete) {
            messages.remove(msgId);
        }

        // ------------------------------------------------ DELETE USER & SESSION ------------------------------------
        users.remove(usrId);
        cookies.remove(session);

        // -------------------------------------------------- RESPONSE -----------------------------------------------
        ctx.removeCookie("session_id");
        ctx.status(HttpStatus.NO_CONTENT);
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
