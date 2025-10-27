# 🧩 Mopla Template Engine

**Mopla** es un motor de plantillas minimalista para **Java**, inspirado en Blade/Laravel, con soporte para layouts, includes, bucles y condicionales.

Compatible con cualquier proyecto **Java** o **Spring Boot**.

---

## 🚀 Instalación (vía JitPack)

1️⃣ Agrega el repositorio de JitPack a tu `pom.xml`:

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
````

2️⃣ Luego agrega la dependencia:

```xml
<dependency>
  <groupId>com.github.ronaldbit</groupId>
  <artifactId>mopla-java</artifactId>
  <version>v0.1.0</version>
</dependency>
```

> 🟢 Si no creaste un tag aún, puedes usar `main-SNAPSHOT` en vez de `v0.1.0`.

---

## 🧠 Ejemplo de uso

### Estructura

```
src/
 └─ main/
     ├─ java/
     │   └─ com/example/App.java
     └─ resources/
         └─ templates/
             ├─ layout.html
             ├─ page.html
             ├─ header.html
             └─ footer.html
```

---

### layout.html

```html
<html>
<head><title>@var(title)</title></head>
<body>
  @include("header.html")
  <main>@yield("content")</main>
  @include("footer.html")
</body>
</html>
```

### page.html

```html
@extend("layout.html")
@section("content")
<h1>@var(title|upper)</h1>

@if(showMessage)
  <p>@var(message)</p>
@else
  <p>No hay mensaje</p>
@endif

<ul>
@foreach(user in users)
  <li>@var(user)</li>
@endforeach
</ul>
@endsection
```

---

### App.java

```java
import com.ronaldbit.Mopla;
import java.util.*;

public class App {
    public static void main(String[] args) throws Exception {
        Mopla mopla = new Mopla("src/main/resources/templates")
                .setDevMode(true)
                .setCacheEnabled(true);

        Map<String,Object> vars = new HashMap<>();
        vars.put("title", "Usuarios");
        vars.put("showMessage", true);
        vars.put("message", "Listado cargado con éxito");
        vars.put("users", List.of("Ronald", "User2", "Pedro"));

        String html = mopla.render("page.html", vars);
        System.out.println(html);
    }
}
```

Salida:

```html
<html>
<head><title>Usuarios</title></head>
<body>
  <header>...</header>
  <main>
    <h1>USUARIOS</h1>
    <p>Listado cargado con éxito</p>
    <ul>
      <li>Ronald</li>
      <li>User2</li>
      <li>Pedro</li>
    </ul>
  </main>
  <footer>...</footer>
</body>
</html>
```

---

## 🧩 Características

| Directiva                       | Descripción                         | Ejemplo                                   |                 |   |
| ------------------------------- | ----------------------------------- | ----------------------------------------- | --------------- | - |
| `@var(name)`                    | Inserta variables (con escape HTML) | `@var(title)`                             |                 |   |
| `@raw(name)`                    | Inserta sin escape                  | `@raw(html)`                              |                 |   |
| `@foreach(item in list)`        | Itera listas                        | `@foreach(user in users) ... @endforeach` |                 |   |
| `@if(cond)` / `@else`           | Condicional simple                  | `@if(show) Hola @endif`                   |                 |   |
| `@extend("layout.html")`        | Herencia de layout                  | —                                         |                 |   |
| `@section("x") ... @endsection` | Define sección para `@yield("x")`   | —                                         |                 |   |
| `@include("file.html")`         | Incluye otra plantilla              | —                                         |                 |   |
| `Filtros`                       | `@var(name                          | upper)`o`@var(price                       | number:"#.00")` | — |

---

## ⚡ Filtros integrados

| Filtro              | Efecto           | Ejemplo     |                 |
| ------------------- | ---------------- | ----------- | --------------- |
| `upper`             | Mayúsculas       | `@var(title | upper)`         |
| `lower`             | Minúsculas       | `@var(name  | lower)`         |
| `trim`              | Elimina espacios | `@var(text  | trim)`          |
| `number:"#,##0.00"` | Formato numérico | `@var(price | number:"#.00")` |

---

## 🧰 Configuración

```java
new Mopla("src/main/resources/templates")
  .setDevMode(true)      // recarga si el archivo cambia
  .setCacheEnabled(true) // usa cache en producción
  .register("reverse", v -> new StringBuilder(v.toString()).reverse().toString());
```

---

## 🧑‍💻 Contribuir

1. Clona el repo

   ```bash
   git clone https://github.com/ronaldbit/mopla-java
   cd mopla-java
   ```
2. Haz tus cambios (usa Java 17+)
3. Prueba con:

   ```bash
   mvn test
   ```
4. Crea un PR 🚀

---

## 🏷️ Licencia

**MIT License**
Hecho con ❤️ por [@ronaldbit](https://github.com/ronaldbit)

---

## 📦 Publicación en JitPack

Cada vez que crees un **tag** (por ejemplo `v0.1.0`), JitPack genera automáticamente la librería.

Puedes ver el build aquí:
👉 [https://jitpack.io/#ronaldbit/mopla-java](https://jitpack.io/#ronaldbit/mopla-java)

Y usarlo en cualquier proyecto con:

```xml
<dependency>
  <groupId>com.github.ronaldbit</groupId>
  <artifactId>mopla-java</artifactId>
  <version>v0.1.0</version>
</dependency>
```

---

### ✨ Ejemplo rápido

```java
Mopla tpl = new Mopla("templates");
String html = tpl.renderString("<h1>@var(name|upper)</h1>", Map.of("name","Ronald"));
System.out.println(html); // <h1>RONALD</h1>
```
