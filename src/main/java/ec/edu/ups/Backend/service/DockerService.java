package ec.edu.ups.Backend.service;

import ec.edu.ups.Backend.model.OdooInstance;
import ec.edu.ups.Backend.repository.OdooInstanceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class DockerService {

    @Value("${docker.image}")
    private String odooImage;

    @Value("${docker.postgres.image}")
    private String postgresImage;

    @Value("${docker.postgres.user}")
    private String postgresUser;

    @Value("${docker.postgres.password}")
    private String postgresPassword;

    @Value("${docker.postgres.db}")
    private String postgresDb;

    @Value("${docker.network}")
    private String dockerNetwork;

    @Value("${docker.container.prefix}")
    private String containerPrefix;

    private final OdooInstanceRepository odooInstanceRepository;

    public DockerService(OdooInstanceRepository odooInstanceRepository) {
        this.odooInstanceRepository = odooInstanceRepository;
    }

    public String createOdooInstance(String instanceName, String category) {
        try {
            int port = getPortByCategory(category);
            if (port == -1) {
                return "Error: Categoría no válida.";
            }

            String dbContainerName = containerPrefix + category + "_" + instanceName + "_db";
            String odooContainerName = containerPrefix + category + "_" + instanceName;
            String url = "http://localhost:" + port + "/web/database/selector";

            String dbCommand = String.format(
                    "docker run -d --name %s --network %s -e POSTGRES_USER=%s -e POSTGRES_PASSWORD=%s -e POSTGRES_DB=%s %s",
                    dbContainerName, dockerNetwork, postgresUser, postgresPassword, postgresDb, postgresImage
            );

            String odooCommand = String.format(
                    "docker run -d --name %s --network %s -e HOST=%s -e USER=%s -e PASSWORD=%s -e DB=%s -p %d:8069 %s",
                    odooContainerName, dockerNetwork, dbContainerName, postgresUser, postgresPassword, postgresDb, port, odooImage
            );

            if (!executeCommand(dbCommand) || !executeCommand(odooCommand)) {
                return "Error al crear la instancia.";
            }

            // **Guardar la instancia en la base de datos**
            OdooInstance instance = new OdooInstance(instanceName, category, url);
            odooInstanceRepository.save(instance);

            return url;

        } catch (Exception e) {
            return "Error al crear la instancia: " + e.getMessage();
        }
    }

    public List<OdooInstance> getAllInstances() {
        return odooInstanceRepository.findAll();
    }

    private int getPortByCategory(String category) {
        switch (category.toLowerCase()) {
            case "development": return 8070;
            case "staging": return 8071;
            case "production": return 8069;
            default: return -1;
        }
    }

    private boolean executeCommand(String command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command);
        int exitCode = process.waitFor();
        return exitCode == 0;
    }
}
