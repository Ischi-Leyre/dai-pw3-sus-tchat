package ch.heigvd.messages;

import io.javalin.http.*;

// ---- Java Time Imports ----
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

// ---- Java Util Imports ----
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ---- Java Concurrent Imports ----
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import ch.heigvd.users.User;

public class MessagesController {
    private final ConcurrentMap<Integer, Message> messages;
    private final ConcurrentMap<Integer, User> users;
    private final ConcurrentMap<String, Integer> cookies;
    private final static AtomicInteger messageId = new AtomicInteger(1);
    private final static AtomicReference<Instant> lastModified = new AtomicReference<>(Instant.now());

    // Update lastModified atomically
    public static Instant updateAndGetLastModified() {
        return lastModified.updateAndGet(old -> {
            Instant now = Instant.now();
            return now.isAfter(old) ? now : old;
        });
    }

    public MessagesController(ConcurrentMap<Integer, Message> messages,
                              ConcurrentMap<Integer, User> users,
                              ConcurrentMap<String, Integer> cookies) {
        this.messages = messages;
        this.users = users;
        this.cookies = cookies;
    }

    public void create(Context ctx) {
        // ---------------------------------- COOKIE VALIDATION --------------------------------------------------------
        String session = verifySessionId(ctx.cookie("session_id"));
        Integer userId = getUserIdFromSession(session);

        // ---------------------------------- BODY VALIDATION ----------------------------------------------------------
        Map<?, ?> raw = ctx.bodyAsClass(Map.class);
        if (raw == null || raw.isEmpty()) {
            throw new BadRequestResponse("Request body is missing or empty");
        }

        // Only allowed field: content
        for (Object k : raw.keySet()) {
            if (!(k instanceof String key)) throw new BadRequestResponse("Invalid field type");
            if (!"content".equals(key)) throw new BadRequestResponse("Unexpected field: " + key);
        }

        // ------------------------------------------------ Extract content --------------------------------------------
        String content = raw.get("content") instanceof String ? ((String) raw.get("content")).trim() : null;
        if (content == null || content.isBlank()) {
            throw new BadRequestResponse("Missing content");
        }

        // ------------------------------------------------ Check Conflicts --------------------------------------------
        int msgId = messageId.getAndIncrement();
        // Check for conflicts
        for (Message message : messages.values()) {
            if (message.userId().equals(userId) && message.msgId().equals(msgId)) {
                throw new ConflictResponse();
            }
        }
        // ------------------------------------------------ Create Message ---------------------------------------------

        Message newMessage =
                new Message(
                        userId,
                        msgId,
                        updateAndGetLastModified(),
                        null,
                        content);


        messages.put(msgId, newMessage);

        // ------------------------------------------------- Response --------------------------------------------------
        ctx.status(HttpStatus.CREATED);
        ctx.json(newMessage.toCreatedMap());
    }

    public void update(Context ctx) {
        // ---------------------------------- COOKIE VALIDATION --------------------------------------------------------
        String session = verifySessionId(ctx.cookie("session_id"));
        Integer userId = getUserIdFromSession(session);

        // ---------------------------------- PATH PARAM VALIDATION ----------------------------------------------------
        Integer msgId = ctx.pathParamAsClass("msgId", Integer.class).get();

        // Check message exists
        if (!messages.containsKey(msgId)) {
            throw new NotFoundResponse();
        }

        // Check message belongs to user
        Message existingMessage = messages.get(msgId);
        if (!existingMessage.userId().equals(userId)) {
            throw new ForbiddenResponse("Forbidden: You can only update your own messages");
        }

        // ---------------------------------- BODY VALIDATION ----------------------------------------------------------
        Map<?, ?> raw = ctx.bodyAsClass(Map.class);
        if (raw == null || raw.isEmpty())
            throw new BadRequestResponse("Request body is missing or empty");

        for (Object k : raw.keySet()) {
            if (!(k instanceof String key))
                throw new BadRequestResponse("Invalid field type");

            if (!"content".equals(key))
                throw new BadRequestResponse("Unexpected field: " + key);
        }

        // ------------------------------------------------ Extract content --------------------------------------------
        String content = raw.get("content") instanceof String ? ((String) raw.get("content")).trim() : null;

        if (content == null || content.isBlank())
            throw new BadRequestResponse("Missing content");

        // ------------------------------------------------ Update Message ---------------------------------------------
        Message updateMessage = new Message(
                existingMessage.userId(),
                existingMessage.msgId(),
                existingMessage.createdAt(),
                updateAndGetLastModified(),
                content);

        messages.replace(msgId, updateMessage);

        // ------------------------------------------------- Response --------------------------------------------------
        ctx.status(HttpStatus.OK);
        ctx.json(updateMessage.toEditedMap());
    }

    public void getMine(Context ctx) {
        // ---------------------------------- COOKIE VALIDATION --------------------------------------------------------
        String session = verifySessionId(ctx.cookie("session_id"));
        Integer userId = getUserIdFromSession(session);

        // ---------------------------------- Retrieve Messages --------------------------------------------------------
        List<Map<String, Object>> userMessages = new ArrayList<>();
        for (Message message : messages.values()) {
            if (message.userId().equals(userId)) {
                userMessages.add(message.toMineMap());
            }
        }

        // ------------------------------------------------- Response --------------------------------------------------
        ctx.status(HttpStatus.OK);
        ctx.json(userMessages);
    }

    public void getAll(Context ctx) {
        // ---------------------------------- COOKIE VALIDATION --------------------------------------------------------
        String session = verifySessionId(ctx.cookie("session_id"));
        getUserIdFromSession(session);

        // ---------------------------------- Cache & Query Params Variables -------------------------------------------
        // Variables Cache Validation
        Instant ifModifiedSince = null;

        // Variables Query Parameters
        String since = null;
        String username = null;
        Instant sinceDateTime = null;

        // ---------------------------------- Cache Validation & Query Params ------------------------------------------
        // Check for query parameters
        boolean hasQueryParams = !ctx.queryParamMap().isEmpty();

        if(hasQueryParams) {
            // --------------------------------- Query Params Validation ----------------------------
            // validate allowed query params ('username' and 'since')
            for (String key : ctx.queryParamMap().keySet()) {
                if (key != null && !"username".equals(key) && !"since".equals(key)) {
                    throw new BadRequestResponse();
                }
            }

            since = ctx.queryParam("since"); // format: dd-mm-yyyy
            username = ctx.queryParam("username");

            // Check and convert since param
            if (since != null && !since.isBlank()) {
                if (!since.matches("\\d{2}-\\d{2}-\\d{4}")) {
                    throw new BadRequestResponse("since parameter must be in format dd-mm-yyyy");
                }

                // Convert since to LocalDateTime
                String[] parts = since.split("-");
                int day = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int year = Integer.parseInt(parts[2]);
                LocalDate date = LocalDate.of(year, month, day);
                sinceDateTime = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            }

            // Check username param
            if (username != null && username.isBlank()) {
                throw new BadRequestResponse("username parameter cannot be blank");
            }

        } else {
            // ------------------------------- Cache Validation --------------------------------
            // Cache validation only if not query parameters are present
            String header = ctx.header("If-Modified-Since");
            if (header != null) {
                try {
                    ifModifiedSince = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(header));
                } catch (Exception e) {
                    throw new BadRequestResponse("Invalid If-Modified-Since header format");
                }
                if (!lastModified.get().isAfter(ifModifiedSince)) {
                    ctx.status(HttpStatus.NOT_MODIFIED);
                    return;
                }
            }
        }

        // ---------------------------------- Retrieve Messages --------------------------------------------------------
        List<Map<String, Object>> msg = new ArrayList<>();

        for (Message message : this.messages.values()) {
            if (sinceDateTime != null && message.createdAt().isBefore(sinceDateTime)) {
                continue;
            }

            if (sinceDateTime != null && message.editedAt() != null && message.editedAt().isBefore(sinceDateTime)) {
                continue;
            }

            if (username != null && !users.get(message.userId()).username().equalsIgnoreCase(username)) {
                continue;
            }

            Map<String, Object> res = new HashMap<>();
            res.put("username", users.get(message.userId()).username());
            res.put("createdAt", message.createdAt());
            res.put("editedAt", message.editedAt());
            res.put("content", message.content());

            msg.add(res);
        }

        // ------------------------------------------------- Response --------------------------------------------------
        // Set Last-Modified header
        ctx.header("Last-Modified", DateTimeFormatter.RFC_1123_DATE_TIME
                .withZone(ZoneOffset.UTC)
                .format(lastModified.get()));

        ctx.status(HttpStatus.OK);
        ctx.json(msg);
    }

    public void delete(Context ctx) {
        // ---------------------------------- COOKIE VALIDATION --------------------------------------------------------
        String session = verifySessionId(ctx.cookie("session_id"));
        Integer userId = getUserIdFromSession(session);

        // ---------------------------------- PATH PARAM VALIDATION ----------------------------------------------------
        Integer msgId = ctx.pathParamAsClass("msgId", Integer.class).get();

        if (!messages.containsKey(msgId)) {
            throw new NotFoundResponse();
        }

        // ---------------------------------- Check Ownership ----------------------------------------------------------
        // Check message belongs to user
        Message existingMessage = messages.get(msgId);
        if (!existingMessage.userId().equals(userId)) {
            ctx.status(HttpStatus.FORBIDDEN);
            return;
        }

        // ---------------------------------- Delete Message -----------------------------------------------------------
        messages.remove(msgId);

        // ------------------------------------------------- Response --------------------------------------------------
        ctx.status(HttpStatus.NO_CONTENT);
    }

    public boolean deleteAllMessagesForUser(Integer userId) {
        boolean deleted = false;
        List<Integer> messagesToDelete = new ArrayList<>();

        // Collect message IDs to delete
        for (Message message : messages.values()) {
            if (message.userId().equals(userId)) {
                messagesToDelete.add(message.msgId());
            }
        }

        // Delete collected messages
        for (Integer msgId : messagesToDelete) {
            messages.remove(msgId);
            deleted = true;
        }

        return deleted;
    }

    // Function utils

    // Validate sessionId
    protected String verifySessionId(String session) {
        if (session == null || !cookies.containsKey(session)) {
            throw new UnauthorizedResponse("Invalid or missing cookie");
        }
        return session;
    }

    // Get userId from sessionId
    protected Integer getUserIdFromSession(String session) {
        Integer userId = cookies.get(session);
        if (userId == null) {
            throw new UnauthorizedResponse("Invalid session");
        }
        return userId;
    }
}
