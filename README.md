# HuertoHogar Backend 🌱

API REST de la tienda **HuertoHogar**, proyecto de la **Evaluación Parcial N°3 de DSY1104 (Desarrollo Fullstack II, sección 003D, Duoc UC)**.
Backend en **Spring Boot** conectado a **MySQL**, con seguridad **JWT por roles** y documentación en **Swagger**.

Frontend React: https://github.com/samu189/huerto-hogar

## 👥 Equipo

- Samuel Berrios
- Antonio Espinoza
- Tomas Gonzalez Llanos

## 🛠️ Tecnologías

| Área | Herramienta |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.3.5 (Web, Data JPA, Validation, Security) |
| Base de datos | MySQL 8 (Hibernate crea las tablas automáticamente) |
| Seguridad | Spring Security + JWT (JJWT 0.12.6), contraseñas con BCrypt |
| Documentación | springdoc-openapi 2.6.0 (Swagger UI) |
| Pruebas | JUnit 5 + MockMvc + H2 en modo MySQL |

## 🚀 Cómo ejecutar

Requisitos: **Java 17+** y **MySQL 8** encendido (también sirve XAMPP). Maven no es necesario: el proyecto trae el Maven Wrapper.

```bash
# Si tu MySQL tiene contraseña (en XAMPP, root no tiene clave y no hace falta):
#   PowerShell:  $env:DB_PASSWORD="tu_clave"
.\mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run          # macOS / Linux
```

- La base `huertohogar` se crea sola la primera vez y se cargan datos de ejemplo.
- API: http://localhost:8080/api/v1
- Swagger: http://localhost:8080/swagger-ui.html

### Cuentas de prueba

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | admin@huertohogar.cl | admin123 |
| Vendedor | vendedor@huertohogar.cl | vendedor123 |
| Cliente | pedro.hacker20@example.com | pedro123 |

### Variables de entorno opcionales

| Variable | Por defecto | Uso |
|---|---|---|
| `DB_USER` | `root` | Usuario de MySQL |
| `DB_PASSWORD` | (vacía) | Contraseña de MySQL |
| `JWT_SECRET` | clave de desarrollo | Clave Base64 para firmar los tokens |

## 🔐 Roles y permisos

| Recurso | Visitante | Cliente | Vendedor | Admin |
|---|---|---|---|---|
| Ver productos y categorías | ✅ | ✅ | ✅ | ✅ |
| Comprar y ver sus compras | ❌ | ✅ | ✅ | ✅ |
| Ver todas las órdenes | ❌ | ❌ | ✅ | ✅ |
| CRUD de productos, categorías y usuarios; cambiar estado de órdenes | ❌ | ❌ | ❌ | ✅ |

Login en `POST /api/v1/auth/login` → devuelve `token` (60 min) y `refreshToken` (7 días).
Las rutas protegidas usan el encabezado `Authorization: Bearer <token>`. El token se renueva con `POST /api/v1/auth/refresh`.

## 📚 Endpoints (v1)

| Recurso | Endpoints |
|---|---|
| Autenticación | `POST /auth/login`, `POST /auth/registro`, `POST /auth/refresh`, `GET /auth/me` |
| Productos | `GET /productos`, `GET /productos/{id}`, `GET /productos/stock-critico`, `POST`, `PUT /{id}`, `DELETE /{id}` |
| Categorías | `GET /categorias`, `GET /categorias/{id}`, `POST`, `PUT /{id}`, `DELETE /{id}` |
| Usuarios | `GET /usuarios`, `GET /usuarios/{id}`, `POST`, `PUT /{id}`, `DELETE /{id}` |
| Boletas | `POST /boletas`, `GET /boletas/mis-boletas`, `GET /boletas`, `GET /boletas/{id}`, `PUT /boletas/{id}/estado`, `DELETE /boletas/{id}` |

El detalle completo (datos de entrada, respuestas y roles) está en Swagger y en la documentación del proyecto.

## 🗄️ Modelo de datos

`usuario` 1─N `boleta` 1─N `detalle_boleta` N─1 `producto` N─1 `categoria`. El script de las tablas está en [`database/schema.sql`](database/schema.sql).

Reglas de negocio principales (en la capa de servicios):
- Una compra valida stock, usa el precio de oferta si corresponde, calcula el total y descuenta el stock en una sola transacción.
- No se elimina una categoría con productos ni un producto con ventas.
- Al anular una boleta se devuelve el stock.
- Un administrador no puede eliminarse a sí mismo ni eliminar al último administrador.

## 🧪 Pruebas

```bash
.\mvnw.cmd test
```

18 pruebas en 3 clases: `ReglasNegocioTest` (lógica de negocio), `AuthControllerTest` (JWT) y `SeguridadRolesTest` (permisos por rol). Usan una base H2 en memoria, así que no necesitan MySQL.

## 📁 Estructura

```text
src/main/java/cl/duoc/huertohogar/
├── controller/   # endpoints REST /api/v1
├── service/      # lógica de negocio
├── repository/   # acceso a datos (Spring Data JPA)
├── model/        # entidades: Usuario, Categoria, Producto, Boleta, DetalleBoleta
├── dto/          # datos de entrada y salida
├── security/     # JwtService, JwtAuthFilter, SecurityConfig
├── config/       # Swagger y datos iniciales
└── exception/    # manejo global de errores
```
