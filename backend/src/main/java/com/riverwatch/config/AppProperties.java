package com.riverwatch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "riverwatch")
public class AppProperties {
    private String jwtSecret;
    private int accessTokenMinutes = 30;
    private int refreshTokenDays = 7;
    private int maxFailedLogins = 5;
    private int lockMinutes = 15;
    private boolean cookieSecure = false;
    private String corsOrigin = "";
    private Admin admin = new Admin();

    public static class Admin {
        private String name = "System Administrator";
        private String email = "";
        private String password = "";
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
    public int getAccessTokenMinutes() { return accessTokenMinutes; }
    public void setAccessTokenMinutes(int v) { this.accessTokenMinutes = v; }
    public int getRefreshTokenDays() { return refreshTokenDays; }
    public void setRefreshTokenDays(int v) { this.refreshTokenDays = v; }
    public int getMaxFailedLogins() { return maxFailedLogins; }
    public void setMaxFailedLogins(int v) { this.maxFailedLogins = v; }
    public int getLockMinutes() { return lockMinutes; }
    public void setLockMinutes(int v) { this.lockMinutes = v; }
    public boolean isCookieSecure() { return cookieSecure; }
    public void setCookieSecure(boolean v) { this.cookieSecure = v; }
    public String getCorsOrigin() { return corsOrigin; }
    public void setCorsOrigin(String v) { this.corsOrigin = v; }
    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }
}
