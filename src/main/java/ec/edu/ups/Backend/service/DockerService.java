package ec.edu.ups.Backend.service;

import ec.edu.ups.Backend.model.BackupInfo;
import ec.edu.ups.Backend.model.OdooInstance;
import ec.edu.ups.Backend.model.Project;
import ec.edu.ups.Backend.model.User;
import ec.edu.ups.Backend.repository.OdooInstanceRepository;
import ec.edu.ups.Backend.repository.ProjectRepository;
import org.apache.tomcat.util.http.fileupload.FileUtils;
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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;
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

    public String createOdooInstance(String instanceName, String category, Long projectId,
                                     boolean neutralize, String codeSourceCategory, boolean copyDataFromProduction) {
        try {
            System.out.println("🚀 Iniciando método de creación de instancia...");

            Optional<Project> optionalProject = projectRepository.findById(projectId);
            if (optionalProject.isEmpty()) return "❌ Error: Proyecto no encontrado con ID: " + projectId;
            Project project = optionalProject.get();

            System.out.println("Punto 1");

            boolean needsMergeFromProduction = false;
            String codeSourceInstanceName = null;
            String sourceCategory = null;

            // 🔍 Buscar instancia PRODUCTION (por si se requiere merge o fallback de código)
            List<OdooInstance> prodInstances = odooInstanceRepository.findByProjectIdAndCategory(projectId, "PRODUCTION");
            String productionInstanceName = !prodInstances.isEmpty() ? prodInstances.get(0).getName() : "";
            System.out.println("prodInstances "+ prodInstances);
            // 🔁 Lógica según categoría
            if (category.equalsIgnoreCase("STAGING")) {
                System.out.println("1. category STAGING "+ category);

                if ("PRODUCTION".equalsIgnoreCase(codeSourceCategory) && !productionInstanceName.isEmpty()) {
                    System.out.println("2. codeSourceCategory "+ codeSourceCategory);
                    System.out.println("3. productionInstanceName "+ productionInstanceName);
                    codeSourceInstanceName = productionInstanceName;
                    System.out.println("3. productionInstanceName "+ productionInstanceName);
                    sourceCategory = "PRODUCTION";
                    System.out.println(" sourceCategory  "+ sourceCategory);
                    needsMergeFromProduction = copyDataFromProduction;
                    System.out.println("needsMergeFromProduction  "+needsMergeFromProduction );
                    System.out.println("📦 Código y datos para STAGING copiados desde PRODUCTION: " + codeSourceInstanceName);
                } else {
                    List<OdooInstance> devInstances = odooInstanceRepository.findByProjectIdAndCategory(projectId, "DEVELOPMENT");
                    if (!devInstances.isEmpty()) {
                        codeSourceInstanceName = devInstances.get(0).getName();
                        System.out.println(" codeSourceInstanceName  "+ codeSourceInstanceName);
                        sourceCategory = "DEVELOPMENT";
                        System.out.println("sourceCategory  "+ sourceCategory);
                        System.out.println("📦 Código para STAGING copiado desde DEVELOPMENT: " + codeSourceInstanceName);
                    }
                }
            } else if (category.equalsIgnoreCase("PRODUCTION")) {
                System.out.println("1.1 category PRODUCTION "+category );
                if ("STAGING".equalsIgnoreCase(codeSourceCategory)) {
                    System.out.println("codeSourceCategory  "+ codeSourceCategory);
                    List<OdooInstance> stagingInstances = odooInstanceRepository.findByProjectIdAndCategory(projectId, "STAGING");
                    if (!stagingInstances.isEmpty()) {
                        codeSourceInstanceName = stagingInstances.get(0).getName();
                        sourceCategory = "STAGING";
                        System.out.println("📦 Código para PRODUCTION copiado desde STAGING: " + codeSourceInstanceName);
                    }
                }
            } else if (category.equalsIgnoreCase("DEVELOPMENT")) {
                System.out.println("1.1 category DEVELOPMENT "+category );
                if ("STAGING".equalsIgnoreCase(codeSourceCategory)) {
                    List<OdooInstance> stagingInstances = odooInstanceRepository.findByProjectIdAndCategory(projectId, "STAGING");
                    System.out.println("stagingInstances  "+ stagingInstances);
                    if (!stagingInstances.isEmpty()) {
                        codeSourceInstanceName = stagingInstances.get(0).getName();
                        System.out.println("codeSourceInstanceName  "+ codeSourceInstanceName);
                        sourceCategory = "STAGING";
                        System.out.println("📦 Código para DEVELOPMENT copiado desde STAGING: " + codeSourceInstanceName);
                    }
                } else if ("PRODUCTION".equalsIgnoreCase(codeSourceCategory) && !productionInstanceName.isEmpty()) {
                    codeSourceInstanceName = productionInstanceName;
                    System.out.println("codeSourceInstanceName  "+ codeSourceInstanceName);
                    sourceCategory = "PRODUCTION";
                    System.out.println("📦 Código para DEVELOPMENT copiado desde PRODUCTION: " + codeSourceInstanceName);
                }
            }

            // 📦 Construcción de nombres
            String dbContainerName = containerPrefix + category.toUpperCase() + "_" + instanceName + "_db";
            System.out.println(" dbContainerName  "+ dbContainerName);
            String odooContainerName = containerPrefix + category.toUpperCase() + "_" + instanceName;
            System.out.println(" odooContainerName "+odooContainerName );
            String dbVolume = "odoo_" + instanceName + "_db_data";
            System.out.println(" dbVolume "+ dbVolume);
            String odooVolume = "odoo_" + instanceName + "_data";
            System.out.println(" odooVolume "+ odooVolume);
            String repoPath = "C:\\odoo-modules\\" + instanceName;
            System.out.println("repoPath  "+repoPath );
            String customAddonsPath = repoPath + "\\custom_addons";
            System.out.println("customAddonsPath  "+customAddonsPath );

            int port = findAvailablePort(8069, 8100);
            if (port == -1) return "❌ Error: No hay puertos disponibles.";

            // 🧼 Eliminar contenedores existentes si ya existen
            for (String containerName : List.of(dbContainerName, odooContainerName)) {
                String checkCmd = "docker ps -a -q -f name=" + containerName;
                Process checkProcess = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", checkCmd});
                BufferedReader reader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()));
                String containerId = reader.readLine();
                reader.close();
                checkProcess.waitFor();

                if (containerId != null && !containerId.trim().isEmpty()) {
                    System.out.println("⚠️ Contenedor existente detectado: " + containerName + ". Eliminando...");
                    String removeCmd = "docker rm -f " + containerName;
                    if (!executeCommand(new String[]{"cmd.exe", "/c", removeCmd})) {
                        return "❌ Error al eliminar contenedor existente: " + containerName;
                    }
                }
            }

            // 📦 PostgreSQL
            String dbCommand = String.format(
                    "docker run -d --name %s --network %s -e POSTGRES_USER=%s -e POSTGRES_PASSWORD=%s -e POSTGRES_DB=%s -v %s:/var/lib/postgresql/data %s",
                    dbContainerName, dockerNetwork, postgresUser, postgresPassword, postgresDb, dbVolume, postgresImage
            );
            System.out.println("dbCommand  "+dbCommand );
            if (!executeCommand(new String[]{"cmd.exe", "/c", dbCommand})) return "❌ Error al crear contenedor PostgreSQL.";
            Thread.sleep(4000);

            String createDbCmd = String.format("docker exec %s createdb -U %s %s", dbContainerName, postgresUser, instanceName);
            if (!executeCommand(new String[]{"cmd.exe", "/c", createDbCmd})) return "❌ Error al crear la base de datos.";
            System.out.println(" createDbCmd "+createDbCmd );
            // 📂 Preparar volumen Odoo
            String prepareVolumeCmd = String.format(
                    "docker run --rm -v %s:/data busybox sh -c \"mkdir -p /data/filestore/%s /data/sessions && chmod -R 777 /data\"",
                    odooVolume, instanceName
            );
            System.out.println("prepareVolumeCmd  "+prepareVolumeCmd );
            if (!executeCommand(new String[]{"cmd.exe", "/c", prepareVolumeCmd})) return "❌ Error al preparar volumen de Odoo.";

            // 🔁 Copiar módulos personalizados desde instancia fuente
            if (codeSourceInstanceName != null) {
                System.out.println("2.... codeSourceInstanceName  "+ codeSourceInstanceName);
                String sourceContainer = containerPrefix + sourceCategory + "_" + codeSourceInstanceName;
                System.out.println("sourceContainer  "+sourceContainer );
                File addonsTarget = new File(customAddonsPath);
                System.out.println("addonsTarget  "+addonsTarget );
                if (!addonsTarget.exists()) addonsTarget.mkdirs();

                System.out.println("📁 Copiando módulos desde " + sourceContainer + " a " + customAddonsPath);

                String checkExtraAddons = String.format("docker exec %s test -d /mnt/extra-addons", sourceContainer);
                System.out.println(" checkExtraAddons "+checkExtraAddons );
                if (executeCommand(new String[]{"cmd.exe", "/c", checkExtraAddons})) {
                    String tempPath = "/tmp/addons_to_copy";
                    String copyToTmp = String.format(
                            "docker exec %s bash -c \"rm -rf %s && mkdir -p %s && cp -r /mnt/extra-addons/* %s/\"",
                            sourceContainer, tempPath, tempPath, tempPath);
                    System.out.println("copyToTmp  "+ copyToTmp);
                    if (executeCommand(new String[]{"cmd.exe", "/c", copyToTmp})) {
                        String dockerCpCommand = String.format("docker cp %s:%s/. %s", sourceContainer, tempPath, customAddonsPath);
                        executeCommand(new String[]{"cmd.exe", "/c", dockerCpCommand});
                    }
                }
            }

            // 🚀 Crear contenedor Odoo
            String neutralizeEnv = neutralize ? "-e NEUTRALIZE=true " : "";
            System.out.println("neutralizeEnv  "+neutralizeEnv );
            String odooCommand = String.format(
                    "docker run -d --name %s --network %s -e HOST=%s -e USER=%s -e PASSWORD=%s -e DB=%s %s -p %d:8069 -v %s:/var/lib/odoo -v %s:/var/lib/odoo/custom_addons -v %s:/mnt/extra-addons %s",
                    odooContainerName, dockerNetwork, dbContainerName, postgresUser, postgresPassword, instanceName,
                    neutralizeEnv, port, odooVolume, customAddonsPath, customAddonsPath, odooImage
            );
            System.out.println("odooCommand  "+odooCommand );
            if (!executeCommand(new String[]{"cmd.exe", "/c", odooCommand})) return "❌ Error al crear el contenedor de Odoo.";

            // ⏳ Esperar conexión a base
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

            // 🧱 Inicializar base con módulos base (solo si no se hará merge)
            if (!needsMergeFromProduction) {
                String demoParam = category.equalsIgnoreCase("DEVELOPMENT") ? "all" : "False";
                System.out.println(" demoParam "+ demoParam);
                String initDbCmd = String.format(
                        "docker exec %s odoo -d %s -i base --db_host=%s --db_user=%s --db_password=%s --without-demo=%s --stop-after-init",
                        odooContainerName, instanceName, dbContainerName, postgresUser, postgresPassword, demoParam
                );
                System.out.println("initDbCmd  "+ initDbCmd);
                if (!executeCommand(new String[]{"cmd.exe", "/c", initDbCmd})) return "❌ Error al inicializar la base de datos.";
            }

            // 🧩 Instalar módulos personalizados
            File customFolder = new File(customAddonsPath);
            System.out.println("customFolder  "+customFolder );
            if (customFolder.exists() && customFolder.listFiles() != null && customFolder.listFiles().length > 0) {
                String listModulesCmd = String.format("docker exec %s bash -c \"ls /var/lib/odoo/custom_addons\"", odooContainerName);
                Process process = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", listModulesCmd});
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder moduleList = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        moduleList.append(line.trim()).append(",");
                    }
                }
                reader.close();
                process.waitFor();

                if (!moduleList.toString().isEmpty()) {
                    String modulesToInstall = moduleList.toString().replaceAll(",$", "");
                    String installModulesCmd = String.format(
                            "docker exec %s odoo -d %s -i %s --db_host=%s --db_user=%s --db_password=%s --stop-after-init",
                            odooContainerName, instanceName, modulesToInstall, dbContainerName, postgresUser, postgresPassword
                    );
                    executeCommand(new String[]{"cmd.exe", "/c", installModulesCmd});
                }
            }

            // 📝 Guardar instancia y crear rama GitHub
            String url = "http://localhost:" + port + "/web/login?db=" + instanceName;
            System.out.println(" url "+url );
            OdooInstance instance = new OdooInstance(instanceName, category, url, neutralize, port);
            System.out.println("instance  "+instance );
            instance.setProject(project);
            odooInstanceRepository.save(instance);
            createGitHubBranch(project, instanceName, category);

            // 🔁 Merge desde PRODUCTION si aplica
            if (needsMergeFromProduction) {
                String mergeResult = mergeOdooInstances(projectId, productionInstanceName, instanceName);
                System.out.println(" mergeResult  "+ mergeResult);
                System.out.println("🧾 Resultado del merge: " + mergeResult);
            }

            System.out.println("✅ Instancia creada exitosamente en " + url);
            return url;

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error inesperado: " + e.getMessage();
        }
    }



///

///

    private int executeCommandAndGetExitCode(String command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        process.waitFor();
        return process.exitValue();
    }

    //----------------------
    //Metodo para leer los modulos personalizados en Odoo
    public String installCustomModules(String instanceName, String category, Long projectId) {
        try {
            System.out.println("🔧 Iniciando instalación de módulos personalizados...");

            // Buscar proyecto y usuario
            Optional<Project> optionalProject = projectRepository.findById(projectId);
            if (optionalProject.isEmpty()) return "❌ Error: Proyecto no encontrado con ID: " + projectId;
            Project project = optionalProject.get();
            User user = project.getUser();

            // Buscar instancia
            Optional<OdooInstance> optionalInstance = odooInstanceRepository.findByNameAndCategoryAndProjectId(instanceName, category, projectId);
            if (optionalInstance.isEmpty()) return "❌ Error: Instancia no encontrada.";
            OdooInstance instance = optionalInstance.get();
            int port = instance.getPort();

            // Definir rutas y nombres
            String branchName = category.toLowerCase() + "-" + instanceName.toLowerCase(); // ✅ Rama completa
            String repoUrl = "https://github.com/" + user.getUsername() + "/" + project.getName() + ".git";
            String repoPath = "C:\\odoo-modules\\" + instanceName;
            String customAddonsPath = repoPath + "\\custom_addons";
            String odooContainerName = containerPrefix + category + "_" + instanceName;
            String dbContainerName = containerPrefix + category + "_" + instanceName + "_db";

            File repoDir = new File(repoPath);

            // Clonar repositorio si no existe
            if (!repoDir.exists()) {
                String gitCloneCmd = String.format("git clone -b %s %s \"%s\"", branchName, repoUrl, repoPath);
                System.out.println("gitCloneCmd: " + gitCloneCmd);
                if (!executeCommand(new String[]{"cmd.exe", "/c", gitCloneCmd})) {
                    return "❌ Error al clonar el repositorio desde GitHub.";
                }
            }

            // 📂 Mostrar estructura del repo clonado
            System.out.println("📁 Explorando contenido del repo clonado:");
            printDirectoryStructure(repoDir, 0);

            // Buscar y validar carpeta custom_addons (incluso si ya existe pero está vacía)
            File customAddons = new File(customAddonsPath);
            boolean addonsExist = customAddons.exists() && customAddons.isDirectory()
                    && customAddons.listFiles(File::isDirectory) != null
                    && customAddons.listFiles(File::isDirectory).length > 0;

            if (!addonsExist) {
                File found = findCustomAddonsRecursively(repoDir);
                if (found != null && !found.getAbsolutePath().equalsIgnoreCase(customAddonsPath)) {
                    Files.createDirectories(Paths.get(customAddonsPath).getParent());
                    Files.move(found.toPath(), Paths.get(customAddonsPath), StandardCopyOption.REPLACE_EXISTING);
                    customAddons = new File(customAddonsPath);
                } else {
                    return "ℹ️ No se encontró la carpeta custom_addons con módulos dentro del repositorio.";
                }
            }

            // Obtener módulos
            String[] modules = customAddons.list((dir, name) -> new File(dir, name).isDirectory());
            if (modules == null || modules.length == 0) {
                return "ℹ️ No se encontraron módulos personalizados para instalar.";
            }
            String modulesList = String.join(",", modules);
            System.out.println("🧩 Módulos detectados: " + modulesList);

            // Detener y eliminar contenedor actual
            executeCommand(new String[]{"cmd.exe", "/c", "docker stop " + odooContainerName});
            executeCommand(new String[]{"cmd.exe", "/c", "docker rm " + odooContainerName});

            // Crear nuevo contenedor con ruta extra de addons
            String odooVolume = "odoo_" + instanceName + "_data";
            String odooCommand = String.format(
                    "docker run -d --name %s --network %s " +
                            "-e HOST=%s -e USER=%s -e PASSWORD=%s -e DB=%s " +
                            "-p %d:8069 " +
                            "-v %s:/var/lib/odoo " +
                            "-v %s:/mnt/extra-addons " +
                            "%s",
                    odooContainerName, dockerNetwork,
                    dbContainerName, postgresUser, postgresPassword, instanceName,
                    port, odooVolume,
                    customAddons.getAbsolutePath(),
                    odooImage
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
                return "⚠️ Los módulos no se instalaron correctamente.";
            }

            return "✅ Módulos personalizados instalados correctamente.";

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error inesperado al instalar módulos: " + e.getMessage();
        }
    }


    private File findCustomAddonsRecursively(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return null;

        for (File file : files) {
            if (file.isDirectory()) {
                if (file.getName().equalsIgnoreCase("custom_addons")) {
                    return file;
                } else {
                    File result = findCustomAddonsRecursively(file);
                    if (result != null) return result;
                }
            }
        }
        return null;
    }

    private void printDirectoryStructure(File dir, int depth) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            System.out.println("  ".repeat(depth) + "- " + file.getName());
            if (file.isDirectory()) {
                printDirectoryStructure(file, depth + 1);
            }
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
            System.out.println("branchname"+branchName);

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

    ///  //////////////////


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
        if (instanceName == null || category == null) return null;

        String normalizedCategory = category.toUpperCase(); // ✅ igual que en createOdooInstance
        String base = containerPrefix + normalizedCategory + "_" + instanceName;
        String containerName = isDatabase ? base + "_db" : base;

        System.out.println("✅ Nombre esperado del contenedor (" + (isDatabase ? "DB" : "Odoo") + "): " + containerName);
        return containerName;
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
            System.out.println("🔁 Iniciando merge desde " + sourceInstance + " hacia " + targetInstance);

            // 0) Validaciones y nombres de contenedores
            List<OdooInstance> srcList = odooInstanceRepository.findAllByNameAndProjectId(sourceInstance, projectId);
            List<OdooInstance> dstList = odooInstanceRepository.findAllByNameAndProjectId(targetInstance, projectId);
            if (srcList.isEmpty() || dstList.isEmpty()) {
                return "❌ Una o ambas instancias no existen en el proyecto ID: " + projectId;
            }

            String srcCat = srcList.get(0).getCategory().toUpperCase(); // PRODUCTION, etc.
            String dstCat = dstList.get(0).getCategory().toUpperCase(); // STAGING, etc.

            String srcDbC   = "odoo_instance_" + srcCat + "_" + sourceInstance + "_db";
            String srcOdooC = "odoo_instance_" + srcCat + "_" + sourceInstance;
            String dstDbC   = "odoo_instance_" + dstCat + "_" + targetInstance + "_db";
            String dstOdooC = "odoo_instance_" + dstCat + "_" + targetInstance;
            String dstOdooVolume = "odoo_" + targetInstance + "_data";

            System.out.println("sourceDbContainer   = " + srcDbC);
            System.out.println("sourceOdooContainer = " + srcOdooC);
            System.out.println("targetDbContainer   = " + dstDbC);
            System.out.println("targetOdooContainer = " + dstOdooC);

            // 1) Stop Odoo destino
            System.out.println("🛑 Deteniendo contenedor Odoo destino (si está corriendo)...");
            executeCommand("cmd.exe /c docker stop " + dstOdooC); // ignorar fallo si ya está detenido

            // 2) Dump DB origen → host → destino
            System.out.println("📦 Generando dump de base...");
            String dumpInCont = "/tmp/merge.dump";
            if (!executeCommand(String.format("cmd.exe /c docker exec %s pg_dump -U odoo -Fc -f %s %s", srcDbC, dumpInCont, sourceInstance))) {
                return "❌ Falló pg_dump en " + srcDbC;
            }

            String dumpHost = "dump_" + System.currentTimeMillis() + ".dump";
            if (!executeCommand(String.format("cmd.exe /c docker cp %s:%s %s", srcDbC, dumpInCont, dumpHost))) {
                return "❌ Falló docker cp del dump desde " + srcDbC;
            }

            System.out.println("♻️ Restaurando DB en destino (terminate backends, drop/create, pg_restore)...");
            executeCommand(String.format(
                    "cmd.exe /c docker exec %s psql -U odoo -d postgres -c \"SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '%s';\"",
                    dstDbC, targetInstance));
            executeCommand(String.format("cmd.exe /c docker exec %s dropdb -U odoo %s", dstDbC, targetInstance));
            if (!executeCommand(String.format("cmd.exe /c docker exec %s createdb -U odoo %s", dstDbC, targetInstance))) {
                return "❌ Falló createdb en " + dstDbC;
            }
            if (!executeCommand(String.format("cmd.exe /c docker cp %s %s:/tmp/merge.dump", dumpHost, dstDbC))) {
                return "❌ Falló docker cp del dump hacia " + dstDbC;
            }
            if (!executeCommand(String.format("cmd.exe /c docker exec %s pg_restore -U odoo -d %s --clean --if-exists /tmp/merge.dump", dstDbC, targetInstance))) {
                return "❌ Falló pg_restore en " + dstDbC;
            }

            // 3) Empaquetar filestore en ORIGEN (auto-detect)
            System.out.println("🗂️ Empaquetando filestore en origen (auto-detect)...");
            String fsTarIn = "/tmp/filestore.tar.gz";
            String detectSrcFs =
                    "set -e; " +
                            "if [ -d /var/lib/odoo/filestore/" + sourceInstance + " ]; then " +
                            "  BASE=/var/lib/odoo/filestore; " +
                            "elif [ -d /var/lib/odoo/.local/share/Odoo/filestore/" + sourceInstance + " ]; then " +
                            "  BASE=/var/lib/odoo/.local/share/Odoo/filestore; " +
                            "else " +
                            "  echo 'no filestore'; exit 1; " +
                            "fi; " +
                            "cd \"$BASE\"; " +
                            "tar czf " + fsTarIn + " " + sourceInstance + "; " +
                            "echo \"FS_BASE=$BASE\"";
            if (!executeCommand(String.format("cmd.exe /c docker exec %s bash -lc \"%s\"", srcOdooC, detectSrcFs))) {
                return "❌ No se encontró/empacó filestore en " + srcOdooC;
            }

            String fsTarHost = "filestore_" + System.currentTimeMillis() + ".tar.gz";
            if (!executeCommand(String.format("cmd.exe /c docker cp %s:%s %s", srcOdooC, fsTarIn, fsTarHost))) {
                return "❌ Falló copiar filestore al host";
            }

            // 4) Restaurar filestore en DESTINO (con auto-rename a DB destino)
            System.out.println("📦 Restaurando filestore en destino (auto-rename)...");
            if (!executeCommand(String.format("cmd.exe /c docker cp %s %s:/tmp/filestore.tar.gz", fsTarHost, dstOdooC))) {
                return "❌ Falló copiar filestore al destino";
            }

            String restoreDstFs =
                    "set -e; " +
                            "if [ -d /var/lib/odoo/.local/share/Odoo/filestore ]; then " +
                            "  BASE=/var/lib/odoo/.local/share/Odoo/filestore; " +
                            "else " +
                            "  BASE=/var/lib/odoo/filestore; " +
                            "fi; " +
                            "mkdir -p \"$BASE\" /tmp/filestore_import; " +
                            "rm -rf /tmp/filestore_import/*; " +
                            "tar xzf /tmp/filestore.tar.gz -C /tmp/filestore_import; " +
                            "if [ -d /tmp/filestore_import/" + sourceInstance + " ]; then " +
                            "  rm -rf \"$BASE\"/" + targetInstance + "; " +
                            "  mv /tmp/filestore_import/" + sourceInstance + " \"$BASE\"/" + targetInstance + "; " +
                            "else " +
                            "  rm -rf \"$BASE\"/" + targetInstance + "; " +
                            "  mkdir -p \"$BASE\"/" + targetInstance + "; " +
                            "  shopt -s dotglob; mv /tmp/filestore_import/* \"$BASE\"/" + targetInstance + "/; " +
                            "fi; " +
                            "chmod -R 777 \"$BASE\"/" + targetInstance + "; " +
                            "echo \"FS_BASE_DST=$BASE\"";
            if (!executeCommand(String.format("cmd.exe /c docker exec %s bash -lc \"%s\"", dstOdooC, restoreDstFs))) {
                return "❌ Falló restauración/rename del filestore en " + dstOdooC + " (base autodetectada)";
            }

            // 5) Copiar custom_addons si existen
            System.out.println("🧩 Copiando custom_addons si existen...");
            int addonsExist = executeCommandAndGetExitCode(
                    String.format("cmd.exe /c docker exec %s bash -lc \"test -d /var/lib/odoo/custom_addons\"", srcOdooC));
            if (addonsExist == 0) {
                String addTarHost = "custom_addons_" + System.currentTimeMillis() + ".tar.gz";
                if (!executeCommand(String.format("cmd.exe /c docker exec %s bash -lc \"cd /var/lib/odoo && tar czf /tmp/custom_addons.tar.gz custom_addons\"", srcOdooC))) {
                    return "❌ Falló crear tar de custom_addons en origen";
                }
                if (!executeCommand(String.format("cmd.exe /c docker cp %s:/tmp/custom_addons.tar.gz %s", srcOdooC, addTarHost))) {
                    return "❌ Falló traer custom_addons al host";
                }
                if (!executeCommand(String.format("cmd.exe /c docker cp %s %s:/tmp/custom_addons.tar.gz", addTarHost, dstOdooC))) {
                    return "❌ Falló copiar custom_addons al destino";
                }
                if (!executeCommand(String.format("cmd.exe /c docker exec %s bash -lc \"mkdir -p /var/lib/odoo/custom_addons && tar xzf /tmp/custom_addons.tar.gz -C /var/lib/odoo\"", dstOdooC))) {
                    return "❌ Falló extraer custom_addons en /var/lib/odoo";
                }
                if (!executeCommand(String.format("cmd.exe /c docker exec %s bash -lc \"mkdir -p /mnt/extra-addons && tar xzf /tmp/custom_addons.tar.gz -C /mnt/extra-addons\"", dstOdooC))) {
                    return "❌ Falló extraer custom_addons en /mnt/extra-addons";
                }
            } else {
                System.out.println("ℹ️ No hay /var/lib/odoo/custom_addons en origen. Se omite copia.");
            }

            // --- Purge de assets viejos en la DB destino (evita 500 en /web/assets/*) ---
            String purgeAssetsCmd = String.format(
                    "cmd.exe /c docker exec %s psql -U odoo -d %s -c \"DELETE FROM ir_attachment WHERE url LIKE '/web/assets/%%';\"",
                    dstDbC, targetInstance
            );
            if (!executeCommand(purgeAssetsCmd)) {
                return "❌ Falló limpieza de assets en ir_attachment.";
            }

            // --- Rebuild de assets mínimos (web, web_editor, base) con one-shot ---
            String rebuildAssetsCmd = String.format(
                    "cmd.exe /c docker run --rm --network odoo_network " +
                            "-v %s:/var/lib/odoo " +
                            "odoo:17.0 bash -lc \"odoo -d %s --db_host=%s --db_user=odoo --db_password=odoo -u web,web_editor,base --stop-after-init\"",
                    dstOdooVolume, targetInstance, dstDbC
            );
            if (!executeCommand(rebuildAssetsCmd)) {
                return "❌ Falló la reconstrucción de assets (web/web_editor/base).";
            }

            // 6) Actualizar módulos (one-shot, servidor apagado) -U ALL (opcional pero recomendable)
            System.out.println("🔄 Actualizando módulos (one-shot -u all)...");
            String updaterName = "odoo_updater_" + targetInstance + "_" + System.currentTimeMillis();
            String addonsPath = "/usr/lib/python3/dist-packages/odoo/addons,/var/lib/odoo/addons/17.0,/var/lib/odoo/custom_addons,/mnt/extra-addons";
            String updaterCmd = String.format(
                    "cmd.exe /c docker run --rm --name %s --network odoo_network " +
                            "-v %s:/var/lib/odoo " +
                            "-e HOST=%s -e USER=odoo -e PASSWORD=odoo " +
                            "odoo:17.0 bash -lc \"odoo -d %s --db_host=%s --db_user=odoo --db_password=odoo --addons-path=%s -u all --stop-after-init\"",
                    updaterName, dstOdooVolume, dstDbC, targetInstance, dstDbC, addonsPath
            );
            if (!executeCommand(updaterCmd)) {
                executeCommand("cmd.exe /c docker logs " + updaterName + " --since 10m");
                return "❌ Falló la actualización de módulos (ver logs del updater).";
            }

            // 7) Asegurar carpeta de sesiones y permisos
            executeCommand(String.format(
                    "cmd.exe /c docker run --rm -v %s:/target busybox sh -c \"mkdir -p /target/sessions && chmod -R 777 /target/sessions\"",
                    dstOdooVolume
            ));

            // 8) Arrancar Odoo destino
            System.out.println("▶️ Arrancando contenedor Odoo destino...");
            executeCommand("cmd.exe /c docker start " + dstOdooC);

            System.out.println("✅ Merge completado.");
            return "✅ Merge completado exitosamente desde " + sourceInstance + " a " + targetInstance;

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error inesperado durante el merge: " + e.getMessage();
        }
    }



    private String detectarFilestoreBase(String odooContainer, String dbName) throws IOException, InterruptedException {
        String probe = String.format(
                "cmd.exe /c docker exec %s bash -lc \"if [ -d /var/lib/odoo/.local/share/Odoo/filestore/%s ]; then echo /var/lib/odoo/.local/share/Odoo/filestore; " +
                        "elif [ -d /var/lib/odoo/filestore/%s ]; then echo /var/lib/odoo/filestore; fi\"",
                odooContainer, dbName, dbName
        );
        String out = executeCommandAndGetOutput(probe);
        return out == null ? null : out.trim();
    }

    private String detectFilestoreBase(String container) throws IOException, InterruptedException {
        // Primero intenta .local (path moderno)
        int modern = executeCommandAndGetExitCode(
                "docker exec " + container + " bash -lc \"test -d /var/lib/odoo/.local/share/Odoo/filestore\""
        );
        if (modern == 0) return "/var/lib/odoo/.local/share/Odoo/filestore";
        // Si no, usa el legacy
        return "/var/lib/odoo/filestore";
    }

    private String executeCommandAndGetOutput(String command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                out.append(line).append("\n");
            }
        }
        p.waitFor();
        return out.toString();
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