package nro.models.server.control.auth;

public enum UserRole {
    SUPER_ADMIN(3),
    ADMIN(3),
    VIEWER(1);

    private final int level;

    UserRole(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public boolean hasPermission(UserRole requiredRole) {
        // Any Admin (ADMIN or SUPER_ADMIN) has full permission across entire system
        if (this == SUPER_ADMIN || this == ADMIN || this.level >= 2) {
            return true;
        }
        return this.level >= requiredRole.level;
    }

    public static UserRole fromString(String roleStr) {
        if (roleStr == null) return VIEWER;
        try {
            String clean = roleStr.toUpperCase().trim();
            if ("ADMIN".equals(clean) || "SUPER_ADMIN".equals(clean)) {
                return SUPER_ADMIN;
            }
            return UserRole.valueOf(clean);
        } catch (IllegalArgumentException e) {
            return VIEWER;
        }
    }
}
