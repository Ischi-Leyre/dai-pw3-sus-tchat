package ch.heigvd;

import ch.heigvd.auth.AuthController;
import ch.heigvd.users.User;
import ch.heigvd.users.UsersController;
import ch.heigvd.messages.Message;
import ch.heigvd.messages.MessagesController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Main {
    public static final int PORT = 8080;

    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {

            // ---------------- JSON Mapper ----------------
            config.jsonMapper(new JavalinJackson(mapper, false));

            // ---------------- Static Files ----------------
            config.staticFiles.add(staticFileConfig -> {
                staticFileConfig.hostedPath = "/";
                staticFileConfig.directory = "public";
                staticFileConfig.location = Location.CLASSPATH;
            });
        });

        // This will serve as our database
        ConcurrentMap<Integer, User> users = new ConcurrentHashMap<>();
        ConcurrentMap<Integer, Message> messages = new ConcurrentHashMap<>();
        ConcurrentMap<String, Integer> cookies = new ConcurrentHashMap<>();

        // Controllers
        AuthController authController = new AuthController(users, messages,cookies);
        MessagesController messagesController = new MessagesController(messages, users, cookies);
        UsersController usersController = new UsersController(users, cookies, messagesController);

        // Create welcome admin and message
        Welcome(users, messages);

        // Users routes
        app.post  ("/users",          usersController::create);
        app.patch ("/users/{userId}", usersController::update);
        app.get   ("/users",          usersController::getMany);
        app.get   ("/users/{userId}", usersController::getOne);
        app.delete("/users/{userId}", usersController::delete);

        // Auth routes
        app.post  ("/login",    authController::login);
        app.post  ("/logout",   authController::logout);
        app.get   ("/profile",  authController::profile);

        // Message Route
        app.post  ("/messages",         messagesController::create);
        app.patch ("/messages/{msgId}", messagesController::update);
        app.get   ("/messages/mine",    messagesController::getMine);
        app.get   ("/messages",         messagesController::getAll);
        app.delete("/messages/{msgId}", messagesController::delete);

        app.start(PORT);
    }

    private static void Welcome(ConcurrentMap<Integer, User> users, ConcurrentMap<Integer, Message> messages) {
        // Create admin user
        User admin = new User(
            0,
            "admin",
            "admin@heig-vd.ch",
            "adminpassword",
            true
        );

        // Add admin user to users map
        users.put(admin.userId(), admin);

        // Create a welcome message
        Message welcomeMessage = new Message(
            0,
            0,
                Instant.now(),
            null,
            "Welcome to the JitSUSmon chat!\nFeel free to explore and connect with others. ;¬)"
        );

        // Add welcome message to messages map
        messages.put(welcomeMessage.msgId(), welcomeMessage);
    }
}
