# Afinavila API

API REST para la gestión de comunidades de propietarios y su documentación.

## Stack

- **Ktor 3.0.2** (Netty) — framework HTTP
- **Exposed 0.41.1** — ORM sobre SQLite
- **Java JWT 4.4.0** — autenticación JWT (HMAC256)
- **Gson** — serialización JSON
- **Kotlin 2.1.0** / **JDK 21**

## Estructura

```
src/main/kotlin/es/afinavila/
├── Application.kt          # Entry point, lee vars de entorno
├── models/
│   ├── Comunidad.kt        # Tabla comunidad (id, nombre, codigo_acceso)
│   ├── Archivo.kt          # Tabla archivo (id, nombre, nombre_mostrar, descripcion, FK comunidad)
│   └── AuthModels.kt       # LoginRequest / LoginResponse
├── services/
│   ├── FileNameParser.kt   # Parsea nombres de archivo → nombreMostrar + descripción
│   ├── ComunidadService.kt # CRUD comunidades
│   ├── ArchivoService.kt   # CRUD archivos + sync desde disco
│   └── AuthService.kt      # Validación de contraseña + generación JWT
├── plugins/
│   ├── Database.kt         # Conexión SQLite, creación de tablas
│   ├── Serialization.kt    # Content Negotiation con Gson
│   ├── Security.kt         # JWT Authentication plugin
│   ├── StatusPages.kt      # Manejo global de errores
│   └── Routing.kt          # Registro de rutas
└── routes/
    ├── ComunidadRoutes.kt  # CRUD /comunidades (POST/PUT/DELETE requieren JWT)
    ├── ArchivoRoutes.kt    # CRUD /archivos, /archivo (POST/DELETE requieren JWT)
    └── AuthRoutes.kt       # POST /auth/login → JWT
```

## Endpoints

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| GET | `/comunidades` | No | Lista todas las comunidades |
| GET | `/comunidades/{id}` | No | Detalle de una comunidad |
| POST | `/comunidades` | JWT | Crear comunidad |
| PUT | `/comunidades/{id}` | JWT | Actualizar comunidad |
| DELETE | `/comunidades/{id}` | JWT | Eliminar comunidad + archivos |
| GET | `/archivos/{comunidadId}` | No | Lista archivos de una comunidad |
| GET | `/archivo/{id}` | No | Metadatos de un archivo |
| GET | `/archivo/pdf/{id}` | No | Descarga el PDF |
| POST | `/archivo/{comunidadId}` | JWT | Subir archivo(s) (multipart) |
| DELETE | `/archivo/{id}` | JWT | Eliminar archivo |
| POST | `/auth/login` | No | Login admin → devuelve JWT |

### Formato de respuesta de archivo

```json
{
  "id": 1,
  "nombre": "acta250324.pdf",
  "nombreMostrar": "Acta 25/03/2024",
  "descripcion": "Acta de reunión del 25 de Marzo de 2024",
  "comunidadId": 1,
  "categoria": "Actas"
}
```

### Categorías detectadas por FileNameParser

| Prefijo | Ejemplo | Categoría | nombreMostrar |
|---------|---------|-----------|---------------|
| `acta` | `acta250324.pdf` | Actas | Acta 25/03/2024 |
| `evo` | `evo2407.pdf` | Evoluciones | Evolución 2do sem. 2024 |
| `ene`-`dic` | `feb24.pdf` | Extractos | Febrero 2024 |
| `cuota` | `cuota2025.pdf` | Otros | Cuota 2025 |

## Sincronización desde disco

Al arrancar, la API escanea la carpeta `FILES_PATH` (por defecto `comunidades/`). Cada subdirectorio se interpreta como una comunidad (nombre de carpeta = código de acceso). Los archivos `.pdf` dentro se registran automáticamente en la BD.

```
comunidades/
├── af3w7h/          ← codigoAcceso
│   ├── acta250324.pdf
│   └── ene24.pdf
└── x9k2m5/
    └── feb25.pdf
```

## Variables de entorno

| Variable | Default | Descripción |
|----------|---------|-------------|
| `PORT` | 8081 | Puerto HTTP |
| `DB_PATH` | `data/afinavila.db` | Ruta del archivo SQLite |
| `FILES_PATH` | `comunidades` | Ruta raíz de archivos de comunidades |
| `JWT_SECRET` | `change-me` | Secreto para firmar tokens JWT |
| `ADMIN_PASSWORD` | `admin` | Contraseña del admin |

## Ejecutar

```bash
# Desarrollo
./gradlew run

# Build JAR
./gradlew shadowJar
java -jar build/libs/afinavila-all.jar

# Docker
docker compose up -d --build
```
