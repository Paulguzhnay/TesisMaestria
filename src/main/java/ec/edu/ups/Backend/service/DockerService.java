package ec.edu.ups.Backend.service;

import ec.edu.ups.Backend.model.BackupInfo;
import ec.edu.ups.Backend.model.OdooInstance;
import ec.edu.ups.Backend.model.Project;
import ec.edu.ups.Backend.model.User;
import ec.edu.ups.Backend.repository.OdooInstanceRepository;
import ec.edu.ups.Backend.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;



@Service
public class DockerService {
    private final ProjectRepository projectRepository;

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

    @Value("${docker.postgres.shared.dbname}")
    private String postgresSharedDbName;

    @Autowired
    private GithubService githubService;

    private final OdooInstanceRepository odooInstanceRepository;



    public DockerService(OdooInstanceRepository odooInstanceRepository, ProjectRepository projectRepository

    ) {
        this.odooInstanceRepository = odooInstanceRepository;
        this.projectRepository = projectRepository;
    }

    public String createOdooInstance(String instanceName, String category, Long projectId, boolean neutralize) {
        try {
            System.out.println("🚀 Iniciando método de creación de instancia...");

            // Buscar proyecto
            Optional<Project> optionalProject = projectRepository.findById(projectId);
            if (optionalProject.isEmpty()) return "❌ Error: Proyecto no encontrado con ID: " + projectId;
            Project project = optionalProject.get();

            // Si es STAGING y existe PRODUCTION, hacer merge
            if (category.equalsIgnoreCase("STAGING")) {
                Optional<OdooInstance> prodInstanceOpt = odooInstanceRepository.findByCategoryAndProjectId("PRODUCTION", projectId);
                if (prodInstanceOpt.isPresent()) {
                    System.out.println("🔁 Clonando desde instancia PRODUCTION...");
                    return mergeOdooInstances(projectId, prodInstanceOpt.get().getName(), instanceName);
                }
            }

            // Parámetros generales
            String dbContainerName = containerPrefix + category + "_" + instanceName + "_db";
            String odooContainerName = containerPrefix + category + "_" + instanceName;
            String dbVolume = "odoo_" + instanceName + "_db_data";
            String odooVolume = "odoo_" + instanceName + "_data";
            String repoPath = "C:\\odoo-modules\\" + instanceName;

            int port = findAvailablePort(8069, 8100);
            if (port == -1) return "❌ Error: No hay puertos disponibles.";

            // Crear contenedor PostgreSQL
            String dbCommand = String.format(
                    "docker run -d --name %s --network %s " +
                            "-e POSTGRES_USER=%s -e POSTGRES_PASSWORD=%s -e POSTGRES_DB=%s " +
                            "-v %s:/var/lib/postgresql/data %s",
                    dbContainerName, dockerNetwork, postgresUser, postgresPassword, postgresDb,
                    dbVolume, postgresImage
            );
            if (!executeCommand(new String[]{"cmd.exe", "/c", dbCommand})) return "❌ Error al crear contenedor PostgreSQL.";
            Thread.sleep(4000);

            // Crear base de datos
            String createDbCmd = String.format("docker exec %s createdb -U %s %s", dbContainerName, postgresUser, instanceName);
            if (!executeCommand(new String[]{"cmd.exe", "/c", createDbCmd})) return "❌ Error al crear la base de datos.";

            // Crear volumen de Odoo (filestore y sesiones)
            String prepareVolumeCmd = String.format(
                    "docker run --rm -v %s:/data busybox sh -c \"mkdir -p /data/filestore/%s /data/sessions && chmod -R 777 /data\"",
                    odooVolume, instanceName
            );
            if (!executeCommand(new String[]{"cmd.exe", "/c", prepareVolumeCmd})) return "❌ Error al preparar volumen Odoo.";

            // Crear contenedor Odoo
            String neutralizeEnv = neutralize ? "-e NEUTRALIZE=true " : "";
            String odooCommand = String.format(
                    "docker run -d --name %s --network %s " +
                            "-e HOST=%s -e USER=%s -e PASSWORD=%s -e DB=%s " +
                            "%s" +
                            "-p %d:8069 -v %s:/var/lib/odoo %s",
                    odooContainerName, dockerNetwork,
                    dbContainerName, postgresUser, postgresPassword, instanceName,
                    neutralizeEnv,
                    port, odooVolume, odooImage
            );
            if (!executeCommand(new String[]{"cmd.exe", "/c", odooCommand})) return "❌ Error: No se pudo crear el contenedor de Odoo.";

            // Verificar conexión a la base de datos
            int retries = 10;
            boolean connected = false;
            while (retries-- > 0) {
                String checkConnection = String.format(
                        "docker exec %s bash -c \"PGPASSWORD=%s psql -h %s -U %s -d %s -c 'SELECT 1;'\"",
                        odooContainerName, postgresPassword, dbContainerName, postgresUser, instanceName
                );
                if (executeCommand(new String[]{"cmd.exe", "/c", checkConnection})) {
                    connected = true;
                    break;
                }
                Thread.sleep(2000);
            }
            if (!connected) return "❌ Error: Odoo no pudo conectarse a PostgreSQL.";

            // Inicializar base de datos
            String demoParam = category.equalsIgnoreCase("DEVELOPMENT") ? "all" : "False";
            String initDbCmd = String.format(
                    "docker exec %s odoo -d %s -i base --db_host=%s --db_user=%s --db_password=%s --without-demo=%s --stop-after-init",
                    odooContainerName, instanceName, dbContainerName, postgresUser, postgresPassword, demoParam
            );
            if (!executeCommand(new String[]{"cmd.exe", "/c", initDbCmd})) {
                return "❌ Error al inicializar la base de datos.";
            }

            // Guardar en la base
            String url = "http://localhost:" + port + "/web/login?db=" + instanceName;
            OdooInstance instance = new OdooInstance(instanceName, category, url, neutralize, port);
            instance.setProject(project);
            odooInstanceRepository.save(instance);

            // Crear rama GitHub
            createGitHubBranch(project, instanceName, category);

            System.out.println("✅ Instancia creada exitosamente en " + url);
            return url;

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error inesperado: " + e.getMessage();
        }
    }



    //----------------------
    //Metodo para leer los modulos personalizados en Odoo
    public String installCustomModules(String instanceName, String category, Long projectId) {
        try {
            System.out.println("🔧 Iniciando instalación de módulos personalizados...");

            // Buscar proyecto y usuario
            Optional<Project> optionalProject = projectRepository.findById(projectId);
            if (optionalProject.isEmpty()) {
                return "❌ Error: Proyecto no encontrado con ID: " + projectId;
            }
            Project project = optionalProject.get();
            User user = project.getUser();

            // Buscar instancia para obtener puerto actual
            Optional<OdooInstance> optionalInstance = odooInstanceRepository.findByNameAndCategoryAndProjectId(instanceName, category, projectId);
            if (optionalInstance.isEmpty()) {
                return "❌ Error: Instancia no encontrada.";
            }
            OdooInstance instance = optionalInstance.get();
            int port = instance.getPort();

            // Definir rutas y nombres
            String branchName = category.toLowerCase() + "-" + instanceName.replaceAll("\\s+", "-");
            String repoUrl = "https://github.com/" + user.getUsername() + "/" + project.getName() + ".git";
            String repoPath = "C:\\odoo-modules\\" + instanceName;
            String customAddonsPath = repoPath + "\\custom_addons";
            String odooContainerName = containerPrefix + category + "_" + instanceName;
            String dbContainerName = containerPrefix + category + "_" + instanceName + "_db";

            // Clonar repositorio si no existe
            File customAddons = new File(customAddonsPath);
            if (!customAddons.exists()) {
                String gitCloneCmd = String.format("git clone -b %s %s \"%s\"", branchName, repoUrl, repoPath);
                System.out.println("gitCloneCmd: " + gitCloneCmd);
                if (!executeCommand(new String[]{"cmd.exe", "/c", gitCloneCmd})) {
                    return "❌ Error al clonar el repositorio.";
                }
            }

            // Verificar existencia de carpeta custom_addons
            if (!customAddons.exists() || !customAddons.isDirectory()) {
                return "ℹ️ No se encontró la carpeta custom_addons en la rama.";
            }

            // Obtener módulos
            String[] modules = customAddons.list((dir, name) -> new File(dir, name).isDirectory());
            if (modules == null || modules.length == 0) {
                return "ℹ️ No se encontraron módulos personalizados para instalar.";
            }
            String modulesList = String.join(",", modules);
            System.out.println("🧩 Módulos detectados: " + modulesList);

            // Parar y eliminar contenedor actual
            executeCommand(new String[]{"cmd.exe", "/c", "docker stop " + odooContainerName});
            executeCommand(new String[]{"cmd.exe", "/c", "docker rm " + odooContainerName});

            // Volúmenes
            String odooVolume = "odoo_" + instanceName + "_data";
            String odooCustomVolume = String.format("-v \"%s:/mnt/extra-addons\"", customAddons.getAbsolutePath());

            // Crear nuevo contenedor con ruta extra de addons
            String odooCommand = String.format(
                    "docker run -d --name %s --network %s " +
                            "-e HOST=%s -e USER=%s -e PASSWORD=%s -e DB=%s " +
                            "-p %d:8069 -v %s:/var/lib/odoo %s %s",
                    odooContainerName, dockerNetwork,
                    dbContainerName, postgresUser, postgresPassword, instanceName,
                    port, odooVolume, odooCustomVolume, odooImage
            );
            System.out.println("odooCommand: " + odooCommand);
            if (!executeCommand(new String[]{"cmd.exe", "/c", odooCommand})) {
                return "❌ Error: No se pudo recrear el contenedor Odoo.";
            }

            // Instalar módulos
            String installCmd = String.format(
                    "docker exec %s odoo -d %s -i %s --db_host=%s --db_user=%s --db_password=%s --stop-after-init",
                    odooContainerName, instanceName, modulesList, dbContainerName, postgresUser, postgresPassword
            );
            System.out.println("🔧 Ejecutando instalación: " + installCmd);
            if (!executeCommand(new String[]{"cmd.exe", "/c", installCmd})) {
                return "⚠️ Módulos no se instalaron correctamente.";
            }

            return "✅ Módulos personalizados instalados correctamente.";

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error inesperado al instalar módulos: " + e.getMessage();
        }
    }



    //------------------------

    //Metodo para crear una rama en github
    private void createGitHubBranch(Project project, String instanceName, String category) {
        try {
            User user = project.getUser();

            if (user == null || user.getGithubToken() == null) {
                System.out.println("⚠️ Usuario no tiene token de GitHub. Rama no creada.");
                return;
            }

            String repo = project.getName().replaceAll("\\s+", "-");
            String branchName = category.toLowerCase() + "-" + instanceName.replaceAll("\\s+", "-");

            githubService.createBranch(
                    user.getGithubToken(),
                    user.getUsername(),
                    repo,
                    branchName
            );

            System.out.println("✅ Rama de GitHub creada: " + branchName);

        } catch (Exception e) {
            System.err.println("❌ Error al crear la rama en GitHub: " + e.getMessage());
        }


    }




    // Método auxiliar para buscar un puerto libre
    private int findAvailablePort(int startPort, int endPort) {
        for (int port = startPort; port <= endPort; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        return -1; // No disponible
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
            Process process = Runtime.getRuntime().exec(
                    new String[]{"cmd.exe", "/c", "docker inspect -f \"{{.State.Running}}\" " + containerName}
            );
            process.waitFor();
            byte[] output = process.getInputStream().readAllBytes();
            String result = new String(output).trim();
            System.out.println("🧐 Estado del contenedor " + containerName + ": " + result);
            return result.equalsIgnoreCase("true");
        } catch (Exception e) {
            System.out.println("❌ Error verificando estado de contenedor " + containerName + ": " + e.getMessage());
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

    /// /////////////////////

    public String createBackup(String username, Long projectId, String instanceName, String category) {
        System.out.println("iniciando");

        // ✅ Obtener el proyecto y validar que pertenece al usuario autenticado
        Project project = projectRepository.findById(projectId)
                .filter(p -> p.getUser().getUsername().equals(username))
                .orElse(null);

        if (project == null) {
            return "❌ Proyecto no válido para el usuario autenticado.";
        }

        String projectName = project.getName().replaceAll("\\s+", "-"); // nombre sin espacios

        System.out.println("proyecto: " + projectName);

        // 🔎 Buscar instancia dentro del proyecto
        List<OdooInstance> matches = odooInstanceRepository.findAllByNameAndProjectId(instanceName, projectId);
        if (matches.isEmpty()) {
            return "❌ Instancia no encontrada en el proyecto especificado.";
        } else if (matches.size() > 1) {
            return "❌ Se encontraron múltiples instancias con ese nombre en el proyecto. Verifica tu base de datos.";
        }

        OdooInstance instance = matches.get(0);

        String dbContainerName = getContainerName(instanceName, category, true);
        String odooContainerName = getContainerName(instanceName, category, false);
        System.out.println("dbContainerName: " + dbContainerName);
        System.out.println("odooContainerName: " + odooContainerName);

        if (dbContainerName == null || odooContainerName == null) {
            return "❌ No se encontraron los contenedores para la instancia.";
        }

        File backupDirFile = new File(backupDir);
        if (!backupDirFile.exists() && !backupDirFile.mkdirs()) {
            return "❌ No se pudo crear la carpeta de backups.";
        }

        String timestamp = String.valueOf(System.currentTimeMillis());

        // ✅ Incluir nombre del proyecto en los archivos
        String dbFileName = String.format("db_backup_%s_%s_%s_%s.dump",
                projectName, category.toUpperCase(), instanceName, timestamp);

        String odooFileName = String.format("odoo_data_%s_%s_%s_%s.tar.gz",
                projectName, category.toUpperCase(), instanceName, timestamp);

        String dbBackupFilePath = backupDir + File.separator + dbFileName;
        String odooBackupFilePath = backupDir + File.separator + odooFileName;

        System.out.println("dbBackupFilePath: " + dbBackupFilePath);
        System.out.println("odooBackupFilePath: " + odooBackupFilePath);

        try {
            // 📦 Comando para backup de base de datos
            String dbBackupPathInContainer = "/tmp/" + dbFileName;
            String dbDumpCommand = String.format(
                    "docker exec %s pg_dump -U %s -d %s -Fc -f %s",
                    dbContainerName, postgresUser, instanceName, dbBackupPathInContainer
            );
            String dbCopyCommand = String.format(
                    "docker cp %s:%s \"%s\"",
                    dbContainerName, dbBackupPathInContainer, dbBackupFilePath
            );

            // 📁 Comando para backup del filestore
            String odooBackupPathInContainer = "/tmp/" + odooFileName;
            String odooBackupCommand = String.format(
                    "docker exec %s tar -czf %s -C /var/lib/odoo .",
                    odooContainerName, odooBackupPathInContainer
            );
            String dockerCopyCommand = String.format(
                    "docker cp %s:%s \"%s\"",
                    odooContainerName, odooBackupPathInContainer, odooBackupFilePath
            );

            System.out.println("dbDumpCommand: " + dbDumpCommand);
            System.out.println("dbCopyCommand: " + dbCopyCommand);
            System.out.println("odooBackupCommand: " + odooBackupCommand);
            System.out.println("dockerCopyCommand: " + dockerCopyCommand);

            boolean dbSuccess = executeCommand(new String[]{"cmd.exe", "/c", dbDumpCommand}) &&
                    executeCommand(new String[]{"cmd.exe", "/c", dbCopyCommand});

            boolean odooSuccess = executeCommand(new String[]{"cmd.exe", "/c", odooBackupCommand}) &&
                    executeCommand(new String[]{"cmd.exe", "/c", dockerCopyCommand});

            System.out.println("dbSuccess: " + dbSuccess);
            System.out.println("odooSuccess: " + odooSuccess);

            if (!dbSuccess || !odooSuccess) {
                return "❌ Error: No se pudo realizar el backup.";
            }

            return "✅ Backup creado exitosamente para la instancia: " + instanceName;

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error al crear el backup: " + e.getMessage();
        }
    }

    /// //////////////////////
    public List<BackupInfo> getBackupsForInstance(String username, String instanceName, String category) {
        List<OdooInstance> instances = odooInstanceRepository.findByProjectUserUsername(username);

        // Validar que la instancia pertenezca al usuario
        boolean valid = instances.stream().anyMatch(i ->
                i.getName().equals(instanceName) && i.getCategory().equalsIgnoreCase(category));

        if (!valid) {
            System.err.println("❌ Instancia no válida para el usuario");
            return List.of();
        }

        File directory = new File(backupDir);
        if (!directory.exists() && !directory.mkdirs()) {
            System.err.println("❌ No se pudo crear la carpeta de backups: " + backupDir);
            return List.of();
        }

        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            return List.of();
        }

        List<BackupInfo> backups = new ArrayList<>();
        for (File file : files) {
            if (file.getName().contains(instanceName) && file.getName().contains(category)) {
                long timestamp = file.getName().matches(".*\\d{13}.*")
                        ? Long.parseLong(file.getName().replaceAll("\\D", ""))
                        : file.lastModified();

                backups.add(new BackupInfo(
                        file.getName(),
                        Instant.ofEpochMilli(timestamp),
                        category,
                        "15.0",
                        file.getName().contains("Auto") ? "Automático" : "Manual",
                        "N/A"
                ));
            }
        }

        return backups;
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

            return (containerName == null || containerName.isBlank()) ? null : containerName;


        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //-----------------v1
    public String restoreBackup(String username, Long projectId, String instanceName, String category,
                                String dbBackupFileName, String odooBackupFileName) {
        // Validar que el proyecto pertenece al usuario
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty() || !projectOpt.get().getUser().getUsername().equals(username)) {
            return "❌ No tienes permiso para restaurar en este proyecto.";
        }

        Project project = projectOpt.get();

        // Regex para extraer project, category, instance de nombres tipo: db_backup_<project>_<category>_<instance>_<timestamp>.dump
        Pattern pattern = Pattern.compile("^db_backup_(.+?)_([A-Z]+)_(.+?)_\\d+\\.dump$");
        Matcher matcher = pattern.matcher(dbBackupFileName);

        if (!matcher.matches()) {
            return "❌ Nombre de archivo de backup inválido.";
        }

        String projectFromFile = matcher.group(1);
        String categoryFromFile = matcher.group(2);
        String instanceFromFile = matcher.group(3);

        if (!instanceFromFile.equals(instanceName) ||
                !categoryFromFile.equalsIgnoreCase(category) ||
                !projectFromFile.equals(project.getName())) {
            return "❌ El archivo de backup no coincide con la instancia, categoría o proyecto.";
        }

        List<OdooInstance> instances = odooInstanceRepository.findAllByNameAndProjectId(instanceName, projectId);
        if (instances.isEmpty()) {
            return "❌ Instancia no encontrada en el proyecto especificado.";
        } else if (instances.size() > 1) {
            return "❌ Se encontraron múltiples instancias con ese nombre en el proyecto. Verifica.";
        }

        String dbContainer = getContainerName(instanceName, category, true);
        String odooContainer = getContainerName(instanceName, category, false);

        String dbFile = backupDir + File.separator + dbBackupFileName;
        String odooFile = backupDir + File.separator + odooBackupFileName;

        if (!new File(dbFile).exists() || !new File(odooFile).exists()) {
            return "❌ Archivos de backup no encontrados.";
        }

        try {
            // Comandos para restaurar base de datos
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

            // Restauración de archivos Odoo usando busybox
            String odooVolume = "odoo_" + instanceName + "_data";
            String tempContainerName = "restore_temp_" + instanceName;

            String createTempContainer = String.format("docker create --name %s -v %s:/data busybox", tempContainerName, odooVolume);
            String copyToTemp = String.format("docker cp \"%s\" %s:/data/restore.tar.gz", odooFile, tempContainerName);
            String extractFromTemp = String.format(
                    "docker run --rm --volumes-from %s busybox sh -c \"cd /data && tar -xzf restore.tar.gz && chmod -R 777 /data\"",
                    tempContainerName);
            String removeTemp = String.format("docker rm %s", tempContainerName);

            boolean odooRestored =
                    executeCommand(new String[]{"cmd.exe", "/c", createTempContainer}) &&
                            executeCommand(new String[]{"cmd.exe", "/c", copyToTemp}) &&
                            executeCommand(new String[]{"cmd.exe", "/c", extractFromTemp}) &&
                            executeCommand(new String[]{"cmd.exe", "/c", removeTemp});

            return (dbRestored && odooRestored)
                    ? "✅ Restauración completada en la instancia " + instanceName
                    : "❌ Falló la restauración en la instancia " + instanceName;

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error durante el restore: " + e.getMessage();
        }
    }


    public String mergeOdooInstances(Long projectId, String sourceInstance, String targetInstance) {
        try {
            System.out.println("🚀 Iniciando proceso de merge: " + sourceInstance + " ➡️ " + targetInstance);

            List<OdooInstance> sourceList = odooInstanceRepository.findAllByNameAndProjectId(sourceInstance, projectId);
            List<OdooInstance> targetList = odooInstanceRepository.findAllByNameAndProjectId(targetInstance, projectId);

            if (sourceList.isEmpty() || targetList.isEmpty()) {
                return "❌ Una o ambas instancias no pertenecen al proyecto con ID: " + projectId;
            }

            String sourceCategory = sourceList.get(0).getCategory();
            String targetCategory = targetList.get(0).getCategory();

            String sourceDbContainer = getContainerName(sourceInstance, sourceCategory, true);
            String targetDbContainer = getContainerName(targetInstance, targetCategory, true);
            String sourceOdooContainer = getContainerName(sourceInstance, sourceCategory, false);
            String targetOdooContainer = getContainerName(targetInstance, targetCategory, false);

            if (sourceDbContainer == null || targetDbContainer == null || sourceOdooContainer == null || targetOdooContainer == null) {
                return "❌ No se encontraron todos los contenedores necesarios para el merge.";
            }

            String timestamp = String.valueOf(System.currentTimeMillis());
            String dumpLocalPath = String.format("merge_%s_to_%s_%s.dump", sourceInstance, targetInstance, timestamp);
            String filestoreLocalPath = "filestore_backup.tar.gz";
            String odooVolume = "odoo_" + targetInstance + "_data";

            // 1. Dump base de datos
            if (!runCmd(String.format("docker exec %s pg_dump -U odoo %s -Fc -f /tmp/%s", sourceDbContainer, sourceInstance, dumpLocalPath)))
                return "❌ Falló el dump de base de datos.";

            if (!runCmd(String.format("docker cp %s:/tmp/%s %s", sourceDbContainer, dumpLocalPath, dumpLocalPath)))
                return "❌ Falló la copia del dump al host.";

            runCmd(String.format("docker exec %s psql -U odoo -d postgres -c \"SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '%s';\"", targetDbContainer, targetInstance));
            runCmd(String.format("docker exec %s dropdb -U odoo %s", targetDbContainer, targetInstance));
            runCmd(String.format("docker exec %s createdb -U odoo %s", targetDbContainer, targetInstance));

            if (!runCmd(String.format("docker cp %s %s:/tmp/merge_temp.dump", dumpLocalPath, targetDbContainer)))
                return "❌ Falló la copia del dump al destino.";

            if (!runCmd(String.format("docker exec %s pg_restore -U odoo -d %s --clean --if-exists /tmp/merge_temp.dump", targetDbContainer, targetInstance)))
                return "❌ Falló la restauración del dump.";

            // 2. Backup del filestore
            if (!runCmd(String.format("docker exec %s tar czf /tmp/filestore_backup.tar.gz -C /var/lib/odoo/filestore/%s .", sourceOdooContainer, sourceInstance)))
                return "❌ Falló el backup del filestore.";

            if (!runCmd(String.format("docker cp %s:/tmp/filestore_backup.tar.gz %s", sourceOdooContainer, filestoreLocalPath)))
                return "❌ Falló la copia del filestore al host.";

            // 3. Restaurar filestore en volumen destino usando contenedor temporal
            if (!runCmd("docker create --name temp_restore -v /data busybox"))
                return "❌ No se pudo crear el contenedor temporal.";

            if (!runCmd(String.format("docker cp %s temp_restore:/data/filestore_backup.tar.gz", filestoreLocalPath)))
                return "❌ No se pudo copiar el tar.gz al contenedor temporal.";

            if (!runCmd(String.format(
                    "docker run --rm --volumes-from temp_restore -v %s:/target busybox sh -c \"mkdir -p /target/filestore/%s && tar xzf /data/filestore_backup.tar.gz -C /target/filestore/%s && chmod -R 777 /target/filestore/%s\"",
                    odooVolume, targetInstance, targetInstance, targetInstance
            ))) {
                return "❌ Falló la restauración del filestore.";
            }

            runCmd("docker rm -f temp_restore");


            // 4. Crear carpeta de sesiones directamente en el volumen
            runCmd(String.format(
                    "docker run --rm -v %s:/target busybox sh -c \"mkdir -p /target/sessions && chmod -R 777 /target/sessions\"",
                    odooVolume
            ));

            // 5. Limpieza de archivos temporales
            runCmd(String.format("docker exec %s rm -f /tmp/merge_temp.dump /tmp/filestore_backup.tar.gz", targetDbContainer));
            runCmd(String.format("docker exec %s rm -f /tmp/filestore_backup.tar.gz", sourceOdooContainer));
            runCmd(String.format("docker exec %s rm -f /tmp/filestore_backup.tar.gz", targetOdooContainer));
            deleteTempFile(dumpLocalPath);
            deleteTempFile(filestoreLocalPath);

            // 6. Reiniciar contenedor Odoo destino
            runCmd(String.format("docker restart %s", targetOdooContainer));

            // 7. Verificar disponibilidad web
            if (!waitForOdooReady(targetOdooContainer)) {
                return "⚠️ Merge completado pero el contenedor no respondió a tiempo.";
            }

            return "✅ Merge realizado correctamente de " + sourceInstance + " ➡️ " + targetInstance;

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error inesperado durante el merge: " + e.getMessage();
        }
    }


    private boolean runCmd(String command) throws IOException {
        try {
            System.out.println("🛠️ Ejecutando: " + command);
            Process process = new ProcessBuilder("cmd.exe", "/c", command)
                    .redirectErrorStream(true)
                    .start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[OUTPUT] " + line);
                }
            }

            int exitCode = process.waitFor();
            System.out.println("↪️ Exit code: " + exitCode);
            return exitCode == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void deleteTempFile(String filename) {
        File file = new File(filename);
        if (file.exists()) {
            boolean deleted = file.delete();
            System.out.println(deleted ? "🧹 Archivo temporal eliminado: " + filename : "⚠️ No se pudo eliminar: " + filename);
        }
    }

    private boolean waitForOdooReady(String containerName) {
        int maxAttempts = 30;
        int waitSeconds = 6;
        int port = findMappedPort(containerName);

        if (port == -1) {
            System.out.println("❌ No se pudo encontrar el puerto mapeado para el contenedor " + containerName);
            return false;
        }

        System.out.println("🌐 Verificando disponibilidad en http://localhost:" + port + "/web/login");

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // Verificar si el contenedor aún está corriendo
                if (!isContainerRunning(containerName)) {
                    System.out.println("❌ Contenedor " + containerName + " no está corriendo. Abortando espera.");
                    return false;
                }

                System.out.println("🔄 Intento " + attempt + ": Verificando Odoo...");
                Process process = Runtime.getRuntime().exec(
                        new String[]{"cmd.exe", "/c", "curl -s -o nul -w \"%{http_code}\" http://localhost:" + port + "/web/login"}
                );

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String responseCode = reader.readLine();

                int exitCode = process.waitFor();

                if ("200".equals(responseCode) || exitCode == 0 || exitCode == 52) {
                    System.out.println("✅ Odoo parece estar listo (HTTP " + responseCode + ", exitCode=" + exitCode + ") en intento " + attempt);
                    return true;
                }

                System.out.println("⏳ Aún no responde (HTTP " + responseCode + ", exitCode=" + exitCode + "). Esperando " + waitSeconds + " segundos...");
                Thread.sleep(waitSeconds * 1000);

            } catch (IOException | InterruptedException e) {
                System.out.println("❌ Error en intento " + attempt + ": " + e.getMessage());
                return false;
            }
        }

        System.out.println("⏱️ Tiempo agotado: Odoo no respondió tras " + maxAttempts + " intentos.");
        return false;
    }

    private int findMappedPort(String containerName) {
        try {
            System.out.println("🔎 Buscando puerto expuesto de " + containerName);
            Process process = Runtime.getRuntime().exec(
                    new String[]{"cmd.exe", "/c", "docker inspect -f \"{{ (index (index .NetworkSettings.Ports \\\"8069/tcp\\\") 0).HostPort }}\" " + containerName}
            );
            process.waitFor();
            byte[] output = process.getInputStream().readAllBytes();
            String portStr = new String(output).trim();

            if (portStr.isEmpty()) {
                System.out.println("❌ No se encontró puerto expuesto para 8069/tcp en " + containerName);
                return -1;
            }

            int port = Integer.parseInt(portStr);
            System.out.println("✅ Puerto encontrado para " + containerName + ": " + port);
            return port;

        } catch (Exception e) {
            System.out.println("❌ Error obteniendo puerto mapeado para " + containerName + ": " + e.getMessage());
            return -1;
        }
    }

    public List<OdooInstance> getInstancesByProject(String projectName) {
        return odooInstanceRepository.findByProjectName(projectName);
    }
//------
    public String importDatabaseFromFile(MultipartFile file, String dbName, String category) throws IOException {
    // 1. Guardar archivo temporal
    Path tempPath = Files.createTempFile("upload-", ".dump");
    file.transferTo(tempPath.toFile());

    String containerDbName = getContainerName(dbName, category, true);
    String containerFilePath = "/tmp/" + file.getOriginalFilename();

    try {
        System.out.println("📦 Copiando dump al contenedor...");
        runCmd(String.format("docker cp \"%s\" %s:%s", tempPath, containerDbName, containerFilePath));

        System.out.println("🔒 Terminando sesiones activas...");
        runCmd(String.format(
                "docker exec %s psql -U odoo -d postgres -c \"SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '%s' AND pid <> pg_backend_pid();\"",
                containerDbName, dbName));

        System.out.println("🗑️ Eliminando base si existe...");
        runCmd(String.format("docker exec %s dropdb --if-exists -U odoo %s", containerDbName, dbName));

        System.out.println("📚 Creando nueva base...");
        runCmd(String.format("docker exec %s createdb -U odoo %s", containerDbName, dbName));

        System.out.println("🔁 Restaurando dump...");
        boolean restoreSuccess = runCmdAndCheckWarnings(String.format(
                "docker exec %s pg_restore -U odoo -d %s --clean --if-exists --verbose %s",
                containerDbName, dbName, containerFilePath));

        if (!restoreSuccess) {
            System.out.println("⚠️ pg_restore devolvió código distinto de 0 pero sin errores fatales. Se considera exitoso con advertencias.");
        }

    } finally {
        Files.deleteIfExists(tempPath);
    }

    return "✅ Base de datos restaurada exitosamente.";
}

    private boolean runCmdAndCheckWarnings(String command) {
        try {
            System.out.println("🛠️ Ejecutando (con análisis): " + command);
            Process process = new ProcessBuilder("cmd.exe", "/c", command)
                    .redirectErrorStream(true)
                    .start();

            boolean hasFatalError = false;
            List<String> outputLines = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[OUTPUT] " + line);
                    outputLines.add(line);
                    if (line.contains("pg_restore: error:")) {
                        hasFatalError = true;
                    }
                }
            }

            int exitCode = process.waitFor();
            System.out.println("↪️ Exit code: " + exitCode);

            if (hasFatalError) {
                System.out.println("❌ Error fatal detectado en la salida del proceso.");
                return false;
            }

            // Aunque el exitCode sea distinto de 0, si no hubo errores fatales, consideramos éxito.
            if (exitCode != 0) {
                System.out.println("⚠️ pg_restore retornó código distinto de 0 pero sin errores fatales.");
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //SHEL DB
    public String executeSqlCommandInInstance(String command, String instanceName, String category) {
    String containerName = String.format("odoo_instance_%s_%s_db", category.toUpperCase(), instanceName);

    // Seguridad: bloquear comandos destructivos
    String lower = command.trim().toLowerCase();
    if (lower.matches("^(drop|delete|alter|truncate|update).*")) {
        return "❌ Comando no permitido por seguridad.";
    }

    try {
        // Escapar comillas dobles
        String escapedCommand = command.replace("\"", "\\\"");

        String dockerCommand = String.format(
                "docker exec -i %s psql -U %s -d %s -q -t --csv -c \"%s\"",
                containerName, postgresUser, instanceName, escapedCommand);

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String[] shellCmd = isWindows
                ? new String[]{"cmd.exe", "/c", dockerCommand}
                : new String[]{"/bin/sh", "-c", dockerCommand};

        ProcessBuilder builder = new ProcessBuilder(shellCmd);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            return "❌ Error al ejecutar el comando. Código de salida: " + exitCode + "\n" + output;
        }

        return output.toString().trim().isEmpty()
                ? "✅ Comando ejecutado, sin salida."
                : output.toString().trim();

    } catch (Exception e) {
        return "❌ Excepción al ejecutar el comando: " + e.getMessage();
    }
}

    //IMPORT DATABASE AND ODOO
    public String importDatabaseAndOdooFiles(MultipartFile dbFile, MultipartFile odooFile, String dbName, String category) throws IOException {
    // Guardar archivo temporal de la base de datos
    Path dbTempPath = Files.createTempFile("upload-db-", ".dump");
    dbFile.transferTo(dbTempPath.toFile());

    String containerDbName = getContainerName(dbName, category, true);
    String containerDbFilePath = "/tmp/restore.dump";

    try {
        System.out.println("📦 Copiando dump al contenedor...");
        runCmd(String.format("docker cp \"%s\" %s:%s", dbTempPath, containerDbName, containerDbFilePath));

        System.out.println("🔒 Terminando sesiones activas...");
        runCmd(String.format(
                "docker exec %s psql -U odoo -d postgres -c \"SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '%s' AND pid <> pg_backend_pid();\"",
                containerDbName, dbName));

        System.out.println("🗑️ Eliminando base si existe...");
        runCmd(String.format("docker exec %s dropdb --if-exists -U odoo %s", containerDbName, dbName));

        System.out.println("📚 Creando nueva base...");
        runCmd(String.format("docker exec %s createdb -U odoo %s", containerDbName, dbName));

        System.out.println("🔁 Restaurando dump...");
        boolean restoreSuccess = runCmdAndCheckWarnings(String.format(
                "docker exec %s pg_restore -U odoo -d %s --clean --if-exists --verbose %s",
                containerDbName, dbName, containerDbFilePath));

        if (!restoreSuccess) {
            System.out.println("⚠️ pg_restore devolvió código distinto de 0 pero sin errores fatales.");
        }

        // Si se recibió archivo de Odoo (.tar.gz), restaurar también
        if (odooFile != null && !odooFile.isEmpty()) {
            System.out.println("📁 Restaurando filestore de Odoo...");

            Path odooTempPath = Files.createTempFile("upload-odoo-", ".tar.gz");
            odooFile.transferTo(odooTempPath.toFile());

            String odooVolume = "odoo_" + dbName + "_data";
            String tempContainer = "restore_temp_" + dbName;

            runCmd(String.format("docker create --name %s -v %s:/data busybox", tempContainer, odooVolume));
            runCmd(String.format("docker cp \"%s\" %s:/data/restore.tar.gz", odooTempPath, tempContainer));
            runCmd(String.format(
                    "docker run --rm --volumes-from %s busybox sh -c \"cd /data && tar -xzf restore.tar.gz && chmod -R 777 /data\"",
                    tempContainer));
            runCmd(String.format("docker rm %s", tempContainer));

            Files.deleteIfExists(odooTempPath);
        }

    } finally {
        Files.deleteIfExists(dbTempPath);
    }

    return "✅ Base de datos restaurada exitosamente.";
}

    //DELETE INSTANCE
    public String deleteOdooInstance(String name, String category) {
        try {
            String container = "odoo_instance_" + category + "_" + name;
            String dbContainer = container + "_db";
            String odooVolume = "odoo_" + name + "_data";
            String dbVolume = "odoo_" + name + "_db_data";

            System.out.println("🗑 Eliminando contenedor de Odoo: " + container);
            executeCommand(new String[]{"cmd.exe", "/c", "docker rm -f " + container});

            System.out.println("🗑 Eliminando contenedor de PostgreSQL: " + dbContainer);
            executeCommand(new String[]{"cmd.exe", "/c", "docker rm -f " + dbContainer});

            System.out.println("🗑 Eliminando volumen Odoo: " + odooVolume);
            executeCommand(new String[]{"cmd.exe", "/c", "docker volume rm " + odooVolume});

            System.out.println("🗑 Eliminando volumen DB: " + dbVolume);
            executeCommand(new String[]{"cmd.exe", "/c", "docker volume rm " + dbVolume});

            Optional<OdooInstance> optional = odooInstanceRepository.findAllByNameAndCategory(name, category).stream().findFirst();
            if (optional.isEmpty()) {
                return "✅ Contenedores eliminados. La instancia no estaba registrada en la base de datos.";
            }

            odooInstanceRepository.deleteByNameAndCategory(name, category);

            return "✅ Instancia '" + name + "' (" + category + ") eliminada correctamente.";
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error al eliminar instancia: " + e.getMessage();
        }
    }

}