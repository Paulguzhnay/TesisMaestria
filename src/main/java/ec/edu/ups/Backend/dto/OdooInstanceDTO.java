package ec.edu.ups.Backend.dto;

import ec.edu.ups.Backend.model.OdooInstance;

public class OdooInstanceDTO {

    private String name;
    private String category;
    private Long projectId;
    private String url;
    private Boolean neutralize = false;

    // ✅ Nuevos campos
    private String codeSourceCategory;
    private boolean copyDataFromProduction;

    // Getters y setters

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

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isNeutralize() {
        return neutralize != null && neutralize;
    }

    public void setNeutralize(boolean neutralize) {
        this.neutralize = neutralize;
    }

    public String getCodeSourceCategory() {
        return codeSourceCategory;
    }

    public void setCodeSourceCategory(String codeSourceCategory) {
        this.codeSourceCategory = codeSourceCategory;
    }

    public boolean isCopyDataFromProduction() {
        return copyDataFromProduction;
    }

    public void setCopyDataFromProduction(boolean copyDataFromProduction) {
        this.copyDataFromProduction = copyDataFromProduction;
    }

    // Constructor desde entidad
    public OdooInstanceDTO(OdooInstance instance) {
        this.name = instance.getName();
        this.category = instance.getCategory();
        this.projectId = instance.getProject().getId();
        this.url = instance.getUrl();
        this.neutralize = instance.isNeutralize();
        // codeSourceCategory y copyDataFromProduction no se obtienen de la entidad OdooInstance
    }

    // Constructor vacío (necesario para deserialización)
    public OdooInstanceDTO() {
    }
}
