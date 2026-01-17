package ch.heigvd.users;

import io.javalin.http.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class UsersController {
    private final ConcurrentMap<Integer, User> users;
    private final ConcurrentMap<String, Integer> cookies;

    private final AtomicInteger uniqueId = new AtomicInteger(1);

    public UsersController(ConcurrentMap<Integer, User> users, ConcurrentMap<String, Integer> cookies) {
        this.users = users;
        this.cookies = cookies;
    }

    public void create(Context ctx) {
        // Parse and validate request body
        User req =
                ctx.bodyValidator(User.class)
                        .check(obj -> obj.username() != null, "Missing username")
                        .check(obj -> obj.email() != null, "Missing email")
                        .check(obj -> obj.password() != null, "Missing password")
                        .get();

        // Check for conflicts
        for (User user : users.values()) {
            if (req.email().equalsIgnoreCase(user.email()) ||
            req.username().equalsIgnoreCase(user.username())) {
                throw new ConflictResponse();
            }
        }

        // Create the new user
        User newUser =
                new User(
                        uniqueId.getAndIncrement(),
                        req.username(),
                        req.email(),
                        req.password(),
                        false);

        users.put(newUser.userId(), newUser);

        ctx.status(HttpStatus.CREATED);

        Map<String, Object> res = new HashMap<>();
        res.put("userId", newUser.userId());
        res.put("username", newUser.username());
        res.put("email", newUser.email());

        ctx.json(res);
    }

    public void update(Context ctx) {
        // session validation
        String session = ctx.cookie("session_id");
        if (session == null) {
            throw new UnauthorizedResponse();
        }

        Integer userId = cookies.get(session);
        if (userId == null) {
            throw new UnauthorizedResponse();
        }

        if (!userId.equals(ctx.pathParamAsClass("userId", Integer.class).get())) {
            throw new ForbiddenResponse("Forbidden: You can only update your own user");
        }

        if (!users.containsKey(userId)) {
            throw new NotFoundResponse();
        }

        // read body as map and validate allowed fields
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        if (body == null || body.isEmpty()) {
            throw new BadRequestResponse();
        }

        User existingUser = users.get(userId);

        String newEmail = body.containsKey("email") ? (String) body.get("email") : existingUser.email();
        String newPassword = body.containsKey("password") ? (String) body.get("password") : existingUser.password();

        for (User user : users.values()) {
            if (newEmail.equalsIgnoreCase(user.email())) {
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

        Map<String, Object> res = new HashMap<>();
        res.put("userId", updateUser.userId());
        res.put("username", updateUser.username());
        res.put("email", updateUser.email());

        ctx.json(res);
    }

    public void getOne(Context ctx) {
        // Validate the cookie
        String session = ctx.cookie("session_id");
        if (session == null) {
            throw new UnauthorizedResponse();
        }

        // Get
        Integer id = ctx.pathParamAsClass("userId", Integer.class).get();

        User user = users.get(id);

        if (user == null) {
            throw new NotFoundResponse();
        }

        Map<String, Object> res = new HashMap<>();
        res.put("userId", user.userId());
        res.put("username", user.username());

        ctx.json(res);
    }

    public void getMany(Context ctx) {
        // validate that only 'username' query parameter is present
        for (String key : ctx.queryParamMap().keySet()) {
            if (!"username".equals(key)) {
                throw new BadRequestResponse();
            }
        }

        // validate cookie
        String session = ctx.cookie("session_id");
        if (session == null) {
            throw new UnauthorizedResponse();
        }

        String username = ctx.queryParam("username");

        List<Map<String, Object>> list = new ArrayList<>();

        for (User user : this.users.values()) {
            if (username != null && !user.username().equalsIgnoreCase(username)) {
                continue;
            }

            Map<String, Object> u = new HashMap<>();
            u.put("userId", user.userId());
            u.put("username", user.username());
            list.add(u);
        }

        if (list.isEmpty()) {
            ctx.status(HttpStatus.NO_CONTENT);
            return;
        }

        ctx.json(list);
    }

    public void delete(Context ctx) {
        Integer id = ctx.pathParamAsClass("id", Integer.class).get();

        if (!users.containsKey(id)) {
            throw new NotFoundResponse();
        }

        users.remove(id);

        ctx.status(HttpStatus.NO_CONTENT);
    }
}
