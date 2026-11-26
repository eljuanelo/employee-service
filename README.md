# Employee Service API

## 1. Descripción y objetivo

API REST para gestionar empleados, desarrollada como evaluación técnica para la posición de Desarrollador Java Senior en INVEX. Permite consultar, crear individualmente o por lote, actualizar, eliminar y buscar empleados por nombre. La API protege las operaciones de empleados con JWT y utiliza MySQL para la persistencia.

## 2. Stack tecnológico

| Componente | Versión utilizada |
|---|---|
| Java | 17 |
| Maven / Maven Wrapper | Maven 3.9.16 / Wrapper 3.3.4 |
| Spring Boot, Spring Data JPA y Actuator | 2.7.18 |
| Spring MVC y MockMvc | 5.3.31 |
| Spring Security y OAuth2 Resource Server | 5.7.11 |
| Nimbus JOSE + JWT | 9.22 |
| Hibernate ORM / Hibernate Validator | 5.6.15.Final / 6.2.5.Final |
| MySQL / MySQL Connector/J | Imagen `mysql:8.0` / 8.0.33 |
| H2 | 2.1.214, solo en pruebas |
| Lombok | 1.18.30 |
| Springdoc OpenAPI / Swagger UI | 1.8.0 / 5.11.8 |
| JUnit Jupiter / Mockito | 5.8.2 / 4.5.1 |
| Logback | 1.2.12 |

Se utiliza `javax.persistence` y `javax.validation`, de acuerdo con el stack de Spring Boot 2.7. La versión del artefacto Maven es `0.0.1-SNAPSHOT`. La versión del contrato OpenAPI y la etiqueta de imagen prevista en Kubernetes son `1.0.0`.

## 3. Arquitectura por capas

El paquete base es `com.invex.employee`. Los controladores delegan en el servicio, y el servicio utiliza el repositorio para acceder a los datos.

| Capa | Responsabilidad |
|---|---|
| Controller | Expone las rutas REST, valida la entrada y determina los códigos HTTP. |
| Service | Aplica las reglas de negocio y delimita transacciones. `JwtService` genera los tokens de acceso. |
| Repository | Usa Spring Data JPA para persistir y consultar empleados, incluida la búsqueda parcial. |
| Entity | Representa la tabla `employees` y define los valores iniciales de persistencia. |
| DTO | Define los contratos JSON de entrada, salida y error. |
| Mapper | Convierte entre DTO y entidades y aplica los campos presentes en las actualizaciones. |
| Exception | Define los errores de negocio y transforma los errores conocidos en respuestas HTTP. |
| Config | Configura Spring Security y OpenAPI. |
| Filter | Registra información de las peticiones antes de Spring Security. |

## 4. Estructura del proyecto

~~~text
employee-service/
├── .mvn/wrapper/
├── develop/
├── postman_collection/
├── src/
│   ├── main/
│   │   ├── java/com/invex/employee/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── enums/
│   │   │   ├── exception/
│   │   │   ├── filter/
│   │   │   ├── mapper/
│   │   │   ├── repository/
│   │   │   └── service/impl/
│   │   └── resources/application.properties
│   └── test/
│       ├── java/com/invex/employee/
│       └── resources/application-test.properties
├── .dockerignore
├── .env.example
├── Dockerfile
├── docker-compose.yml
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
~~~

## 5. Requisitos previos

- Git y acceso autorizado al repositorio privado.
- Docker con Docker Compose para la ejecución principal y para el MySQL de desarrollo.
- JDK 17 con `JAVA_HOME` apuntando a su carpeta raíz para la ejecución local y las pruebas. Maven Wrapper descarga Maven en el primer uso.
- Puertos locales `8080` para la API y `3306` para MySQL disponibles.
- Postman es opcional para ejecutar la colección.

## 6. Variables de entorno

Desde la raíz del repositorio, crea `.env` a partir de la plantilla y sustituye los valores de ejemplo.

**Git Bash**

~~~bash
cp .env.example .env
~~~

**PowerShell**

~~~powershell
Copy-Item .env.example .env
~~~

| Variable | Uso |
|---|---|
| `MYSQL_DATABASE` | Base que MySQL crea al inicializar un volumen nuevo. La plantilla usa `employee_db`. |
| `DB_USERNAME` | Usuario de la aplicación en MySQL. |
| `DB_PASSWORD` | Contraseña del usuario de MySQL. |
| `MYSQL_ROOT_PASSWORD` | Contraseña de administración de MySQL. |
| `DB_URL` | URL JDBC para ejecutar Spring Boot desde el host. La plantilla usa `jdbc:mysql://localhost:3306/employee_db`. Compose genera su propia URL con `mysql:3306` y `MYSQL_DATABASE`. |
| `API_USERNAME` | Usuario en memoria de la API. La plantilla usa `employee_api`. |
| `API_PASSWORD` | Contraseña del usuario de la API. |
| `JWT_SECRET` | Clave de firma HS256 en Base64. Debe decodificar a por lo menos 32 bytes. |

Genera `JWT_SECRET` con bytes aleatorios y copia el resultado en `.env`.

~~~bash
openssl rand -base64 32
~~~

En PowerShell también puedes generarlo sin OpenSSL.

~~~powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $rng.GetBytes($bytes)
    [Convert]::ToBase64String($bytes)
} finally {
    $rng.Dispose()
}
~~~

Git excluye `.env` y `develop/secret.yaml` para mantener las credenciales y la clave de firma fuera del historial. Las plantillas versionadas contienen valores de sustitución. Docker excluye los archivos de entorno del contexto de construcción e inyecta los valores al arrancar los contenedores.

## 7. Ejecución principal con Docker Compose

~~~bash
git clone https://github.com/eljuanelo/employee-service.git
cd employee-service
~~~

Crea y completa `.env` como se indica arriba. Luego construye e inicia ambos servicios desde la raíz del repositorio.

~~~bash
docker compose up --build -d
docker compose ps
docker compose logs --tail=100 employee-service mysql
~~~

El Dockerfile ejecuta las pruebas y genera el JAR en la etapa de construcción. La imagen final ejecuta el JAR con Java 17 y un usuario sin privilegios. Compose espera a que MySQL esté saludable antes de iniciar la API. Comprueba la aplicación en [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health).

Para detener ambos servicios conservando los contenedores, ejecuta `docker compose stop`. MySQL guarda sus datos en el volumen nombrado `mysql-data`. El volumen también se conserva con `docker compose down`. Evita la opción `-v` si necesitas conservar los datos. Las credenciales de inicialización de MySQL se aplican al crear la base en un volumen nuevo.

## 8. Ejecución local con Java 17 y MySQL en Docker

Detén únicamente la API de Compose para liberar el puerto `8080` y deja MySQL en ejecución. Si MySQL aún no está iniciado, el segundo comando lo inicia.

~~~bash
docker compose stop employee-service
docker compose up -d mysql
docker compose ps mysql
~~~

Cuando MySQL esté saludable, carga el `.env` de desarrollo y ejecuta Spring Boot en la misma terminal. Mantén `DB_URL` apuntando a `localhost:3306` y usa la misma base configurada en `MYSQL_DATABASE`. Los ejemplos de carga suponen líneas simples `CLAVE=valor`, como en la plantilla.

**Git Bash**

~~~bash
java -version
set -a
. ./.env
set +a
sh ./mvnw spring-boot:run
~~~

**PowerShell**

~~~powershell
java -version
Get-Content .env | ForEach-Object {
    if ($_ -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
    }
}
.\mvnw.cmd spring-boot:run
~~~

La API se ejecuta en `http://localhost:8080`. Si el puerto sigue ocupado, detén el proceso que lo utiliza antes de iniciar Spring Boot.

## 9. Endpoints REST

| Método | Ruta | Descripción | Códigos principales |
|---|---|---|---|
| POST | `/auth/login` | Autentica y emite un JWT. | `200`, `400`, `401` |
| GET | `/employees` | Lista todos los empleados. | `200`, `401` |
| GET | `/employees/{id}` | Consulta un empleado por ID. | `200`, `400`, `401`, `404` |
| POST | `/employees` | Crea un empleado o un lote en una transacción. | `201`, `400`, `401` |
| PUT | `/employees/{id}` | Actualiza los campos proporcionados. | `200`, `400`, `401`, `404` |
| DELETE | `/employees/{id}` | Elimina físicamente el empleado. | `204`, `400`, `401`, `404` |
| GET | `/employees/search?name={name}` | Busca coincidencias parciales en nombres y apellidos, sin distinguir mayúsculas. | `200`, `400`, `401` |

Las peticiones con cuerpo usan `Content-Type: application/json`. El POST siempre responde con un arreglo, incluso si recibe un solo objeto. La búsqueda devuelve un arreglo vacío cuando no hay coincidencias. El DELETE exitoso devuelve `204` sin cuerpo.

`birthDate` utiliza `dd-MM-yyyy`. `age` se valida en años cumplidos al crear y cuando cambia `age` o `birthDate` en una actualización. Ajusta las edades de los ejemplos a la fecha en que los ejecutes. `sex` acepta `MALE` y `FEMALE`. En la creación, `middleName` y `secondLastName` son opcionales y `active` toma `true` si se omite o se envía como `null`. El servidor genera `id` y `createdAt` al persistir. `createdAt` se devuelve en ISO-8601 y se conserva al actualizar.

**Creación individual**

~~~json
{
  "firstName": "Alex",
  "lastName": "García",
  "age": 32,
  "sex": "FEMALE",
  "birthDate": "08-06-1994",
  "position": "Software Engineer"
}
~~~

**Creación múltiple**

~~~json
[
  {
    "firstName": "María",
    "middleName": "Elena",
    "lastName": "García",
    "age": 31,
    "sex": "FEMALE",
    "birthDate": "10-03-1995",
    "position": "Business Analyst"
  },
  {
    "firstName": "Pedro",
    "lastName": "Martínez",
    "age": 38,
    "sex": "MALE",
    "birthDate": "20-08-1988",
    "position": "Technical Lead",
    "active": false
  }
]
~~~

**Actualización parcial**

~~~json
{
  "position": "Senior Software Engineer",
  "active": false
}
~~~

En el PUT, los campos omitidos o enviados como `null` conservan el valor anterior. `active: false` cambia el estado a inactivo. El estado es independiente del borrado físico que realiza DELETE.

## 10. Seguridad con Spring Security y JWT

Envía `username` y `password` en el JSON de `POST /auth/login` usando los valores de `API_USERNAME` y `API_PASSWORD`. La respuesta incluye `token`, `tokenType` con valor `Bearer` y `expiresIn` con valor `3600`. Incluye `Authorization: Bearer <token>` en las peticiones protegidas.

La API valida las firmas HS256, el emisor `employee-service` y una vigencia de 3600 segundos (una hora). Spring Security opera sin sesiones. Las rutas públicas son `POST /auth/login`, `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs`, `/v3/api-docs/**` y las rutas de salud bajo `/actuator/health`. Las rutas de empleados y las demás peticiones requieren un JWT válido.

## 11. Swagger y OpenAPI

- Swagger UI. [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- Contrato JSON. [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Ejecuta `POST /auth/login` desde Swagger, copia el valor de `token`, abre **Authorize** e introduce el token sin el prefijo `Bearer` en `bearerAuth`. Swagger agrega el prefijo al enviar las peticiones de empleados.

## 12. Colección Postman

Importa [Employee Service API.postman_collection.json](postman_collection/Employee%20Service%20API.postman_collection.json) desde **Import** en Postman. En las variables de la colección, confirma `baseUrl` y `username` y completa `password` con el valor local de `API_PASSWORD`. Mantén esas credenciales en tu entorno local de Postman.

**Login** guarda el JWT en `token` al recibir `200`. **Create employee** guarda en `employeeId` el primer ID del arreglo al recibir `201`. Los dos ejemplos de creación calculan las edades antes de enviar la petición. La búsqueda utiliza la variable `searchName`.

Orden recomendado. **Login** → **Create employee** → **Find all employees** → **Find employee by ID** → **Update employee** → **Search employees by name** → **Delete employee**. **Create multiple employees** se puede ejecutar después del login; el ID guardado sigue siendo el de la creación individual.

## 13. Pruebas automatizadas

**Windows PowerShell**

~~~powershell
.\mvnw.cmd clean test
~~~

**Git Bash**

~~~bash
sh ./mvnw clean test
~~~

Las pruebas unitarias usan JUnit 5 y Mockito para el servicio. El mapper y el filtro se prueban directamente. Las pruebas web del controlador usan MockMvc con el servicio simulado. Las pruebas de integración verifican persistencia y búsquedas con Spring Data JPA y H2, así como login y autorización con los filtros reales de Spring Security. El perfil `test` proporciona sus propios valores de configuración. La suite puede ejecutarse sin MySQL ni Docker.

Última ejecución de `clean test` con Java 17 y Maven 3.9.16 mediante Maven Wrapper 3.3.4. **82 pruebas exitosas, 0 fallos, 0 errores y 0 omitidas**. Los reportes XML se generan en `target/surefire-reports/`.

## 14. Logging

`RequestLoggingFilter` registra en `INFO` el método, la URI sin query string, el código HTTP y la duración en milisegundos. En `DEBUG` registra únicamente `Content-Type` y `Accept`, con valores sanitizados y limitados a 256 caracteres. El filtro corre antes de Spring Security y registra también las respuestas de rechazo. Excluye cabeceras de autenticación, cookies, tokens, contraseñas y cuerpos de peticiones y respuestas.

## 15. Health Check

Spring Boot Actuator expone tres rutas públicas de salud.

- Estado general. [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- Liveness. [http://localhost:8080/actuator/health/liveness](http://localhost:8080/actuator/health/liveness)
- Readiness. [http://localhost:8080/actuator/health/readiness](http://localhost:8080/actuator/health/readiness)

## 16. Kubernetes

Los manifiestos de [`develop/`](develop/) están preparados y se revisaron localmente. **No se han validado en un clúster Kubernetes.**

| Archivo | Función |
|---|---|
| [`deployment.yaml`](develop/deployment.yaml) | Define dos réplicas con la imagen `employee-service:1.0.0`, sondas de Actuator y ejecución con UID `10001`. |
| [`service.yaml`](develop/service.yaml) | Expone la API dentro del clúster mediante un `ClusterIP` en el puerto `8080`. |
| [`configmap.yaml`](develop/configmap.yaml) | Define `DB_URL` con un host MySQL de ejemplo. |
| [`secret.example.yaml`](develop/secret.example.yaml) | Plantilla `Opaque` para preparar el Secret con las credenciales y la clave JWT. |

La imagen debe construirse con el tag indicado y estar disponible para los nodos. El despliegue requiere una instancia de MySQL accesible desde los pods y la base creada. Ajusta `DB_URL` y prepara `develop/secret.yaml` con valores del entorno antes de aplicar los recursos.

## 17. Estrategia Git Flow

| Rama | Propósito |
|---|---|
| `main` | Contiene la versión estable que se entrega. |
| `develop` | Integra y valida los cambios antes de la entrega. |
| `feature/employee-service` | Rama de trabajo del microservicio. |

El flujo de integración propuesto usa un Pull Request de `feature/employee-service` a `develop`. Tras revisar el cambio y ejecutar las pruebas, un segundo Pull Request lleva la versión aprobada de `develop` a `main`.

## 18. Evidencias de funcionamiento

Pendiente de incorporar capturas reales de los servicios en ejecución, los resultados de Swagger o Postman y la salida de las pruebas automatizadas.
