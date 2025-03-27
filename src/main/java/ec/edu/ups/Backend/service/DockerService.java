package ec.edu.ups.Backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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

    public String createOdooInstance(String instanceName) {
        try {
            // Crear los nombres completos para los contenedores
            String dbContainerName = containerPrefix + instanceName + "_db";
            String odooContainerName = containerPrefix + instanceName;

            // Comando para crear el contenedor de la base de datos
            String dbCommand = String.format(
                    "docker run -d --name %s --network %s -e POSTGRES_USER=%s -e POSTGRES_PASSWORD=%s -e POSTGRES_DB=%s %s",
                    dbContainerName, dockerNetwork, postgresUser, postgresPassword, postgresDb, postgresImage
            );

            // Comando para crear el contenedor de Odoo
            String odooCommand = String.format(
                    "docker run -d --name %s --network %s -e HOST=%s -e USER=%s -e PASSWORD=%s -e DB=%s -p 8069:8069 %s",
                    odooContainerName, dockerNetwork, dbContainerName, postgresUser, postgresPassword, postgresDb, odooImage
            );

            // Ejecutar los comandos y capturar la salida
            Process dbProcess = Runtime.getRuntime().exec(dbCommand);
            String dbOutput = getProcessOutput(dbProcess);
            int dbExitCode = dbProcess.waitFor();
            if (dbExitCode != 0) {
                return "Error al crear el contenedor de la base de datos: " + dbOutput;
            }

            Process odooProcess = Runtime.getRuntime().exec(odooCommand);
            String odooOutput = getProcessOutput(odooProcess);
            int odooExitCode = odooProcess.waitFor();
            if (odooExitCode != 0) {
                return "Error al crear el contenedor de Odoo: " + odooOutput;
            }

            // Devolver la URL para que el frontend pueda redirigir al usuario
            return "Instancia de Odoo creada con éxito: " + instanceName + " - URL: http://localhost:8069";
        } catch (IOException | InterruptedException e) {
            return "Error al crear la instancia de Odoo: " + e.getMessage();
        }
    }


    // Método para capturar la salida del proceso
    private String getProcessOutput(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        return output.toString();
    }
}
