package nro.models.server.control.auth;

public class AdminUser {
    private final int id;
    private final String username;
    private final UserRole role;

    public AdminUser(int id, String username, UserRole role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isSuperAdmin() {
        return role == UserRole.SUPER_ADMIN;
    }
}
