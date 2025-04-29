package ec.edu.ups.Backend.model;

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

    public OdooInstance() {}

    public OdooInstance(String name, String category, String url) {
        this.name = name;
        this.category = category;
        this.url = url;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getUrl() { return url; }
}