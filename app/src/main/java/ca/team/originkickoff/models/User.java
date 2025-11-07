/**
 * User profile model persisted in Firestore and referenced across features.
 */
package ca.team.originkickoff.models;

import com.google.firebase.firestore.PropertyName;
import java.util.Date;

/**
 * Represents an application user with notification preferences and roles.
 */
public class User {
    private String id;
    @PropertyName("device_id")
    private String deviceId;
    @PropertyName("display_name")
    private String displayName;
    private String email;
    private String phone;
    @PropertyName("notif_marketing")
    private boolean notifMarketing;
    @PropertyName("notif_service")
    private boolean notifService;
    @PropertyName("is_organizer")
    private boolean isOrganizer;
    @PropertyName("is_admin")
    private boolean isAdmin;
    @PropertyName("created_at")
    private Date createdAt;
    @PropertyName("updated_at")
    private Date updatedAt;

    /** No-arg constructor required by Firebase deserialization. */
    public User() {
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @PropertyName("device_id")
    public String getDeviceId() { return deviceId; }
    @PropertyName("device_id")
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    @PropertyName("display_name")
    public String getDisplayName() { return displayName; }
    @PropertyName("display_name")
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    @PropertyName("notif_marketing")
    public boolean isNotifMarketing() { return notifMarketing; }
    @PropertyName("notif_marketing")
    public void setNotifMarketing(boolean notifMarketing) { this.notifMarketing = notifMarketing; }

    @PropertyName("notif_service")
    public boolean isNotifService() { return notifService; }
    @PropertyName("notif_service")
    public void setNotifService(boolean notifService) { this.notifService = notifService; }

    @PropertyName("is_organizer")
    public boolean isOrganizer() { return isOrganizer; }
    @PropertyName("is_organizer")
    public void setOrganizer(boolean organizer) { isOrganizer = organizer; }

    @PropertyName("is_admin")
    public boolean isAdmin() { return isAdmin; }
    @PropertyName("is_admin")
    public void setAdmin(boolean admin) { isAdmin = admin; }

    @PropertyName("created_at")
    public Date getCreatedAt() { return createdAt; }
    @PropertyName("created_at")
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    @PropertyName("updated_at")
    public Date getUpdatedAt() { return updatedAt; }
    @PropertyName("updated_at")
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
