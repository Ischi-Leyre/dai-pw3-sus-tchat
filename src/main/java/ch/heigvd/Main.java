package ch.heigvd;

import ch.heigvd.auth.AuthController;
import ch.heigvd.users.User;
import ch.heigvd.users.UsersController;
import ch.heigvd.messages.Message;
import ch.heigvd.messages.MessagesController;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Main {
    public static final int PORT = 8080;

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
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
        UsersController usersController = new UsersController(users,messages, cookies);
        MessagesController messagesController = new MessagesController(messages, users, cookies);

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
}
