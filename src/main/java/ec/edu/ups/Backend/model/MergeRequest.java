package ec.edu.ups.Backend.model;

public class MergeRequest {
    private Long projectId;
    private String source;

    private String target;


    // Getters y setters
    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }



    public String getTarget() {
        return target;
    }



}
