package ec.edu.ups.Backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "odoo_instances")
public class OdooInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String category;
    private String url;

    @Column(nullable = false)
    private Integer port;

    @Column(name = "neutralize")
    private Boolean neutralize;

    @Column(name = "code_source_category")
    private String codeSourceCategory;

    @ManyToOne
    @JoinColumn(name = "project_id")
    @JsonIgnore
    private Project project;

    // === Constructores ===
    public OdooInstance() {}

    public OdooInstance(String name, String category, String url) {
        this.name = name;
        this.category = category;
        this.url = url;
    }

    public OdooInstance(String name, String category, String url, boolean neutralize, Integer port) {
        this.name = name;
        this.category = category;
        this.url = url;
        this.neutralize = neutralize;
        this.port = port;
    }

    public OdooInstance(String name, String category, String url, Boolean neutralize, String codeSourceCategory, Project project) {
        this.name = name;
        this.category = category;
        this.url = url;
        this.neutralize = neutralize;
        this.codeSourceCategory = codeSourceCategory;
        this.project = project;
    }

    // === Getters y Setters ===
    public Long getId() { return id; }

    public String getName() { return name; }

    public String getCategory() { return category; }

    public String getUrl() { return url; }

    public Integer getPort() { return port; }

    public Boolean isNeutralize() { return neutralize; }

    public String getCodeSourceCategory() { return codeSourceCategory; }

    public Project getProject() { return project; }

    public void setId(Long id) { this.id = id; }

    public void setName(String name) { this.name = name; }

    public void setCategory(String category) { this.category = category; }

    public void setUrl(String url) { this.url = url; }

    public void setPort(Integer port) { this.port = port; }

    public void setNeutralize(Boolean neutralize) { this.neutralize = neutralize; }

    public void setCodeSourceCategory(String codeSourceCategory) { this.codeSourceCategory = codeSourceCategory; }

    public void setProject(Project project) { this.project = project; }
}
