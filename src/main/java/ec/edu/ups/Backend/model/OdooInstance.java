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

    @Column(name = "neutralize")
    private Boolean neutralize;

    @ManyToOne
    @JoinColumn(name = "project_id")
    @JsonIgnore
    private Project project; // Asociación al proyecto

    public OdooInstance() {}

    public OdooInstance(String name, String category, String url) {
        this.name = name;
        this.category = category;
        this.url = url;
    }

    // Getters y setters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getUrl() { return url; }
    public Project getProject() { return project; }
    public Boolean isNeutralize() {return neutralize;}

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    public void setUrl(String url) { this.url = url; }
    public void setProject(Project project) { this.project = project; }
    public void setNeutralize(Boolean neutralize) {this.neutralize = neutralize;}

    public OdooInstance(String name, String category, String url, Boolean neutralize) {
        this.name = name;
        this.category = category;
        this.url = url;
        this.neutralize = neutralize;
    }


}
