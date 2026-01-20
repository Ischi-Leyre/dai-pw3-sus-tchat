package ch.heigvd.users;

import java.util.HashMap;
import java.util.Map;

public record User(
        Integer userId,
        String username,
        String email,
        String password,
        Boolean isAdmin
) {
    public Map<String, Object> toCreatedMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", userId);
        m.put("username", username);
        m.put("email", email);
        return m;
    }

    public Map<String, Object> toEditedMap(Boolean[] hasFieldsToUpdate) {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", userId);
        m.put("username", username);
        if (hasFieldsToUpdate != null && hasFieldsToUpdate.length > 0 && Boolean.TRUE.equals(hasFieldsToUpdate[0])) {
            m.put("email", email);
        }
        if (hasFieldsToUpdate != null && hasFieldsToUpdate.length > 1 && Boolean.TRUE.equals(hasFieldsToUpdate[1])) {
            m.put("password", "password has been updated");
        }
        return m;
    }

    public Map<String, Object> toProfileMap(int messageCount) {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", userId);
        m.put("username", username);
        m.put("email", email);
        m.put("isAdmin", isAdmin);
        m.put("messageCount", messageCount);
        return m;
    }

    public Map<String, Object> toListMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", userId);
        m.put("username", username);
        return m;
    }
}
