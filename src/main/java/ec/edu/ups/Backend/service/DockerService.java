package ec.edu.ups.Backend.service;

import ec.edu.ups.Backend.model.BackupInfo;
import ec.edu.ups.Backend.model.OdooInstance;
import ec.edu.ups.Backend.repository.OdooInstanceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
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

    // ✅ Método para crear una nueva instancia de Odoo con persistencia
    public String createOdooInstance(String instanceName, String category) {
        try {
            int port = getPortByCategory(category);
            if (port == -1) {
                return "Error: Categoría no válida.";
            }

            String dbContainerName = containerPrefix + category + "_" + instanceName + "_db";

            String odooContainerName = containerPrefix + category + "_" + instanceName;
            String url = "http://localhost:" + port + "/web/database/selector";

            // ✅ Usar volúmenes persistentes
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

            // ✅ Guardar la instancia en la base de datos
            OdooInstance instance = new OdooInstance(instanceName, category, url);
            odooInstanceRepository.save(instance);

            return url;

        } catch (Exception e) {
            return "Error al crear la instancia: " + e.getMessage();
        }
    }

    // ✅ Método para recuperar las instancias al iniciar el backend
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
            Process process = Runtime.getRuntime().exec("docker inspect -f '{{.State.Running}}' " + containerName);
            process.waitFor();
            byte[] output = process.getInputStream().readAllBytes();
            return new String(output).trim().equals("true");
        } catch (Exception e) {
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
        Process process = new ProcessBuilder(command).start();
        int exitCode = process.waitFor();
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
        String dbBackupFilePath = backupDir + File.separator + "db_backup_" + timestamp + ".sql";
        String odooBackupFilePath = backupDir + File.separator + "odoo_data_" + timestamp + ".tar.gz";
        String finalBackupFile = backupDir + File.separator + "Backup-" + timestamp + ".zip";

        try {
            // ✅ Backup base de datos
            System.out.println("✅ Ejecutando backup de la base de datos...");
            String dbBackupCommand = String.format(
                    "docker exec %s pg_dump -U %s %s > \"%s\"",
                    dbContainerName, postgresUser, postgresDb, dbBackupFilePath
            );

            // ✅ Backup de Odoo (dentro del contenedor)
            String odooBackupPathInContainer = "/tmp/odoo_backup_" + timestamp + ".tar.gz";
            String odooBackupCommand = "docker exec " + odooContainerName +
                    " tar -czf " + odooBackupPathInContainer + " -C /var/lib/odoo .";

            // ✅ Copiar archivo desde el contenedor al host
            String dockerCopyCommand = "docker cp " + odooContainerName + ":" + odooBackupPathInContainer +
                    " \"" + odooBackupFilePath + "\"";

            // 👉 DEBUG
            System.out.println("Comando odooBackupCommand: [" + odooBackupCommand + "]");
            System.out.println("Comando dockerCopyCommand: [" + dockerCopyCommand + "]");

            boolean dbSuccess = executeCommand(new String[]{"cmd.exe", "/c", dbBackupCommand});
            boolean odooCreateSuccess = executeCommand(new String[]{"cmd.exe", "/c", odooBackupCommand});
            boolean odooCopySuccess = executeCommand(new String[]{"cmd.exe", "/c", dockerCopyCommand});

            boolean odooSuccess = odooCreateSuccess && odooCopySuccess;
            System.out.println("Comando odooSuccess: " + odooSuccess + " **FIN");

            System.out.println("📊 Resultados:");
            System.out.println("📌 Backup BD: " + (dbSuccess ? "✅ Éxito" : "❌ Falló"));
            System.out.println("📌 Backup Odoo: " + (odooSuccess ? "✅ Éxito" : "❌ Falló"));

            if (!dbSuccess || !odooSuccess) {
                return "Error: No se pudo realizar el backup.";
            }

            return "Backup creado exitosamente: " + finalBackupFile;

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


    //-----------------
    public String restoreBackup(String instanceName, String category, String fileName) {
        String dbContainer = getContainerName(instanceName, category, true);
        String odooContainer = getContainerName(instanceName, category, false);

        if (dbContainer == null || odooContainer == null) {
            return "Error: Contenedores no encontrados.";
        }

        File backupFile = new File(backupDir, fileName);
        if (!backupFile.exists()) {
            return "Error: El archivo no existe.";
        }

        try {
            if (fileName.endsWith(".tar.gz")) {
                // Restaurar archivos Odoo
                String containerPath = "/tmp/" + fileName;
                String restoreCommand = String.format(
                        "docker cp \"%s\" %s:%s", backupFile.getAbsolutePath(), odooContainer, containerPath);
                String extractCommand = String.format(
                        "docker exec %s tar -xzf %s -C /var/lib/odoo", odooContainer, containerPath);

                return (executeCommand(new String[]{"cmd.exe", "/c", restoreCommand}) &&
                        executeCommand(new String[]{"cmd.exe", "/c", extractCommand}))
                        ? "Archivos Odoo restaurados correctamente." : "Error al restaurar archivos Odoo.";

            } else if (fileName.endsWith(".sql")) {
                // Restaurar base de datos
                String containerPath = "/tmp/" + fileName;
                String copyCommand = String.format(
                        "docker cp \"%s\" %s:%s", backupFile.getAbsolutePath(), dbContainer, containerPath);
                String restoreCommand = String.format(
                        "docker exec %s psql -U %s -d %s -f %s", dbContainer, postgresUser, postgresDb, containerPath);

                return (executeCommand(new String[]{"cmd.exe", "/c", copyCommand}) &&
                        executeCommand(new String[]{"cmd.exe", "/c", restoreCommand}))
                        ? "Base de datos restaurada correctamente." : "Error al restaurar base de datos.";
            } else {
                return "Tipo de archivo no soportado.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error al restaurar backup: " + e.getMessage();
        }
    }









}
