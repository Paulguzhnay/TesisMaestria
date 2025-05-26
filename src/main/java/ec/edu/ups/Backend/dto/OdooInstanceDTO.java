package ec.edu.ups.Backend.dto;

import ec.edu.ups.Backend.model.OdooInstance;

public class OdooInstanceDTO {
    private String name;
    private String category;
    private Long projectId;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // Constructor
    public OdooInstanceDTO(OdooInstance instance) {
        this.name = instance.getName();
        this.category = instance.getCategory();
        this.projectId = instance.getProject().getId();


    }
}

