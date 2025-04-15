package ec.edu.ups.Backend.service;

import ec.edu.ups.Backend.model.BackupInfo;
import ec.edu.ups.Backend.model.OdooInstance;
import ec.edu.ups.Backend.repository.OdooInstanceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;



@Service
public class DockerService {
    private List<String> backups = new ArrayList<>();
    String backupDir = "C:\\Users\\paul-\\2025\\Maestria\\Tesis\\Backups";


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

    // Método para crear una nueva instancia de Odoo con persistencia
    public String createOdooInstance(String instanceName, String category) {
        try {
            int port = getPortByCategory(category);
            if (port == -1) {
                return "Error: Categoría no válida.";
            }

            String dbContainerName = containerPrefix + category + "_" + instanceName + "_db";

            String odooContainerName = containerPrefix + category + "_" + instanceName;
            String url = "http://localhost:" + port + "/web/database/selector";

            //  Usar volúmenes persistentes
            String dbVolume = "odoo_" + instanceName + "_db_data";
            String odooVolume = "odoo_" + instanceName + "_data";

            String dbCommand = String.format(
                    "docker run -d --name %s --network %s " +
                            "-e POSTGRES_USER=%s -e POSTGRES_PASSWORD=%s -e POSTGRES_DB=%s " +
                            "-v %s:/var/lib/postgresql/data " +
                            "%s",
                    dbContainerName, dockerNetwork, postgresUser, postgresPassword, postgresDb, dbVolume, postgresImage
            );

            String odooCommand = String.format(
                    "docker run -d --name %s --network %s " +
                            "-e HOST=%s -e USER=%s -e PASSWORD=%s -e DB=%s " +
                            "-p %d:8069 " +
                            "-v %s:/var/lib/odoo " +
                            "%s",
                    odooContainerName, dockerNetwork, dbContainerName, postgresUser, postgresPassword, postgresDb, port, odooVolume, odooImage
            );

            if (!executeCommand(dbCommand) || !executeCommand(odooCommand)) {
                return "Error al crear la instancia.";
            }

            //  Guardar la instancia en la base de datos
            OdooInstance instance = new OdooInstance(instanceName, category, url);
            odooInstanceRepository.save(instance);

            return url;

        } catch (Exception e) {
            return "Error al crear la instancia: " + e.getMessage();
        }
    }

    // Método para recuperar las instancias al iniciar el backend
    @PostConstruct
    public void restartOdooInstances() {
        List<OdooInstance> instances = odooInstanceRepository.findAll();
        for (OdooInstance instance : instances) {
            restartInstance(instance);
        }
    }

    private void restartInstance(OdooInstance instance) {
        String dbContainerName = containerPrefix + instance.getCategory() + "_" + instance.getName() + "_db";
        String odooContainerName = containerPrefix + instance.getCategory() + "_" + instance.getName();
        int port = getPortByCategory(instance.getCategory());

        if (port == -1) return;

        // Verificar si los contenedores ya están corriendo
        if (isContainerRunning(dbContainerName) && isContainerRunning(odooContainerName)) {
            return;
        }

        try {
            // Reiniciar los contenedores usando los volúmenes existentes
            executeCommand("docker start " + dbContainerName);
            executeCommand("docker start " + odooContainerName);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private boolean isContainerRunning(String containerName) {
        try {
            Process process = Runtime.getRuntime().exec("docker inspect -f \"{{.State.Running}}\" " + containerName);
            process.waitFor();
            byte[] output = process.getInputStream().readAllBytes();
            String result = new String(output).trim();
            System.out.println("🧐 Estado del contenedor " + containerName + ": " + result);
            return result.equals("true");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public List<OdooInstance> getAllInstances() {
        return odooInstanceRepository.findAll();
    }

    public boolean backupOdooInstance(String instanceName, String category) {
        try {
            String dbContainerName = containerPrefix + category + "_" + instanceName + "_db";
            String odooContainerName = containerPrefix + category + "_" + instanceName;
            System.out.println("********************"+dbContainerName);
            System.out.println("********************"+odooContainerName);

            // Directorio donde se guardarán los backups
            String backupDir = "/backups/odoo/";
            new File(backupDir).mkdirs();  // Crear directorio si no existe

            // ✅ Declarar timestamp
            String timestamp = String.valueOf(System.currentTimeMillis());

            // Backup de la base de datos
            String dbBackupCommand = String.format(
                    "docker exec %s pg_dump -U %s %s > %s%s_db_backup.sql",
                    dbContainerName, postgresUser, postgresDb, backupDir, instanceName
            );

            // Backup del volumen de Odoo
            String odooBackupCommand = String.format(
                    "docker run --rm --volumes-from %s -v %s:/backup busybox tar czf /backup/%s",
                    odooContainerName, backupDir, "odoo_data_" + timestamp + ".tar.gz"
            );

            // Ejecutar los comandos
            boolean dbBackupSuccess = executeCommand(new String[]{"/bin/sh", "-c", dbBackupCommand});
            boolean odooBackupSuccess = executeCommand(new String[]{"/bin/sh", "-c", odooBackupCommand});

            return dbBackupSuccess && odooBackupSuccess;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean executeCommand(String[] command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true); // Mezclar stderr con stdout
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[OUTPUT] " + line);
            }
        }

        int exitCode = process.waitFor();
        System.out.println("↪️ Exit code: " + exitCode);
        return exitCode == 0;
    }

    public List<BackupInfo> getBackups() {
        File directory = new File(backupDir);

        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                System.err.println("No se pudo crear la carpeta de backups: " + backupDir);
                return List.of();
            }
        }

        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            return List.of();
        }

        return Arrays.stream(files)
                .filter(File::isFile)
                .map(file -> {
                    String name = file.getName();

                    // Extraer timestamp del nombre del archivo (ejemplo: Backup-1743698454290.zip)
                    long timestamp = name.matches(".*\\d+.*") ?
                            Long.parseLong(name.replaceAll("\\D", "")) :
                            file.lastModified();

                    return new BackupInfo(
                            name,
                            Instant.ofEpochMilli(timestamp),
                            "Desconocido",  // No se puede determinar desde el nombre del archivo
                            "15.0",         // Suposición, modificar según sea necesario
                            name.contains("Auto") ? "Automático" : "Manual",
                            "N/A"           // No hay información de revisión
                    );
                })
                .collect(Collectors.toList());
    }




    public String createBackup(String instanceName, String category) {
        String dbContainerName = getContainerName(instanceName, category, true);
        String odooContainerName = getContainerName(instanceName, category, false);
        String dbName = instanceName; // Usar nombre de instancia como nombre de BD

        System.out.println("🔍 Buscando contenedores...");
        System.out.println("📦 Base de Datos: " + dbContainerName);
        System.out.println("📦 Odoo: " + odooContainerName);

        if (dbContainerName == null || odooContainerName == null) {
            return "Error: No se encontraron los contenedores para la instancia.";
        }

        File backupDirFile = new File(backupDir);
        if (!backupDirFile.exists() && !backupDirFile.mkdirs()) {
            return "Error: No se pudo crear la carpeta de backups.";
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String dbBackupFilePath = backupDir + File.separator + String.format("db_backup_%s_%s_%s.dump", category.toUpperCase(), instanceName, timestamp);
        System.out.println("dbBackupFilePath: "+dbBackupFilePath+" Fin");
        String odooBackupFilePath = backupDir + File.separator + String.format("odoo_data_%s_%s_%s.tar.gz", category.toUpperCase(), instanceName, timestamp);
        System.out.println("odooBackupFilePath: "+odooBackupFilePath+" Fin");

        try {
            // Backup base de datos
            String dbBackupPathInContainer = String.format("/tmp/db_backup_%s_%s_%s.dump", category.toUpperCase(), instanceName, timestamp);
            String dbDumpCommand = String.format(
                    "docker exec %s pg_dump -U %s -d %s -Fc -f %s",
                    dbContainerName, postgresUser, instanceName, dbBackupPathInContainer
            );

            System.out.println("dbDumpCommand: "+dbDumpCommand+" Fin");

            String dbCopyCommand = String.format(
                    "docker cp %s:%s \"%s\"",
                    dbContainerName, dbBackupPathInContainer, dbBackupFilePath
            );
            System.out.println("dbCopyCommand: "+dbCopyCommand+" Fin");

            // Backup de Odoo
            String odooBackupPathInContainer = String.format("/tmp/odoo_data_%s_%s_%s.tar.gz", category.toUpperCase(), instanceName, timestamp);
            String odooBackupCommand = "docker exec " + odooContainerName +
                    " tar -czf " + odooBackupPathInContainer + " -C /var/lib/odoo .";
            String dockerCopyCommand = "docker cp " + odooContainerName + ":" + odooBackupPathInContainer +
                    " \"" + odooBackupFilePath + "\"";
            System.out.println("odooBackupPathInContainer: "+odooBackupPathInContainer+" Fin");
            System.out.println("odooBackupCommand: "+odooBackupCommand+" Fin");
            System.out.println("dockerCopyCommand: "+dockerCopyCommand+" Fin");

            boolean dbDumpSuccess = executeCommand(new String[]{"cmd.exe", "/c", dbDumpCommand});
            boolean dbCopySuccess = executeCommand(new String[]{"cmd.exe", "/c", dbCopyCommand});
            boolean dbSuccess = dbDumpSuccess && dbCopySuccess;




            boolean odooCreateSuccess = executeCommand(new String[]{"cmd.exe", "/c", odooBackupCommand});
            boolean odooCopySuccess = executeCommand(new String[]{"cmd.exe", "/c", dockerCopyCommand});
            boolean odooSuccess = odooCreateSuccess && odooCopySuccess;

            System.out.println("📌 Backup BD: " + (dbSuccess ? "✅ Éxito" : "❌ Falló"));
            System.out.println("📌 Backup Odoo: " + (odooSuccess ? "✅ Éxito" : "❌ Falló"));

            if (!dbSuccess || !odooSuccess) {
                return "Error: No se pudo realizar el backup.";
            }

            return "Backup creado exitosamente para la instancia: " + instanceName;

        } catch (Exception e) {
            e.printStackTrace();
            return "Error al crear el backup: " + e.getMessage();
        }
    }









    //-----------------------
    private String getContainerName(String instanceName, String category, boolean isDatabase) {
        try {
            String filter = isDatabase ? "_db" : "";
            String command = String.format(
                    "docker ps --format \"{{.Names}}\" --filter \"name=%s%s\"",
                    containerPrefix + category + "_" + instanceName, filter
            );

            Process process = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", command});
            process.waitFor();
            byte[] output = process.getInputStream().readAllBytes();
            String result = new String(output).replace("\r", "").trim();

            // Tomar solo el primer nombre si hay más de uno
            String containerName = result.contains("\n") ? result.split("\n")[0].trim() : result.trim();
            System.out.println("⚠️ Contenedor encontrado (" + (isDatabase ? "DB" : "Odoo") + "): [" + containerName + "]");

            return containerName.isEmpty() ? null : containerName;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    //-----------------v1

    public String restoreBackup(String instanceName, String category, String dbBackupFileName, String odooBackupFileName) {
        String dbContainer = getContainerName(instanceName, category, true);
        String odooContainer = getContainerName(instanceName, category, false);

        System.out.println("dbBackupFileName: " + dbBackupFileName);
        System.out.println("odooBackupFileName: " + odooBackupFileName);
        System.out.println("📦 DB: " + dbContainer);
        System.out.println("📦 Odoo: " + odooContainer);

        String dbFile = backupDir + File.separator + dbBackupFileName;
        String odooFile = backupDir + File.separator + odooBackupFileName;

        System.out.println("dbFile: "+dbFile);
        System.out.println("odooFile " +odooFile);

        if (!new File(dbFile).exists() || !new File(odooFile).exists()) {
            return "❌ Archivos de backup no encontrados.";
        }

        try {
            // 🔁 1. Cerrar conexiones activas
            String terminateCmd = String.format(
                    "docker exec %s psql -U %s -d postgres -c \"SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '%s' AND pid <> pg_backend_pid();\"",
                    dbContainer, postgresUser, instanceName);

            System.out.println("terminateCmd "+terminateCmd);

            // 🗑️ 2. Eliminar base de datos
            String dropCmd = String.format("docker exec %s dropdb -U %s %s", dbContainer, postgresUser, instanceName);
            System.out.println("dropCmd "+dropCmd);
            // 🆕 3. Crear base de datos vacía
            String createCmd = String.format("docker exec %s createdb -U %s %s", dbContainer, postgresUser, instanceName);
            System.out.println("createCmd "+createCmd);
            // 📂 4. Copiar backup .dump al contenedor
            String copyDbCmd = String.format("docker cp \"%s\" %s:/tmp/restore.dump", dbFile, dbContainer);
            System.out.println("copyDbCmd "+copyDbCmd);
            // ♻️ 5. Restaurar con pg_restore
            String restoreCmd = String.format(
                    "docker exec %s pg_restore -U %s -d %s --clean --if-exists --verbose /tmp/restore.dump",
                    dbContainer, postgresUser, instanceName);
            System.out.println("restoreCmd "+restoreCmd);
            // Ejecutar comandos paso a paso
            boolean dbRestored =
                    executeCommand(new String[]{"cmd.exe", "/c", terminateCmd}) &&
                            executeCommand(new String[]{"cmd.exe", "/c", dropCmd}) &&
                            executeCommand(new String[]{"cmd.exe", "/c", createCmd}) &&
                            executeCommand(new String[]{"cmd.exe", "/c", copyDbCmd}) &&
                            executeCommand(new String[]{"cmd.exe", "/c", restoreCmd});

            System.out.println("dbRestored: " + dbRestored);

            // 📁 6. Copiar backup de archivos de Odoo
            String copyOdooCmd = String.format("docker cp \"%s\" %s:/tmp/restore.tar.gz", odooFile, odooContainer);
            String extractOdooCmd = String.format("docker exec %s tar -xzf /tmp/restore.tar.gz -C /var/lib/odoo", odooContainer);
            System.out.println("copyOdooCmd "+copyOdooCmd);
            System.out.println(" extractOdooCmd "+extractOdooCmd);
            boolean odooRestored =
                    executeCommand(new String[]{"cmd.exe", "/c", copyOdooCmd}) &&
                            executeCommand(new String[]{"cmd.exe", "/c", extractOdooCmd});

            System.out.println("odooRestored: " + odooRestored);

            return (dbRestored && odooRestored)
                    ? "✅ Restauración completada en la instancia " + instanceName
                    : "❌ Falló la restauración en la instancia " + instanceName;

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error durante el restore: " + e.getMessage();
        }
    }

}
