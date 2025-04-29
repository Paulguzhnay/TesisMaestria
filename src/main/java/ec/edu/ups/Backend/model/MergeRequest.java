package ec.edu.ups.Backend.model;

public class MergeRequest {

    private String source;
    private String target;
    private String category;

    public MergeRequest() {
    }

    public MergeRequest(String source, String target, String category) {
        this.source = source;
        this.target = target;
        this.category = category;
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

    public void setTarget(String target) {
        this.target = target;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
