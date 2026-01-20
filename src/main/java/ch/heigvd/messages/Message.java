package ch.heigvd.messages;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public record Message(
        Integer userId,
        Integer msgId,
        Instant createdAt, // only set when created -> server side handling
        Instant editedAt, // set when edited, can be null -> server side handling
        String content
) {
    public Map<String, Object> toCreatedMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", userId);
        m.put("msgId", msgId);
        m.put("content", content);
        return m;
    }

    public Map<String, Object> toEditedMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("msgId", msgId);
        m.put("content", content);
        return m;
    }

    public Map<String, Object> toMineMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("msgId", msgId);
        m.put("createdAt", createdAt.toString());
        m.put("editedAt", editedAt != null ? editedAt.toString() : null);
        m.put("content", content);
        return m;
    }
}
