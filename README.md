# Tesis Maestría

Este repositorio contiene el desarrollo del proyecto de tesis de maestría, dividido en dos ramas principales:

- **FrontEndF:** Código fuente del frontend de la aplicación.
- **backend:** Código fuente del backend y API.

---

## Prerrequisitos

### Comunes

- Linux, Windows o macOS
- Docker 20+ y permisos para ejecutar `docker exec` (usuario en grupo docker o socket montado)

### Backend

- Java 17+ (JDK)
- Maven 3.9+

### Frontend

- Node 18+ (recomendado LTS)
- Angular CLI 16+ (o la versión usada en el proyecto)

---

## Variables de entorno recomendadas

| Variable            | Descripción                                  | Ejemplo                           |
|---------------------|----------------------------------------------|-----------------------------------|
| SERVER_PORT         | Puerto del backend                           | 8080                              |
| ALLOWED_ORIGINS     | Orígenes permitidos para WebSocket (patrones)| * o https://midominio.com         |
| CONTAINER_NAME_PREFIX | Prefijo permitido para contenedores        | odoo_instance_                    |
| WS_BASE_URL (front) | Base del WS para el front                    | ws://localhost:8080 o wss://midominio.com |

---

## Desarrollo local

### 1. Backend (Spring Boot)

```bash
# En el directorio del backend
mvn -DskipTests package
java -jar target/*.jar
# El backend escucha en http://localhost:8080
```

**Permisos Docker:**  
Asegúrate de que tu usuario puede ejecutar `docker exec` sin sudo.  
Si no, añade el usuario al grupo docker y reabre sesión:

```bash
sudo usermod -aG docker $USER
```

### 2. Frontend (Angular)

Configura el archivo `src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  wsBaseUrl: 'ws://localhost:8080' // backend local
};
```

Arranque del frontend:

```bash
# En el directorio del frontend
npm ci
ng serve --host 0.0.0.0 --port 4200
# UI en http://localhost:4200
```

En el código del front, construye el WebSocket así:

```typescript
const url = `${environment.wsBaseUrl}/ws/shell?container=${encodeURIComponent(containerName)}`;
```

---

## Seguridad mínima recomendada

- Validar el contenedor objetivo en el backend (prefijo/whitelist por proyecto).
- CORS/WS: restringe `ALLOWED_ORIGINS` en producción.
- RBAC: controla quién puede abrir sesiones y sobre qué instancias.
- Auditoría: registra apertura/cierre de sesión, contenedor usado y líneas enviadas (según políticas).
- Secretos: guarda credenciales en un vault; nunca en el repositorio.

---

## Problemas comunes y solución

### “WS no conectado”

- URL del WS apunta al puerto equivocado (por ejemplo, a 4200 en vez de 8080).
- Mixed content: front en HTTPS y WS en ws:// (usa wss://).
- `ALLOWED_ORIGINS` no incluye tu dominio.

### No se puede ejecutar docker exec

- Backend sin permisos Docker (agrega usuario al grupo docker).
- En Compose: falta montar `/var/run/docker.sock`.

---

## Estructura de archivos sugerida

```
repo/
├─ backend/
│  ├─ src/...
│  ├─ pom.xml
│  └─ Dockerfile
├─ frontend/
│  ├─ src/...
│  ├─ angular.json
│  └─ package.json
└─ ops/
   ├─ nginx.conf
   └─ docker-compose.yml
```

---

## Autores

Sebastian Bedoya  
Paul Guzhnay 

## Licencia

Uso académico.
