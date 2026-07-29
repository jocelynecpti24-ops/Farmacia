# Guía paso a paso — Sistema de Farmacia (Java + MySQL)

Este documento explica, en el mismo orden en que se construyó, **cada archivo
del proyecto**, qué hace cada bloque de código importante y **por qué** se
escribió así. La idea es que puedas reconstruirlo tú misma línea por línea y
entender el razonamiento detrás de cada decisión, no solo copiarlo.

---

## 0. De dónde salió cada cosa

Antes de escribir una sola línea de Java se leyeron 4 documentos:

| Archivo | Para qué sirvió |
|---|---|
| `Farmacia (1).sql` | Esquema de base de datos que **debía usarse tal cual** (tablas `medicamento`, `venta`, `usuario`, `detalles`, `registra`, `agrega`, `tiene`, `gestion_personal`). |
| `Tabla de Requerimientos Funcionales...docx` | RF01 a RF10: qué debe hacer el sistema. |
| `Historias_de_Usuario_Farmacia...pdf` | Historias 1 a 11: mismo contenido que los RF pero visto "Como / Quiero / Para" + criterios de aceptación puntuales. |
| Proyecto `C:\Users\jesus\IdeaProjects\universidadUT` | **Arquitectura de referencia** a copiar: mismos 4 paquetes, mismo estilo de clases, mismo patrón de conexión y de menú. |

Cada decisión de este documento cita el `RF` o la `Historia` que la justifica.

---

## 1. Arquitectura general (por qué está organizada así)

`universidadUT` separa el código en 4 paquetes con una responsabilidad cada
uno. Este proyecto usa exactamente el mismo esquema, con el mismo
`groupId` (`org.example`), para que puedas comparar archivo por archivo entre
ambos proyectos:

```
org.example
├── Main.java              → único punto de entrada, solo llama a Menu.menu()
├── config
│   └── Coleccion.java      → UNA sola responsabilidad: abrir la conexión JDBC a MySQL
├── modelo
│   ├── Vendible.java              (interfaz)
│   ├── ProductoFarmaceutico.java  (clase abstracta)
│   ├── MedicamentoLibre.java      (hereda de ProductoFarmaceutico)
│   ├── MedicamentoControlado.java (hereda de ProductoFarmaceutico)
│   ├── Usuario.java
│   ├── Venta.java
│   └── Detalle.java
├── dao
│   ├── MedicamentoDAO.java  → todo el SQL relacionado con medicamentos
│   ├── UsuarioDAO.java      → todo el SQL relacionado con usuarios/login
│   ├── VentaDAO.java        → todo el SQL relacionado con salidas de stock
│   └── DetalleDAO.java      → todo el SQL relacionado con la bitácora
└── vista
    └── Menu.java            → la consola: lee del teclado y llama a los DAO
```

**¿Por qué separar así?** Es el mismo motivo que en `universidadUT`:

- **`modelo`** solo sabe representar un dato y validarse a sí mismo (por
  ejemplo, un `Usuario` sabe que su contraseña debe tener 8 caracteres). No
  sabe nada de SQL.
- **`dao`** (Data Access Object) es el único lugar del programa que escribe
  sentencias SQL. Si mañana cambias de MySQL a otro motor, solo tocas esta
  carpeta.
- **`vista`** es la única que hace `System.out.println` y `readLine()`. No
  contiene SQL ni reglas de negocio complejas, solo pregunta datos y los
  pasa a los DAO.
- **`config`** aísla el "cómo conectarse" para no repetir usuario/contraseña
  de MySQL en cada DAO.

Esto se llama separar el proyecto en **capas**, y es la razón por la que en
`AlumnoDAO`/`ProfesorDAO` de la referencia nunca ves un `System.out.print`
pidiendo datos, ni en `Menu.java` ves un `PreparedStatement`.

---

## 2. Orden de creación y explicación de cada archivo

### 2.1 `pom.xml`

Copiado casi igual al de `universidadUT`, solo cambia el `artifactId`:

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.3.0</version>
</dependency>
```

Esta es la **única dependencia externa** del proyecto: el driver JDBC que
permite que las clases de `java.sql.*` (que sí son parte del JDK) sepan
hablar el protocolo de MySQL. Sin esta dependencia, `Class.forName("com.mysql.cj.jdbc.Driver")`
lanzaría `ClassNotFoundException`.

`maven.compiler.source/target = 8` para mantener compatibilidad con la
referencia (aunque el proyecto compila también en versiones más nuevas de
Java).

### 2.2 `sql/farmacia.sql`

Es **el mismo script que me diste**, con 8 columnas añadidas de forma
aditiva (no se borró ni renombró nada tuyo). Cada una está marcada con un
comentario `-- AÑADIDO:` dentro del archivo. Resumen:

| Tabla | Columna añadida | Por qué (RF/Historia) |
|---|---|---|
| `medicamento` | `numero_lote`, `concentracion` | RF03/Historia 3 los pide como campos obligatorios del formulario y no existían. |
| `medicamento` | `precio_base`, `stock` | RF10/Historia 11 exige que `ProductoFarmaceutico` valide un precio > 0 y un stock >= 0. |
| `medicamento` | `tipo_medicamento` | Para poder reconstruir en Java si un registro es `MedicamentoLibre` o `MedicamentoControlado` (polimorfismo) al leerlo de la BD. |
| `venta` | `cedula_medico` | RF10/Historia 11: al despachar un medicamento controlado se debe validar la cédula del médico. |
| `detalles` | `tipo_movimiento` | Historia 9: la bitácora debe mostrar la "Acción (Entrada/Salida)" y la tabla original no distinguía eso. |

Al final del script hay dos `INSERT`:

```sql
insert into venta (productos_venta, cantidadventa, fechaventa, horaventa)
values ('SIN VENTA ASOCIADA (REGISTRO PLACEHOLDER)', 0, curdate(), curtime());
```

**Por qué existe esto:** en tu diseño, `usuario.id_ventas` es `NOT NULL` y
tiene llave foránea a `venta`. Es decir, *todo* usuario, incluso uno recién
creado que nunca ha hecho una salida, debe apuntar a una venta que ya
exista. Para no violar esa restricción se crea una venta "vacía" con
`id = 1`, y todo usuario nuevo apunta ahí hasta que registra su primera
salida real (ver `UsuarioDAO.VENTA_PLACEHOLDER`).

```sql
insert into usuario (nombre, apellidos, contrasenia, telefono, tipo, id_ventas)
values ('admin', 'Administrador General', 'Admin123!', '0000000000', 'Administrador', 1);
```

**Por qué existe esto:** RF08 dice que *solo un Administrador puede crear
usuarios*. Si la tabla `usuario` empieza vacía, nadie podría iniciar sesión
para crear al primer usuario. Por eso el script deja ya un Administrador
inicial: usuario `admin`, contraseña `Admin123!`.

### 2.3 `config/Coleccion.java`

```java
private static final String url = "jdbc:mysql://localhost:3306/farmacia";
```

Igual que `Coleccion` de la referencia, cambia solo el nombre de la base
(`farmacia` en vez de `mydb`). El método `conectar()`:

```java
Class.forName("com.mysql.cj.jdbc.Driver");
conexion = DriverManager.getConnection(url, user, password);
```

1. `Class.forName(...)` carga la clase del driver en memoria (esto registra
   el driver ante `DriverManager`).
2. `DriverManager.getConnection(...)` abre la conexión TCP real contra
   MySQL usando usuario/contraseña.

Cada DAO llama a `Coleccion.conectar()` **una vez por operación** (no se
guarda una conexión global), igual que en la referencia. Es simple de
entender aunque no sea lo más eficiente para una app grande — para una app
de consola de este tamaño es más que suficiente.

> Si tu MySQL local no usa usuario `root` / contraseña `12345678`, este es
> el único archivo que necesitas editar para que la app conecte.

### 2.4 Modelo POO: `Vendible`, `ProductoFarmaceutico`, `MedicamentoLibre`, `MedicamentoControlado`

Este bloque es la respuesta directa a **RF10 / Historia 11**, que pide
explícitamente: clase abstracta + interfaz + dos subclases con herencia y
polimorfismo. Es el mismo patrón que `PersonaUT` (abstracta) + `Alumno` /
`Profesor` (subclases) en la referencia, aplicado a medicamentos.

**`Vendible.java`** — interfaz mínima (como `Ensenable`/`Evaluador` en la
referencia):

```java
public interface Vendible {
    double calcularPrecioFinal();
}
```

Cualquier clase que "se pueda vender" debe saber calcular su propio precio
final. Es una interfaz y no un método normal porque **cada subclase lo
calcula distinto** (esto es polimorfismo).

**`ProductoFarmaceutico.java`** — la clase abstracta. Puntos clave:

- **Todos los atributos son `private`** (encapsulamiento, exigido
  explícitamente en la Historia 11):
  ```java
  private int idMedicamento;
  private String nombreComercial;
  ...
  private double precioBase;
  private int stock;
  ```

- **El constructor llama a los setters**, no asigna los campos
  directamente. Así cualquier objeto `ProductoFarmaceutico`, se cree como se
  cree, siempre pasa por las mismas validaciones:
  ```java
  public ProductoFarmaceutico(int idMedicamento, String nombreComercial, ...) {
      setIdMedicamento(idMedicamento);
      setNombreComercial(nombreComercial);
      ...
      setPrecioBase(precioBase);
      setStock(stock);
  }
  ```

- **Los setters validan y, a diferencia de la referencia, *lanzan
  excepciones*** en vez de solo imprimir un mensaje:
  ```java
  public void setPrecioBase(double precioBase) {
      if (precioBase <= 0) {
          throw new IllegalArgumentException("El precio base debe ser mayor a 0.");
      }
      this.precioBase = precioBase;
  }
  ```
  **Por qué el cambio respecto a `Alumno`/`Profesor`:** en la referencia,
  un setter inválido solo imprime un texto y deja el campo sin actualizar
  (silencioso). La Historia 11 pide explícitamente que "todas las
  validaciones de setters... estén protegidas con bloques `try-catch` para
  desplegar mensajes de error amigables sin detener la ejecución". Eso solo
  tiene sentido si el setter **lanza** una excepción; si no, no habría nada
  que "capturar". Por eso aquí los setters lanzan `IllegalArgumentException`
  y es `Menu.java` quien la captura con `try/catch` y muestra el mensaje.

- **Los getters devuelven el dato "formateado"**, tal como pide la
  Historia 11 ("los getters deben retornar cadenas con formato
  descriptivo... excepto ID y stock que se devuelven nativos"):
  ```java
  public String getNombreComercial() {
      return nombreComercial != null ? nombreComercial.toUpperCase() : "";
  }
  public String getPrecioBase() {
      return String.format("$%.2f", precioBase);   // "$150.00"
  }
  public int getIdMedicamento() { return idMedicamento; }  // nativo
  public int getStock() { return stock; }                  // nativo
  ```
  Como `getPrecioBase()` ahora devuelve un **String** con formato de
  moneda (no un número), las subclases necesitan otra forma de obtener el
  precio numérico para poder calcular con él. Por eso existe:
  ```java
  protected double precioBaseNumerico() { return precioBase; }
  public double getPrecioBaseCrudo() { return precioBase; }
  ```
  `precioBaseNumerico()` es `protected` y lo usan `MedicamentoLibre`/
  `MedicamentoControlado` para calcular el precio final.
  `getPrecioBaseCrudo()` es `public` y lo usa `MedicamentoDAO` para poder
  guardar el número en la columna `DECIMAL` de MySQL (no puedes guardar
  `"$150.00"` en una columna numérica).

- **Semáforo de caducidad (RF05 / Historia 5)**, calculado con la API de
  fechas de Java (`java.time`):
  ```java
  public String obtenerColorSemaforo() {
      long mesesRestantes = ChronoUnit.MONTHS.between(LocalDate.now(), fechaCaducidad);
      if (mesesRestantes < 0)      return "ROJO (CADUCADO)";
      else if (mesesRestantes < 6) return "ROJO";
      else if (mesesRestantes <= 12) return "AMARILLO";
      else                          return "VERDE";
  }
  ```
  `ChronoUnit.MONTHS.between(hoy, fechaCaducidad)` calcula cuántos meses
  completos faltan. Este método vive en la clase **abstracta** (no en las
  subclases) porque el semáforo se calcula igual sin importar si el
  medicamento es libre o controlado.

- **Tres métodos `abstract`** que cada subclase debe completar a su manera:
  ```java
  public abstract String mostrarTipoProducto();
  public abstract String obtenerNormativa();
  public abstract boolean validarRequisitoEspecial(String dato);
  ```

- **`toString()` solo llama a getters** (exigencia explícita de la
  Historia 11), nunca a los campos privados directamente.

**`MedicamentoLibre.java`**: no agrega atributos nuevos. Su
`calcularPrecioFinal()` simplemente devuelve el precio base (sin recargo) y
`validarRequisitoEspecial()` siempre retorna `true` porque un medicamento
libre no exige ningún requisito adicional.

**`MedicamentoControlado.java`**: agrega una constante
`RECARGO_REGULATORIO = 0.15` y **sobrescribe el cálculo del precio**
(polimorfismo real: el mismo método `calcularPrecioFinal()` de la interfaz
`Vendible` da un resultado distinto según la subclase):
```java
@Override
public double calcularPrecioFinal() {
    return precioBaseNumerico() * (1 + RECARGO_REGULATORIO);
}

@Override
public boolean validarRequisitoEspecial(String dato) {
    return dato != null && dato.matches("\\d{4,10}"); // dato = cédula del médico
}
```

### 2.5 `modelo/Usuario.java`

Mapea la tabla `usuario` y aplica **RF07** (roles) y **RF09/Historia 10**
(contraseña segura).

```java
public static final String ROL_ADMINISTRADOR = "Administrador";
public static final String ROL_ENFERMERIA = "Enfermeria";
public static final String ROL_PRACTICANTE = "Practicante";
```
Se usan constantes en vez de "strings sueltos" para no equivocarse al
escribir el nombre del rol en distintas partes del código (`Menu`,
`UsuarioDAO`, etc.).

```java
private static final String REGEX_CONTRASENIA =
        "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$";
```
Esta expresión regular es la traducción directa de la Historia 10: *mínimo
8 caracteres, una mayúscula, una minúscula, un número y un carácter
especial*. Cada `(?=.*X)` es un "look-ahead": exige que en algún punto de la
cadena exista al menos un carácter de tipo X, sin consumirlo.

```java
public String getContrasenia() {
    return contrasenia;   // OJO: no se pone en mayúsculas ni se reformatea
}
```
Es la única excepción al patrón "getter formateado": si convirtiéramos la
contraseña a mayúsculas, `UsuarioDAO.autenticar()` nunca podría comparar
correctamente contra lo que el usuario escribió en el login.

`esAdministrador()` es un método de conveniencia usado todo el tiempo desde
`Menu.java` para decidir qué opciones mostrar (RF07).

### 2.6 `modelo/Venta.java` y `modelo/Detalle.java`

- **`Venta`** mapea la tabla `venta`: representa cada salida/retiro de
  stock (RF06). Incluye `cedulaMedico` (puede ir vacío si el medicamento es
  de venta libre).
- **`Detalle`** mapea la tabla `detalles`: es la fila de bitácora
  (Historia 9). Tiene una constante para el tipo de movimiento:
  ```java
  public static final String MOVIMIENTO_ENTRADA = "ENTRADA";
  public static final String MOVIMIENTO_SALIDA = "SALIDA";
  ```
  Cada registro de medicamento nuevo genera una fila `ENTRADA`; cada salida
  de stock genera una fila `SALIDA`.

### 2.7 `dao/MedicamentoDAO.java`

Punto más importante: **`registrarMedicamento(...)`** no solo hace un
`INSERT INTO medicamento`. Como el esquema de base de datos original tiene
tablas puente (`registra`, `agrega`), un solo registro de medicamento en
realidad escribe en **tres tablas**, en este orden:

1. `INSERT INTO medicamento` (los datos del medicamento en sí).
2. `INSERT INTO registra` (queda constancia de **qué usuario** lo dio de
   alta — trazabilidad, RF07).
3. `INSERT INTO detalles` + `INSERT INTO agrega` (queda constancia del
   movimiento de **ENTRADA** en la bitácora, Historia 9).

```java
try (ResultSet claves = stm.getGeneratedKeys()) {
    claves.next();
    idMedicamento = claves.getInt(1);
}
```
`Statement.RETURN_GENERATED_KEYS` + `getGeneratedKeys()` es la forma
estándar en JDBC de recuperar el `AUTO_INCREMENT` que MySQL generó para la
fila que acabas de insertar — se necesita para poder usarlo en los
`INSERT` siguientes (`registra`, `agrega`).

**`buscar(String texto)`** (RF04/Historia 4):
```java
String sql = "SELECT * FROM medicamento WHERE nombre_comercial LIKE ? " +
        "OR nombre_sustancia LIKE ? OR numero_lote LIKE ? ORDER BY fecha_caducidad ASC";
String comodin = "%" + texto + "%";
```
`LIKE ?` con `%texto%` es una búsqueda "contiene" (no exige coincidencia
exacta). `ORDER BY fecha_caducidad ASC` es lo que garantiza que el lote más
próximo a vencer salga primero (RF04 lo exige explícitamente).

**`construirProducto(ResultSet rs)`** es el método más importante para
entender el polimorfismo con base de datos:
```java
String tipo = rs.getString("tipo_medicamento");
if ("CONTROLADO".equalsIgnoreCase(tipo)) {
    return new MedicamentoControlado(...);
}
return new MedicamentoLibre(...);
```
La base de datos no sabe qué es una "clase Java" — solo guarda texto. Por
eso se guardó la columna `tipo_medicamento`: al leer una fila, este método
decide **en tiempo de ejecución** qué subclase instanciar, y a partir de
ahí todo el resto del programa puede tratar el objeto como un
`ProductoFarmaceutico` sin preocuparse del tipo exacto (polimorfismo).

### 2.8 `dao/UsuarioDAO.java`

**`autenticar(nombre, contrasenia)`** (RF01/RF09):
```java
String sql = "SELECT * FROM usuario WHERE nombre = ? AND contrasenia = ?";
```
Se usa `PreparedStatement` con `?` (no se concatena el texto del usuario
directamente en el SQL). Esto es **obligatorio** por seguridad: evita
inyección SQL. Si alguien escribe `' OR '1'='1` como contraseña, con
`PreparedStatement` eso se trata como un simple texto literal, no como
código SQL.

**`crearUsuario(...)`** usa el `VENTA_PLACEHOLDER = 1` explicado en la
sección del SQL, y después de crear el usuario inserta en
`gestion_personal` quién (qué administrador) lo dio de alta.

**`cambiarContrasenia(...)`** (RF07):
```java
Usuario admin = autenticar(nombreAdmin, claveAdmin);
if (admin == null || !admin.esAdministrador()) {
    System.out.println("Clave de administrador incorrecta. No se realizó el cambio.");
    return false;
}
```
Antes de tocar la tabla, se reutiliza el propio método `autenticar()` para
comprobar que la contraseña de administrador ingresada es correcta. Así se
cumple "cualquier cambio de contraseña exige la clave del Administrador"
sin duplicar lógica de verificación.

### 2.9 `dao/VentaDAO.java` y `dao/DetalleDAO.java`

**`VentaDAO.registrarSalida(...)`** es el método más largo del proyecto
porque, otra vez, el esquema original obliga a tocar varias tablas para una
sola acción de negocio ("retirar stock"):

1. `INSERT INTO venta` (la salida en sí, con `cedula_medico` si aplica).
2. `INSERT INTO detalles` con `tipo_movimiento = 'SALIDA'` (bitácora).
3. `INSERT INTO tiene` (liga la venta con su fila de bitácora).
4. `INSERT INTO agrega` (liga el medicamento con esa misma fila de
   bitácora).
5. `MedicamentoDAO.actualizarStock(...)` (descuenta la cantidad retirada).

**`DetalleDAO.filtrar(usuario, medicamento, fechaTexto)`** (Historia 9)
trae todos los movimientos y filtra en memoria (Java), no con `WHERE`
dinámico en SQL:
```java
boolean coincideUsuario = usuario == null || usuario.trim().isEmpty()
        || detalle.getUsuario().toUpperCase().contains(usuario.toUpperCase());
```
Se hizo así a propósito para que el código sea más fácil de leer: en vez de
armar un `WHERE` con partes opcionales concatenadas (`if` por cada
condición dentro del SQL), se trae la lista completa y se aplican los tres
filtros con `&&`, dejando pasar el registro solo si cumple los tres (o si
un filtro viene vacío, ese filtro simplemente no descarta nada).

`contarPorTipo("ENTRADA")` / `contarPorTipo("SALIDA")` alimentan los
contadores que pide la Historia 9 ("contadores visibles... resumen del
total de entradas y salidas").

### 2.10 `vista/Menu.java` y `Main.java`

`Main.java` es idéntico en espíritu al de la referencia: no contiene
lógica, solo arranca el programa.

`Menu.java` tiene 3 ideas centrales:

**a) Un "bucle de sesión"** en `menu()`:
```java
while (!salirPrograma) {
    if (usuarioActual == null) {
        boolean autenticado = login();
        if (!autenticado) salirPrograma = true;
    } else {
        salirPrograma = mostrarMenuPrincipal();
    }
}
```
Si no hay nadie logueado, se pide login (RF01). Si hay alguien logueado, se
muestra el menú de operaciones. `usuarioActual` es la variable que "recuerda
quién entró" durante toda la ejecución (equivalente a una sesión web, pero
en memoria, porque es una app de consola).

**b) Login con máximo 3 intentos** (Historia 10, "el sistema podrá
bloquear temporalmente el acceso"):
```java
int intentos = 0;
while (intentos < 3) {
    ...
    Usuario encontrado = usuarioDAO.autenticar(nombre, contrasenia);
    if (encontrado != null) { usuarioActual = encontrado; return true; }
    intentos++;
    System.out.println("Usuario o contraseña incorrectos.");
}
```
Nótese que el mensaje de error es el mismo sin importar si falló el
usuario o la contraseña — así lo pide literalmente la Historia 10, para no
darle pistas a quien intenta adivinar credenciales.

**c) Menú que cambia según el rol** (RF02/RF07):
```java
System.out.println("1. Buscar medicamentos");
System.out.println("2. Registrar salida de medicamento");
System.out.println("3. Cambiar mi contraseña");
if (usuarioActual.esAdministrador()) {
    System.out.println("4. Registrar medicamento nuevo");
    System.out.println("5. Crear usuario");
    System.out.println("6. Ver bitácora de movimientos");
}
System.out.println("7. Cerrar sesión");
System.out.println("0. Salir del programa");
```
Las opciones 1, 2 y 3 las ve cualquier rol (Administrador, Enfermería,
Practicante). Las opciones 4, 5 y 6 solo aparecen (y solo se ejecutan,
`case 4/5/6` también revalida `esAdministrador()` por si acaso) cuando el
usuario logueado es Administrador. Esto es el control de acceso por rol que
pide RF07.

**d) Cada acción está envuelta en su propio `try-catch`** (Historia 11),
por ejemplo:
```java
try {
    ...
    ProductoFarmaceutico producto = new MedicamentoLibre(...); // puede lanzar IllegalArgumentException
    medicamentoDAO.registrarMedicamento(...);
} catch (IllegalArgumentException | DateTimeParseException e) {
    System.out.println("No se pudo registrar el medicamento: " + e.getMessage());
}
```
Si el usuario escribe una fecha con formato incorrecto, un precio negativo,
o deja un campo vacío, el programa **no se cae**: imprime el mensaje de la
excepción (que viene de los setters, ver sección 2.4) y vuelve a mostrar el
menú.

---

## 3. Decisiones de diseño (resumen rápido del "por qué")

| Decisión | Por qué |
|---|---|
| Getters con `throw` en vez de `println` (a diferencia de `universidadUT`) | La Historia 11 pide explícitamente manejo de errores con `try-catch`, algo que solo tiene sentido si el setter lanza una excepción real. |
| `getPrecioBase()` devuelve `String`, `getPrecioBaseCrudo()` devuelve `double` | La Historia 11 pide getters "con formato descriptivo (ej. moneda)"; pero los cálculos y el guardado en BD necesitan el número puro. |
| Columna `tipo_medicamento` en `medicamento` | Sin ella, al leer de la base de datos no habría forma de saber si construir un `MedicamentoLibre` o un `MedicamentoControlado` (se perdería el polimorfismo). |
| Venta "placeholder" (`id = 1`) | `usuario.id_ventas` es `NOT NULL` en tu esquema original; un usuario nuevo aún no tiene ventas propias. |
| Usuario `admin` inicial en el script SQL | RF08 exige que solo un Administrador cree usuarios; sin uno inicial, nadie podría crear al primero. |
| Filtrado de la bitácora en Java, no en SQL dinámico | Con 3 filtros opcionales combinables, es más legible construir el `WHERE` con `&&` en Java que concatenar SQL condicionalmente. |
| Recargo del 15% en `MedicamentoControlado.calcularPrecioFinal()` | Ejemplo concreto de polimorfismo pedido por RF10/Historia 11 (mismo método, resultado distinto según subclase). No es un valor real de ninguna ley — puedes cambiarlo. |

---

## 4. Cómo ejecutar el proyecto

1. **Base de datos**: abre MySQL Workbench (o consola `mysql`) y ejecuta
   todo el contenido de `sql/farmacia.sql`. Esto borra y vuelve a crear la
   base `farmacia` completa, con el usuario `admin` ya cargado.
2. **Conexión**: revisa `src/main/java/org/example/config/Coleccion.java`.
   Si tu contraseña de MySQL no es `12345678`, cámbiala ahí.
3. **Abrir en IntelliJ**: `File > Open`, selecciona la carpeta
   `FarmaciaApp` (donde está el `pom.xml`). IntelliJ debería reconocerlo
   como proyecto Maven y descargar `mysql-connector-j` automáticamente.
4. **Ejecutar**: abre `Main.java` y dale ▶ (Run).
5. **Primer login**: usuario `admin`, contraseña `Admin123!`.
6. Desde ahí, como Administrador, ya puedes usar la opción **5. Crear
   usuario** para dar de alta a Enfermería/Practicantes, y la opción
   **4. Registrar medicamento nuevo** para cargar el inventario.

Este proyecto se verificó compilando manualmente con `javac` (sin
necesidad de tener Maven instalado) y no arrojó errores; con Maven/IntelliJ
debería compilar igual de limpio.

---

## 5. Mapa Requerimiento → Código

Útil para tu sustentación/documentación: dónde vive cada requerimiento.

| RF / Historia | Dónde está implementado |
|---|---|
| RF01/RF09, Historia 1, 10 | `Menu.login()`, `UsuarioDAO.autenticar()` |
| RF02, Historia 2 | `Menu.mostrarMenuPrincipal()` opción 7 (cerrar sesión) |
| RF03, Historia 3 | `Menu.registrarMedicamentoNuevo()`, `MedicamentoDAO.registrarMedicamento()` |
| RF04, Historia 4 | `Menu.buscarMedicamentos()`, `MedicamentoDAO.buscar()` |
| RF05, Historia 5 | `ProductoFarmaceutico.obtenerColorSemaforo()` |
| RF06, Historia 6 | `Menu.registrarSalidaMedicamento()`, `VentaDAO.registrarSalida()` |
| RF07, Historia 7 | `Usuario.esAdministrador()`, menú condicional en `Menu.mostrarMenuPrincipal()`, `UsuarioDAO.cambiarContrasenia()` |
| RF08, Historia 8 | `Menu.crearUsuarioNuevo()`, `UsuarioDAO.crearUsuario()`, `UsuarioDAO.existeTelefono()` |
| Historia 9 (Bitácora) | `Menu.verBitacora()`, `DetalleDAO` completo |
| RF10, Historia 11 (POO) | Paquete `modelo` completo: `Vendible`, `ProductoFarmaceutico`, `MedicamentoLibre`, `MedicamentoControlado` |

---

## 6. Para seguir practicando

Ideas para extender tú misma el proyecto y afianzar lo aprendido:

- Agregar un `MedicamentoDAO.eliminarMedicamento(id)` (similar a
  `AlumnoDAO.eliminarAlumno` de la referencia).
- Agregar una opción de menú "Listar todo el inventario"
  (`MedicamentoDAO.listarTodos()` ya existe, solo falta usarla en `Menu`).
- Guardar el número de intentos fallidos de login *por usuario* en la base
  de datos en vez de solo en memoria (para que el bloqueo sobreviva a
  reiniciar el programa).
- Agregar una tercera subclase, por ejemplo `MedicamentoRefrigerado`, y ver
  qué tan poco código nuevo hace falta gracias al polimorfismo.
