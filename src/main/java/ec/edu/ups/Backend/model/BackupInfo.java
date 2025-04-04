package ec.edu.ups.Backend.model;

import java.time.Instant;

public class BackupInfo {
    private String name;
    private Instant time;
    private String branch;
    private String version;
    private String type;
    private String revision;

    public BackupInfo(String name, Instant time, String branch, String version, String type, String revision) {
        this.name = name;
        this.time = time;
        this.branch = branch;
        this.version = version;
        this.type = type;
        this.revision = revision;
    }

    public String getName() { return name; }
    public Instant getTime() { return time; }
    public String getBranch() { return branch; }
    public String getVersion() { return version; }
    public String getType() { return type; }
    public String getRevision() { return revision; }
}