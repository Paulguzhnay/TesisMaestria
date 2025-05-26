package ec.edu.ups.Backend.dto;

public class BackupRequest {
    private String name;
    private String category;
    private Long projectId;

    // Getters
    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public Long getProjectId() {
        return projectId;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
}
