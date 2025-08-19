package com.tencent.im.backend.common.timeseries.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 时序数据库配置
 */
@Component
@ConfigurationProperties(prefix = "timeseries")
public class TimeSeriesProperties {

    /** 使用的时序数据库类型 */
    private DatabaseType type = DatabaseType.INFLUXDB;

    /** InfluxDB配置 */
    private InfluxDB influxdb = new InfluxDB();

    /** TDengine配置 */
    private TDengine tdengine = new TDengine();

    public enum DatabaseType {
        INFLUXDB, TDENGINE
    }

    public static class InfluxDB {
        private String url = "http://localhost:8086";
        private String token;
        private String org = "my-org";
        private String bucket = "im-messages";
        private boolean enabled = true;

        // Getters and Setters
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getOrg() {
            return org;
        }

        public void setOrg(String org) {
            this.org = org;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class TDengine {
        private String url = "jdbc:TAOS://localhost:6030/";
        private String username = "root";
        private String password = "taosdata";
        private String database = "im_messages";
        private boolean enabled = false;

        // Getters and Setters
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    // Getters and Setters
    public DatabaseType getType() {
        return type;
    }

    public void setType(DatabaseType type) {
        this.type = type;
    }

    public InfluxDB getInfluxdb() {
        return influxdb;
    }

    public void setInfluxdb(InfluxDB influxdb) {
        this.influxdb = influxdb;
    }

    public TDengine getTdengine() {
        return tdengine;
    }

    public void setTdengine(TDengine tdengine) {
        this.tdengine = tdengine;
    }
}
