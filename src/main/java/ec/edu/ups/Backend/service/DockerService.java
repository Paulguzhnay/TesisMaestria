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

    @Value("${docker.image.development}")
    private String odooImageDev;

    @Value("${docker.image.staging}")
    private String odooImageStaging;

    @Value("${docker.image.production}")
    private String odooImageProd;

    @Value("${docker.postgres.shared.container}")
    private String sharedDbContainer;

    @Value("${docker.postgres.shared.dbname}")
    private String sharedDbName;





    @Value("${backup.directory}")
    private String backupDir;


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
            System.out.println("📦 Inicio metodo :createOdooInstance");

            String dbName = instanceName;  // La base tendrá el mismo nombre que la instancia
            String odooContainerName = containerPrefix + category + "_" + instanceName;
            String odooVolume = "odoo_" + instanceName + "_data";
            String odooImageByBranch = switch (category.toLowerCase()) {
                case "development" -> odooImageDev;
                case "staging" -> odooImageStaging;
                case "production" -> odooImageProd;
                default -> odooImage;
            };

            // Verifica que el contenedor de base compartida esté activo
            if (!isContainerRunning(sharedDbContainer)) {
                System.out.println("🧐 Contenedor BD no está activo. Intentando iniciar...");
                if (!executeCommand("docker start " + sharedDbContainer)) {
                    return "❌ No se pudo iniciar la base de datos compartida.";
                }
            }

            // Verificar si la base de datos ya existe
            if (checkDatabaseExists(dbName)) {
                return "❌ Ya existe una base de datos con el nombre: " + dbName;
            }

            // Crear nueva base de datos desde template-odoo (ejecutando como usuario postgres dentro del contenedor)
            String cloneDbCommand = String.format(
                    "docker exec -u postgres %s createdb -U %s %s -T template-odoo",
                    sharedDbContainer, postgresUser, dbName
            );
            if (!executeCommand(cloneDbCommand)) {
                return "❌ Error al clonar la base de datos desde template-odoo.";
            }

            // Asignar puerto dinámico disponible
            int dynamicPort = findAvailablePort();
            System.out.println("🌐 Puerto dinámico asignado: " + dynamicPort);

            String odooRunCommand = String.format(
                    "docker run -d --name %s --network %s " +
                            "-e HOST=%s -e USER=%s -e PASSWORD=%s -e DB=%s " +
                            "-p %d:8069 -v %s:/var/lib/odoo %s",
                    odooContainerName, dockerNetwork,
                    sharedDbContainer, postgresUser, postgresPassword, dbName,
                    dynamicPort, odooVolume, odooImageByBranch
            );
            System.out.println("📦 Ejecutando docker run para el contenedor Odoo...");
            String dockerOutput = runCommandWithOutput(odooRunCommand);
            if (dockerOutput == null || dockerOutput.trim().isEmpty()) {
                return "❌ El comando docker run no generó salida. Es posible que haya fallado en segundo plano.";
            }

            System.out.println("✅ Resultado de docker run:\n" + dockerOutput);

            // Construir URL de acceso
            String url = "http://localhost:" + dynamicPort + "/web/database/selector";

            // Guardar instancia en la base de datos
            OdooInstance instance = new OdooInstance();
            instance.setName(instanceName);
            instance.setCategory(category);
            instance.setUrl(url);
            odooInstanceRepository.save(instance);

            return url;

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error general al crear la instancia: " + e.getMessage();
        }
    }


    public String runCommandWithOutput(String command) {
        try {
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            StringBuilder errorOutput = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("⚠️ Error al ejecutar comando (exit=" + exitCode + "):");
                System.err.println(errorOutput);
            }

            return output.toString();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }



    public String initializeTemplateDatabase() {
        try {
            System.out.println("🔍 Verificando existencia de template-odoo...");

            // 1. Comando para verificar si ya existe
            String checkCmd = String.format(
                    "docker exec %s psql -U %s -tAc \"SELECT 1 FROM pg_database WHERE datname = 'template-odoo'\"",
                    sharedDbContainer, postgresUser
            );

            Process process = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", checkCmd});
            process.waitFor();
            String result = new String(process.getInputStream().readAllBytes()).trim();

            if ("1".equals(result)) {
                return "La base de datos template-odoo ya existe.";
            }

            // 2. Crear base de datos vacía para configurarla desde el navegador
            String createCmd = String.format(
                    "docker exec %s createdb -U %s template-odoo",
                    sharedDbContainer, postgresUser
            );

            if (!executeCommand(createCmd)) {
                return "Error al crear la base de datos template-odoo.";
            }

            return "Base de datos template-odoo creada exitosamente. Accede a cualquier instancia y selecciónala en el navegador para configurarla.";

        } catch (Exception e) {
            e.printStackTrace();
            return "Error general al inicializar template-odoo: " + e.getMessage();
        }
    }


    private boolean checkDatabaseExists(String dbName) {
        try {
            String checkCommand = String.format(
                    "docker exec %s psql -U %s -d postgres -tAc \"SELECT 1 FROM pg_database WHERE datname='%s'\"",
                    sharedDbContainer, postgresUser, dbName
            );
            Process process = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", checkCommand});
            process.waitFor();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            return output.equals("1");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private int findAvailablePort() {
        int startPort = 10000;
        int endPort = 10100;

        for (int port = startPort; port <= endPort; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        return -1; // No hay puertos disponibles
    }

    private boolean isPortAvailable(int port) {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }






    // Método para recuperar las instancias al iniciar el backend
    @PostConstruct
    public void restartOdooInstances() {
        //  Reiniciar contenedor de base de datos compartido si existe
        if (!isContainerRunning("odoo_shared_db")) {
            try {
                executeCommand("docker start odoo_shared_db");
            } catch (Exception e) {
                System.err.println("⚠️ Error al reiniciar base de datos compartida: " + e.getMessage());
            }
        }

        //  Reiniciar instancias Odoo individuales
        List<OdooInstance> instances = odooInstanceRepository.findAll();
        for (OdooInstance instance : instances) {
            String odooContainerName = containerPrefix + instance.getCategory() + "_" + instance.getName();
            if (!isContainerRunning(odooContainerName)) {
                try {
                    executeCommand("docker start " + odooContainerName);
                } catch (Exception e) {
                    System.err.println("⚠️ Error al reiniciar instancia: " + odooContainerName);
                }
            }
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
        System.out.println("🧪 Ejecutando comando: " + command);
        Process process = Runtime.getRuntime().exec(command);
        int exitCode = process.waitFor();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[Docker Output] " + line);
            }
        }

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
        List<OdooInstance> instances = odooInstanceRepository.findAll();

        for (OdooInstance instance : instances) {
            String containerName = containerPrefix + instance.getCategory() + "_" + instance.getName();
            try {
                String inspectCmd = String.format(
                        "docker inspect --format=\"{{(index (index .NetworkSettings.Ports \\\"8069/tcp\\\") 0).HostPort}}\" %s",
                        containerName
                );

                Process process = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", inspectCmd});
                process.waitFor();
                byte[] output = process.getInputStream().readAllBytes();
                String assignedPort = new String(output).trim();

                if (!assignedPort.isEmpty()) {
                    String newUrl = "http://localhost:" + assignedPort + "/web/database/selector";
                    instance.setUrl(newUrl);
                }
            } catch (Exception e) {
                System.err.println("⚠️ Error al inspeccionar contenedor " + containerName + ": " + e.getMessage());
            }
        }

        return instances;
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

            // Declarar timestamp
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
        String dbContainerName = sharedDbContainer; // Siempre la base de datos compartida
        String odooContainerName = getContainerName(instanceName, category, false);

        if (odooContainerName == null) {
            return "Error: No se encontró el contenedor Odoo para la instancia.";
        }

        File backupDirFile = new File(backupDir);
        if (!backupDirFile.exists() && !backupDirFile.mkdirs()) {
            return "Error: No se pudo crear la carpeta de backups.";
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String dbBackupFilePath = backupDir + File.separator + String.format("db_backup_%s_%s_%s.dump", category.toUpperCase(), instanceName, timestamp);
        String odooBackupFilePath = backupDir + File.separator + String.format("odoo_data_%s_%s_%s.tar.gz", category.toUpperCase(), instanceName, timestamp);

        try {
            // 📦 1. Backup base de datos (solo la BD de la instancia)
            String dbBackupPathInContainer = "/tmp/restore.dump";
            String dbDumpCommand = String.format(
                    "docker exec %s pg_dump -U %s -d %s -Fc -f %s",
                    dbContainerName, postgresUser, instanceName, dbBackupPathInContainer
            );
            String dbCopyCommand = String.format(
                    "docker cp %s:%s \"%s\"",
                    dbContainerName, dbBackupPathInContainer, dbBackupFilePath
            );

            // 📦 2. Backup archivos de Odoo
            String odooBackupPathInContainer = "/tmp/restore.tar.gz";
            String odooBackupCommand = String.format(
                    "docker exec %s tar -czf %s -C /var/lib/odoo .",
                    odooContainerName, odooBackupPathInContainer
            );
            String dockerCopyCommand = String.format(
                    "docker cp %s:%s \"%s\"",
                    odooContainerName, odooBackupPathInContainer, odooBackupFilePath
            );

            boolean dbDumpSuccess = executeCommand(new String[]{"cmd.exe", "/c", dbDumpCommand});
            boolean dbCopySuccess = executeCommand(new String[]{"cmd.exe", "/c", dbCopyCommand});
            boolean odooCreateSuccess = executeCommand(new String[]{"cmd.exe", "/c", odooBackupCommand});
            boolean odooCopySuccess = executeCommand(new String[]{"cmd.exe", "/c", dockerCopyCommand});

            if (dbDumpSuccess && dbCopySuccess && odooCreateSuccess && odooCopySuccess) {
                return "Backup creado exitosamente para la instancia: " + instanceName;
            } else {
                return "Error: Falló el backup de BD o de Odoo.";
            }

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
        String dbContainer = sharedDbContainer; // Base de datos compartida
        String odooContainer = getContainerName(instanceName, category, false);

        if (odooContainer == null) {
            return "Error: Contenedor de Odoo no encontrado.";
        }

        String dbFile = backupDir + File.separator + dbBackupFileName;
        String odooFile = backupDir + File.separator + odooBackupFileName;

        if (!new File(dbFile).exists() || !new File(odooFile).exists()) {
            return "Archivos de backup no encontrados.";
        }

        try {
            // 🔁 Restaurar solo la BD con nombre = instanceName
            String terminateCmd = String.format(
                    "docker exec %s psql -U %s -d postgres -c \"SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '%s' AND pid <> pg_backend_pid();\"",
                    dbContainer, postgresUser, instanceName);
            String dropCmd = String.format("docker exec %s dropdb -U %s %s", dbContainer, postgresUser, instanceName);
            String createCmd = String.format("docker exec %s createdb -U %s %s", dbContainer, postgresUser, instanceName);
            String copyDbCmd = String.format("docker cp \"%s\" %s:/tmp/restore.dump", dbFile, dbContainer);
            String restoreCmd = String.format(
                    "docker exec %s pg_restore -U %s -d %s --clean --if-exists --verbose /tmp/restore.dump",
                    dbContainer, postgresUser, instanceName);

            boolean dbRestored =
                    executeCommand(new String[]{"cmd.exe", "/c", terminateCmd}) &&
                            executeCommand(new String[]{"cmd.exe", "/c", dropCmd}) &&
                            executeCommand(new String[]{"cmd.exe", "/c", createCmd}) &&
                            executeCommand(new String[]{"cmd.exe", "/c", copyDbCmd}) &&
                            executeCommand(new String[]{"cmd.exe", "/c", restoreCmd});

            // 📁 Restaurar archivos de Odoo
            String copyOdooCmd = String.format("docker cp \"%s\" %s:/tmp/restore.tar.gz", odooFile, odooContainer);
            String extractOdooCmd = String.format("docker exec %s tar -xzf /tmp/restore.tar.gz -C /var/lib/odoo", odooContainer);

            boolean odooRestored =
                    executeCommand(new String[]{"cmd.exe", "/c", copyOdooCmd}) &&
                            executeCommand(new String[]{"cmd.exe", "/c", extractOdooCmd});

            return (dbRestored && odooRestored)
                    ? "Restauración completada en la instancia " + instanceName
                    : "Falló la restauración en la instancia " + instanceName;

        } catch (Exception e) {
            e.printStackTrace();
            return "Error durante el restore: " + e.getMessage();
        }
    }


    public String exportTemplateDatabase() {
        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String backupFileName = "template-odoo_" + timestamp + ".dump";
            String backupFilePath = backupDir + File.separator + backupFileName;

            String dumpCmd = String.format(
                    "docker exec %s pg_dump -U %s -d template-odoo -Fc -f /tmp/template.dump",
                    sharedDbContainer, postgresUser
            );
            String copyCmd = String.format(
                    "docker cp %s:/tmp/template.dump \"%s\"",
                    sharedDbContainer, backupFilePath
            );

            boolean dumped = executeCommand(new String[]{"cmd.exe", "/c", dumpCmd});
            boolean copied = executeCommand(new String[]{"cmd.exe", "/c", copyCmd});

            if (dumped && copied) {
                return "Template exportado exitosamente: " + backupFileName;
            } else {
                return "Error al exportar el template.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error general al exportar template: " + e.getMessage();
        }
    }
    public String importTemplateDatabase(String dumpFileName) {
        try {
            File file = new File(backupDir, dumpFileName);
            if (!file.exists()) {
                return "El archivo especificado no existe: " + dumpFileName;
            }

            String terminateCmd = String.format(
                    "docker exec %s psql -U %s -d postgres -c \"SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'template-odoo' AND pid <> pg_backend_pid();\"",
                    sharedDbContainer, postgresUser
            );
            String dropCmd = String.format(
                    "docker exec %s dropdb -U %s template-odoo",
                    sharedDbContainer, postgresUser
            );
            String createCmd = String.format(
                    "docker exec %s createdb -U %s template-odoo",
                    sharedDbContainer, postgresUser
            );
            String copyCmd = String.format(
                    "docker cp \"%s\" %s:/tmp/template.dump",
                    file.getAbsolutePath(), sharedDbContainer
            );
            String restoreCmd = String.format(
                    "docker exec %s pg_restore -U %s -d template-odoo --clean --if-exists /tmp/template.dump",
                    sharedDbContainer, postgresUser
            );

            boolean terminated = executeCommand(new String[]{"cmd.exe", "/c", terminateCmd});
            boolean dropped = executeCommand(new String[]{"cmd.exe", "/c", dropCmd});
            boolean created = executeCommand(new String[]{"cmd.exe", "/c", createCmd});
            boolean copied = executeCommand(new String[]{"cmd.exe", "/c", copyCmd});
            boolean restored = executeCommand(new String[]{"cmd.exe", "/c", restoreCmd});

            return (terminated && dropped && created && copied && restored)
                    ? "Template restaurado exitosamente desde " + dumpFileName
                    : "Error al restaurar template desde " + dumpFileName;

        } catch (Exception e) {
            e.printStackTrace();
            return "Error general al importar template: " + e.getMessage();
        }
    }

}
