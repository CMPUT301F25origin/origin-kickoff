package ca.team.originkickoff.models;

public class NotificationItem {
    private String id;
    private String title;
    private String message;
    private String timestamp; // display-ready string for now

    public NotificationItem() {}

    public NotificationItem(String id, String title, String message, String timestamp) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}

