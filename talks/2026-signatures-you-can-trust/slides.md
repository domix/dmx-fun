---
theme: ../theme
title: From exceptions to signatures you can trust
titleTemplate: '%s · dmx-fun'
info: |
  A 45-minute talk on error handling in Java 25 with dmx-fun.
  English and Spanish from one source: press L to switch, or open with ?lang=es.
canvasWidth: 1920
colorSchema: dark
fonts:
  sans: Inter Variable
  mono: JetBrains Mono
  provider: none
drawings:
  persist: false
transition: fade
blocks:
  - { en: The signature that lies, es: La firma que miente, from: 4, to: 9 }
  - { en: Railway, es: Railway, from: 10, to: 12 }
  - { en: All the errors at once, es: Todos los errores a la vez, from: 13, to: 15 }
  - { en: Out the door, es: Hacia fuera, from: 16, to: 25 }
layout: cover
---

# <span class="gradient-title">From exceptions to<br>signatures you can trust</span>

<Tk k="cover.subtitle" />

<!--
EN: Open with ?lang=es for a Spanish room. Press L at any time to switch languages; D for light mode. Timer starts here: 35 minutes of talk, 10 of questions.
ES: Abrir con ?lang=es para una sala en español. L cambia el idioma en cualquier momento; D pone el modo claro. El cronómetro arranca aquí: 35 minutos de charla, 10 de preguntas.
-->

---
layout: default
---

# <T en="Naming things" es="Ponerle nombre a las cosas" />

<blockquote class="quote">
  <p><Tk k="naming.quote" /></p>
  <footer>Phil Karlton</footer>
</blockquote>

<div class="naming">
  <v-click><span class="word">fun</span></v-click><v-click><span class="verdict err"><Tk k="naming.generic" /></span></v-click>
  <v-click><span class="word">domix</span></v-click><v-click><span class="verdict"><Tk k="naming.nickname" /></span></v-click>
  <v-click><span class="word ok">dmx-fun</span></v-click><v-click><span class="verdict"><Tk k="naming.result" /></span></v-click>
</div>

<v-click>

<Mascot class="closing-mascot" :size="300" />

</v-click>

<!--
EN: [0–1] Thirty seconds, before the real talk starts. Karlton's line, then the confession: when I built this I did not know what to call it. "fun" was the whole idea, and too generic to be found. So the nickname went in front, and that is the name. One of the two hard things solved; the other one is still open. The mascot comes in on the last click; it is the same robot as on the site.
ES: [0–1] Treinta segundos, antes de que empiece la charla de verdad. La frase de Karlton, y luego la confesión: cuando construí esto no sabía cómo llamarlo. "fun" era toda la idea, y demasiado genérico para encontrarlo. Así que el apodo fue delante, y ese es el nombre. Una de las dos cosas difíciles, resuelta; la otra sigue pendiente. La mascota entra en el último clic; es el mismo robot que en el sitio.
-->

---

# <T en="Three gaps I wanted to fill" es="Tres huecos que quería llenar" />

<div class="cols-3">
  <div>
    <span class="num">1</span>
    <h3><Tk k="gaps.lang.title" /></h3>
    <p><Tk k="gaps.lang.body" /></p>
    <p class="fact"><Tk k="gaps.lang.fact" /></p>
  </div>
  <div>
    <span class="num">2</span>
    <h3><Tk k="gaps.docs.title" /></h3>
    <p><Tk k="gaps.docs.body" /></p>
    <p class="fact"><Tk k="gaps.docs.fact" /></p>
  </div>
  <div>
    <span class="num">3</span>
    <h3><Tk k="gaps.eco.title" /></h3>
    <p><Tk k="gaps.eco.body" /></p>
    <p class="fact"><Tk k="gaps.eco.fact" /></p>
  </div>
</div>

<p class="caption"><Tk k="why.vavr" /></p>

<!--
EN: [1–3] The honest part, in the speaker's own order, before any code: why this library exists. One: I wanted a library written in the Java we have now, and the codebase is the proof, not the README. Two: documentation that teaches; the guide, the ten-chapter tutorial this talk follows, the blog, and sample modules that compile and run on every pull request. Three: an ecosystem; the types are only useful if they travel through Spring, Jackson and the observability stack you already have. Vavr and Arrow are mentioned, not attacked: they were right for Java 8 and for Kotlin.
ES: [1–3] La parte franca, en el orden del ponente, antes de cualquier código: por qué existe esta librería. Uno: quería una librería escrita en el Java que tenemos hoy, y la prueba es el código, no el README. Dos: documentación que enseña; la guía, el tutorial de diez capítulos que sigue esta charla, el blog, y módulos de ejemplo que compilan y corren en cada pull request. Tres: un ecosistema; los tipos solo sirven si viajan por Spring, Jackson y la pila de observabilidad que ya tienes. Vavr y Arrow se mencionan, no se atacan: fueron la respuesta correcta para Java 8 y para Kotlin.
-->

---
layout: default
---

# <T en="The signature that lies" es="La firma que miente" />

<div class="hero-code">

```java
User findUser(String id)
```

</div>

<p class="question"><Tk k="hook.question" /></p>

<div class="chips">
  <v-click><span class="chip err"><Tk k="hook.exception" /></span></v-click>
  <v-click><span class="chip err"><Tk k="hook.null" /></span></v-click>
  <v-click><span class="chip err"><Tk k="hook.optional" /></span></v-click>
</div>

<v-click>

<p class="punchline"><Tk k="hook.answer" /></p>

</v-click>

<!--
EN: [3–5] Ask the room before clicking. Someone always says "it throws", someone else "it returns null"; each answer gets its chip. Then the anecdote: the production incident where a caller handled the exception nobody documented, or trusted the Optional that was never empty until it was. One real story, two sentences, no statistics. The last click is the point: none of the three is in the signature.
ES: [3–5] Preguntar a la sala antes de hacer clic. Alguien siempre dice "lanza", otro "devuelve null"; cada respuesta recibe su chip. Luego la anécdota: el incidente en producción donde un caller manejó la excepción que nadie documentó, o confió en el Optional que nunca venía vacío hasta que vino. Una historia real, dos frases, sin estadísticas. El último clic es el punto: ninguna de las tres está en la firma.
-->

---
class: dense
---

# <T en="Chapter 0: twenty lines, four lies" es="Capítulo 0: veinte líneas, cuatro mentiras" />

<Marks :at="[4, 11, 14, 19]" dense>

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter00/EnrollmentService.java#enroll java {1|2-5|10-12|13-15|16-20|all}

</Marks>

<!--
EN: [5–10 starts] Same enroll as tutorial chapter 0. First click: the signature promises an Enrollment. Then click by click: exception, exception, null, exception, wrapped cause. Four failure modes, zero in the signature. Nothing here is bad Java; it is the Java most of us write.
ES: [empieza 5–10] El mismo enroll del capítulo 0 del tutorial. Primer clic: la firma promete un Enrollment. Luego clic a clic: excepción, excepción, null, excepción, causa envuelta. Cuatro formas de fallar, cero en la firma. Nada de esto es mal Java; es el Java que casi todos escribimos.
-->

---
class: dense
---

# <T en="What every caller has to remember" es="Lo que cada caller tiene que recordar" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter00/EnrollmentMessages.java#describe java {4-6|8-9|10-11|12-14|all}

<p class="caption err"><Tk k="caller.caption" /></p>

<!--
EN: The other side of chapter 0: the caller. Click 1: the null check you only write if you knew null meant "already enrolled". Click 2: two exceptions you only catch if you read the body. Click 3: a third one. Click 4: and a bare RuntimeException for the payment, so the catch swallows anything else too. This is the argument against exceptions in one screen: they are not values. The type says nothing about them, you cannot map or chain them, and every layer either re-declares them or wraps them. Checked exceptions fix the first problem and make the other two worse; the long answer is in the questions slide.
ES: El otro lado del capítulo 0: el caller. Clic 1: el null check que solo escribes si sabías que null significaba "ya inscrito". Clic 2: dos excepciones que solo atrapas si leíste el cuerpo. Clic 3: una tercera. Clic 4: y un RuntimeException pelado para el pago, así que el catch se traga cualquier otra cosa también. Este es el argumento contra las excepciones en una pantalla: no son valores. El tipo no dice nada de ellas, no puedes hacer map ni encadenarlas, y cada capa las vuelve a declarar o las envuelve. Las checked arreglan el primer problema y empeoran los otros dos; la respuesta larga está en la slide de preguntas.
-->

---

# <T en="First, name the failures" es="Primero, nombrar los fallos" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter01/EnrollmentError.java#error java

<p class="caption"><Tk k="error.caption" /></p>

<!--
EN: A sealed interface, one record per case. Not strings, not a Throwable: data a caller can act on. "CourseFull carries the capacity because the UI shows it." The compiler now knows the full list; that is what makes the next two slides possible.
ES: Una interfaz sellada, un record por caso. Ni strings ni un Throwable: datos sobre los que el caller puede actuar. "CourseFull lleva la capacidad porque la UI la muestra." El compilador ya conoce la lista completa; eso es lo que hace posibles las dos slides siguientes.
-->

---

# <T en="Return instead of throw" es="Devolver en vez de lanzar" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter01/EnrollmentService.java#signature java {1|2-5|all}

<p class="caption"><Tk k="signature.caption" /></p>

<!--
EN: Read the signature aloud: either an Enrollment, or one of those five errors. Nothing else can come out. The body still has early returns; chapter 2 cleans that up. Result is a sealed interface too: Ok or Err, both records.
ES: Leer la firma en voz alta: o un Enrollment, o uno de esos cinco errores. No puede salir nada más. El cuerpo sigue teniendo returns tempranos; el capítulo 2 lo limpia. Result también es una interfaz sellada: Ok o Err, ambos records.
-->

---

# <T en="Every case, or it does not compile" es="Todos los casos, o no compila" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter01/EnrollmentMessages.java#describe java

<p class="caption"><Tk k="switch.caption" /></p>

<!--
EN: This is the payoff slide of the first block. Record patterns pull the data out in the case label. Ask: "what happens when a colleague adds a sixth error next quarter?" Answer: every switch in the codebase stops compiling and tells them where to look. The compiler does the stack trace's job, before deploy.
ES: Esta es la slide de recompensa del primer bloque. Los record patterns sacan los datos en el propio case. Preguntar: "¿qué pasa cuando un colega añade un sexto error el trimestre que viene?" Respuesta: todos los switch del código dejan de compilar y le dicen dónde mirar. El compilador hace el trabajo del stack trace, antes del deploy.
-->

---
layout: section
---

# <T en="Two tracks" es="Railway" />

<p><T en="Five steps, one chain, no nested switches." es="Cinco pasos, una cadena, sin switch anidados." /></p>

<!--
EN: [10–17] Transition: chapter 1 made the signature truthful but the body is still a staircase of ifs. If every step returns a Result, how do we chain five of them without checking isOk() five times?
ES: [10–17] Transición: el capítulo 1 hizo que la firma dijera la verdad, pero el cuerpo sigue siendo una escalera de ifs. Si cada paso devuelve un Result, ¿cómo encadenamos cinco sin comprobar isOk() cinco veces?
-->

---

# <T en="One chain, first failure wins" es="Una cadena, el primer fallo gana" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter02/EnrollmentService.java#enroll java {2|3-6|7|8-11|all}

<p class="caption"><Tk k="tracks.caption" /></p>

<!--
EN: Walk the chain with the clicks. Click 1: findStudent returns Result<Student, EnrollmentError>. Click 2: four flatMaps, each a small private method returning a Result; a failure anywhere skips the rest. Click 3: map, because save cannot fail. Click 4: recoverWith turns one specific failure back into a success; enrolling twice is idempotent, not an error. Each private method is five lines and tested on its own.
ES: Recorrer la cadena con los clics. Clic 1: findStudent devuelve Result<Student, EnrollmentError>. Clic 2: cuatro flatMap, cada uno un método privado pequeño que devuelve un Result; un fallo en cualquier punto se salta el resto. Clic 3: map, porque save no puede fallar. Clic 4: recoverWith convierte un fallo concreto de vuelta en éxito; inscribirse dos veces es idempotente, no un error. Cada método privado tiene cinco líneas y su propio test.
-->

---
clicks: 2
---

# <T en="The railway, on one slide" es="El railway, en una slide" />

<div :class="'stage-' + $clicks">
  <Diagram name="two-tracks" />
</div>

<p class="caption"><Tk k="railway.caption" /></p>

<!--
EN: The metaphor comes after the code on purpose: now it names something they have just read. Blue is the Ok track, amber the Err track; those two colors mean the same thing on every slide of this deck. Click 1: findCourse fails, CourseNotFound switches down and rides the lower rail; the three stations after it dim because nobody runs them. Click 2: a different run, AlreadyEnrolled at ensureNotEnrolled, skips charge and save, and recoverWith lifts it back onto the Ok rail at the end: enrolling twice is idempotent. Nested switches are what you write when you do not have the lower track.
ES: La metáfora viene después del código a propósito: ahora nombra algo que acaban de leer. Azul es la vía Ok, ámbar la vía Err; esos dos colores significan lo mismo en todas las slides de este deck. Clic 1: findCourse falla, CourseNotFound baja y viaja por la vía inferior; las tres estaciones siguientes se apagan porque nadie las ejecuta. Clic 2: otra ejecución, AlreadyEnrolled en ensureNotEnrolled, se salta charge y save, y recoverWith lo devuelve a la vía Ok al final: inscribirse dos veces es idempotente. Los switch anidados son lo que escribes cuando no tienes la vía de abajo.
-->

---

# <T en="All the errors at once" es="Todos los errores a la vez" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter05/FormValidation.java#byHand java {2-10|12-14|all}

<p class="caption"><Tk k="byhand.caption" /></p>

<!--
EN: [17–23] The block that lands hardest with backend developers. flatMap stops at the first failure; a sign-up form must not. Click 1: one Validated per field. Click 2: combine merges them, concatenating the error lists. A user fixing a form wants all three messages, not one per round trip. This is where Result and Validated differ, and why both exist.
ES: [17–23] El bloque que más cala en desarrolladores backend. flatMap se detiene en el primer fallo; un formulario de alta no debe hacerlo. Clic 1: un Validated por campo. Clic 2: combine los fusiona concatenando las listas de errores. Quien corrige un formulario quiere los tres mensajes, no uno por viaje. Aquí es donde Result y Validated difieren, y por eso existen los dos.
-->

---

# <T en="Rules as values" es="Reglas como valores" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter05/FormValidation.java#guards java {1-4|6-11|13-15|all}

<p class="caption"><Tk k="guards.caption" /></p>

<!--
EN: Same validation, said differently. Click 1: three rules, each a Guard: a predicate and a message. ofCatching turns a throwing parse into a failed rule. Click 2: contramap points a String rule at a form field and prefixes the field name; allOf composes them. Click 3: check runs all of them and returns the Validated from the previous slide. The rules are constants: reused in the API, in the batch importer, in the tests.
ES: La misma validación, dicha de otra forma. Clic 1: tres reglas, cada una un Guard: un predicado y un mensaje. ofCatching convierte un parse que lanza en una regla fallida. Clic 2: contramap apunta una regla de String a un campo del formulario y antepone el nombre del campo; allOf las compone. Clic 3: check las ejecuta todas y devuelve el Validated de la slide anterior. Las reglas son constantes: se reutilizan en la API, en el importador por lotes, en los tests.
-->

---

# <T en="Back on the tracks" es="De vuelta al railway" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter05/EnrollmentService.java#validate java

<p class="caption"><Tk k="bridge.caption" /></p>

<!--
EN: The seam. toResult turns a Validated into a Result; mapError wraps the list of messages into the sixth EnrollmentError case, InvalidForm. From here on the pipeline from chapter 2 runs unchanged. Every type in the library converts to its neighbours, and the conversion is where you decide which failures become which.
ES: La costura. toResult convierte el Validated en un Result; mapError envuelve la lista de mensajes en el sexto caso de EnrollmentError, InvalidForm. A partir de aquí el pipeline del capítulo 2 corre sin cambios. Todos los tipos de la librería se convierten a sus vecinos, y la conversión es donde decides qué fallos se vuelven qué.
-->

---

# <T en="Integrations, as words" es="Integraciones, en palabras" />

<div class="cards">
  <div class="card">
    <h3><Tk k="integrations.json.title" /></h3>
    <p class="mods"><code>fun-jackson3</code> <code>fun-jackson2</code></p>
    <p><Tk k="integrations.json.body" /></p>
  </div>
  <div class="card">
    <h3><Tk k="integrations.web.title" /></h3>
    <p class="mods"><code>fun-spring-boot</code> <code>fun-spring</code> <code>fun-spring-webflux</code> <code>fun-reactor</code> <code>fun-quarkus</code></p>
    <p><Tk k="integrations.web.body" /></p>
  </div>
  <div class="card">
    <h3><Tk k="integrations.obs.title" /></h3>
    <p class="mods"><code>fun-micrometer</code> <code>fun-observation</code> <code>fun-tracing</code> <code>fun-resilience4j</code></p>
    <p><Tk k="integrations.obs.body" /></p>
  </div>
  <div class="card">
    <h3><Tk k="integrations.test.title" /></h3>
    <p class="mods"><code>fun-jakarta-validation</code> <code>fun-jakarta-jaxb</code> <code>fun-assertj</code></p>
    <p><Tk k="integrations.test.body" /></p>
  </div>
</div>

<p class="caption"><Tk k="integrations.caption" /></p>

<!--
EN: [23–25] No logos. Each card is a separate set of artifacts under codes.domix; the BOM aligns versions. The point of the slide is the shape, not the list: the core knows nothing about any of these, and each one is small enough to read in an afternoon, which matters for the "contribute" slide.
ES: [23–25] Sin logos. Cada tarjeta es un conjunto de artefactos separados bajo codes.domix; el BOM alinea versiones. El sentido de la slide es la forma, no la lista: el núcleo no sabe nada de ninguno de estos, y cada uno es lo bastante pequeño para leerse en una tarde, lo que importa para la slide de "contribuir".
-->

---
class: dense
---

# <T en="Out the door: one place decides the status" es="Hacia fuera: un solo lugar decide el status" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter09/HttpMapping.java#statusFor java {3-4|5|6-9|all}

<p class="caption"><Tk k="status.caption" /></p>

<!--
EN: [25–30 begins] Tutorial chapter 9. Everything the pipeline can produce, and the HTTP status for each, in one exhaustive switch. Click 1: the two successes, 201 and 202, because the waiting list is a legitimate answer. Click 2: a bad form is 422. Click 3: not found, conflict, and 502 when the payment provider failed. No framework in this file.
ES: [empieza 25–30] Capítulo 9 del tutorial. Todo lo que el pipeline puede producir, y el status HTTP de cada caso, en un solo switch exhaustivo. Clic 1: los dos éxitos, 201 y 202, porque la lista de espera es una respuesta legítima. Clic 2: un formulario inválido es 422. Clic 3: no encontrado, conflicto, y 502 cuando falló el proveedor de pagos. No hay framework en este archivo.
-->

---

# <T en="Failures with names count themselves" es="Los fallos con nombre se cuentan solos" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter09/EnrollmentMetrics.java#record java {1-5|6|all}

<div class="mt-6">
  <Diagram name="outcomes" />
</div>

<p class="caption"><Tk k="metrics.caption" /></p>

<!--
EN: One Micrometer counter, tagged by what happened. The bars are what the dashboard shows the day you deploy this: the two legitimate outcomes in blue, the named failures in amber, no log parsing in between. Because failures are records, the tag is the simple class name; nobody agreed on a log format. The dashboard shows PaymentFailed next to Waitlisted the day you deploy this.
ES: Un contador de Micrometer, etiquetado por lo que pasó. Las barras son lo que muestra el dashboard el día que despliegas esto: los dos resultados legítimos en azul, los fallos con nombre en ámbar, sin parsear logs en medio. Como los fallos son records, la etiqueta es el nombre simple de la clase; nadie tuvo que acordar un formato de log. El dashboard muestra PaymentFailed junto a Waitlisted el día que despliegas esto.
-->

---

# <T en="The controller, as written" es="El controller, tal cual" />

<<< @/snippets/samples/spring-boot-sample/src/main/java/dmx/fun/sample/item/ItemController.java#endpoints java {1-4|6-9|11-14|all}

<p class="caption"><Tk k="demo.intro" /></p>

<!--
EN: The real file from spring-boot-sample, nothing elided. Click 1: Option<Item>, and the auto-configured handler turns none() into a 404. Click 2: Result<Item, String>; ok is the body, err is the status. Click 3: Result<NonEmptyList<Item>, String>, where the handler unwraps and Jackson writes the list as a plain array. No ResponseEntity, no @ExceptionHandler, no JSON code anywhere in the controller.
ES: El archivo real de spring-boot-sample, sin recortes. Clic 1: Option<Item>, y el handler autoconfigurado convierte none() en un 404. Clic 2: Result<Item, String>; ok es el cuerpo, err es el status. Clic 3: Result<NonEmptyList<Item>, String>, donde el handler desenvuelve y Jackson escribe la lista como un array normal. Sin ResponseEntity, sin @ExceptionHandler, sin código JSON en ningún sitio del controller.
-->

---

# <T en="Demo: a controller that returns the Result" es="Demo: un controller que devuelve el Result" />

| | | |
|---|---|---|
| `POST /items` `{"name":"Widget"}` | **<span class="ok">200</span>** | `Result.ok(item)` |
| `POST /items` `{"name":""}` | **<span class="err">500</span>** `name must not be blank` | `Result.err(message)` |
| `GET /items/-1` | **<span class="err">404</span>** | `Option.none()` |
| `GET /items/search?name=Widget` | **<span class="ok">200</span>** `[…]` | `Result<NonEmptyList<Item>, String>` |

<p class="caption"><Tk k="demo.fallback" /></p>

<!--
EN: Switch to the terminal; demo.md has the four commands. The sample is spring-boot-sample on Boot 4: fun-spring-boot auto-configures the return value handlers and registers the Jackson 3 module; the controller has no ResponseEntity and no @ExceptionHandler. The fourth request is the one that proves Jackson: NonEmptyList comes out as a plain array. Fallback: the asciinema cast in this directory.
ES: Cambiar a la terminal; demo.md tiene los cuatro comandos. El sample es spring-boot-sample sobre Boot 4: fun-spring-boot autoconfigura los return value handlers y registra el módulo de Jackson 3; el controller no tiene ResponseEntity ni @ExceptionHandler. La cuarta petición es la que prueba Jackson: NonEmptyList sale como un array normal. Respaldo: la grabación de asciinema en este directorio.
-->

---

# <T en="The demo, recorded" es="La demo, grabada" />

<Cast />

<p class="caption"><Tk k="cast.caption" /></p>

<!--
EN: Only if the live demo cannot run. Space plays and pauses; the arrows seek. Same four requests, same output, recorded with scripts/record-demo.sh against the sample on Boot 4. Narrate over it exactly as you would over the terminal.
ES: Solo si la demo en vivo no puede correr. Espacio reproduce y pausa; las flechas buscan. Las mismas cuatro peticiones, la misma salida, grabadas con scripts/record-demo.sh contra el sample en Boot 4. Narrar encima igual que sobre la terminal.
-->

---

# <T en="Start at one boundary" es="Empieza en una frontera" />

<div class="steps">
  <div><span class="num">1</span><p><T en="One dependency: codes.domix:fun, or the BOM if you take more than one module." es="Una dependencia: codes.domix:fun, o el BOM si tomas más de un módulo." /></p></div>
  <div><span class="num">2</span><p><T en="One endpoint returns Result. The service behind it does not change yet." es="Un endpoint devuelve Result. El servicio detrás todavía no cambia." /></p></div>
  <div><span class="num">3</span><p><T en="One sealed error type, cases carved by what the caller can do about them." es="Un tipo de error sellado, con los casos recortados por lo que el caller puede hacer con ellos." /></p></div>
</div>

<p class="caption"><Tk k="adopt.caption" /></p>

<!--
EN: [30–32] Never big-bang. The library is designed to be adopted one seam at a time: Option.fromOptional, Try.of around the code that throws, getOrElse on the way out. The blog post "introducing FP into a legacy codebase" is the long version.
ES: [30–32] Nunca big-bang. La librería está diseñada para adoptarse costura a costura: Option.fromOptional, Try.of alrededor del código que lanza, getOrElse al salir. El post "introducing FP into a legacy codebase" es la versión larga.
-->

---

# <T en="Open source: four ways in" es="Open source: cuatro formas de entrar" />

<p class="lead"><Tk k="contribute.open" /></p>

| | |
|---|---|
| **1** | <Tk k="contribute.try" /> |
| **2** | <Tk k="contribute.gfi" /> |
| **3** | <Tk k="contribute.integrations" /> |
| **4** | <Tk k="contribute.posts" /> |

<p class="caption"><Tk k="contribute.gfi.where" /> <a href="https://github.com/domix/dmx-fun/labels/good%20first%20issue" target="_blank">github.com/domix/dmx-fun/labels/good first issue</a></p>

<!--
EN: [32–33] Say it plainly first: Apache 2.0, the repository is public, the ADRs are public, and anyone can open an issue. Then the ask has to be specific. First one is the most valuable: design feedback on the error type people actually built. Second: say how many good-first-issues are open (eight when this deck was written, #575 to #582: a README row, NonEmptySet/NonEmptyMap in both Jackson lines, a JAXB adapter, three AssertJ classes, site tokens, Dependabot for talks/) and describe one in a sentence; each names the files to touch and what done looks like. Check the label the day before. Third: each integration module is a template for the next. Fourth: real adoption stories, on the site blog, with their name on it.
ES: [32–33] Decirlo claro primero: Apache 2.0, el repositorio es público, los ADRs son públicos, y cualquiera puede abrir un issue. Luego la petición tiene que ser concreta. La primera es la más valiosa: feedback de diseño sobre el tipo de error que la gente realmente construyó. Segunda: decir cuántos good first issue hay abiertos (ocho al escribir este deck, #575 a #582: una fila del README, NonEmptySet/NonEmptyMap en las dos líneas de Jackson, un adaptador JAXB, tres clases de AssertJ, tokens del sitio, Dependabot para talks/) y describir uno en una frase; cada uno dice qué archivos tocar y cómo se ve terminado. Revisar la etiqueta el día anterior. Tercera: cada módulo de integración es la plantilla del siguiente. Cuarta: historias reales de adopción, en el blog del sitio, con su nombre.
-->

---

# <T en="One method, five rewrites" es="Un método, cinco reescrituras" />

<div class="rewrites">
  <v-click><div class="row"><code><span class="err">@Nullable Enrollment</span> enroll(String studentId, String courseId)</code><span class="label"><T en="Chapter 0: four lies, none in the signature" es="Capítulo 0: cuatro mentiras, ninguna en la firma" /></span></div></v-click>
  <v-click><div class="row"><code><span class="ok">Result&lt;Enrollment, EnrollmentError&gt;</span> enroll(String studentId, String courseId)</code><span class="label"><T en="Chapter 1: the signature tells the truth" es="Capítulo 1: la firma dice la verdad" /></span></div></v-click>
  <v-click><div class="row"><code>findStudent(id)<span class="ok">.flatMap(…).flatMap(…).map(…).recoverWith(…)</span></code><span class="label"><T en="Chapter 2: one chain, first failure wins" es="Capítulo 2: una cadena, el primer fallo gana" /></span></div></v-click>
  <v-click><div class="row"><code>Result&lt;Enrollment, EnrollmentError&gt; enroll(<span class="ok">EnrollmentForm form</span>)</code><span class="label"><T en="Chapter 5: every form error at once" es="Capítulo 5: todos los errores del formulario a la vez" /></span></div></v-click>
  <v-click><div class="row"><code>Result&lt;<span class="ok">Either&lt;Waitlisted, Enrolled&gt;</span>, EnrollmentError&gt; enroll(EnrollmentForm form)</code><span class="label"><T en="Chapter 6: two outcomes, no villain" es="Capítulo 6: dos resultados, ningún villano" /></span></div></v-click>
</div>

<v-click>

<p class="caption"><Tk k="recap.caption" /></p>

</v-click>

<!--
EN: [33–35] The whole talk in five lines; one click each. The signatures are quoted from the tutorial chapters, and the coloured part is what each rewrite changed. Chapter 6 is the one you have not seen: a full course becomes a waiting list, a second legitimate answer on the Ok track; the status slide already used it. Then the last click: none of these was a big-bang, every one compiled on its own.
ES: [33–35] Toda la charla en cinco líneas; un clic cada una. Las firmas están citadas de los capítulos del tutorial, y la parte coloreada es lo que cambió cada reescritura. El capítulo 6 es el que no han visto: un curso lleno se convierte en lista de espera, una segunda respuesta legítima en la vía Ok; la slide de los status ya lo usó. Y el último clic: ninguna fue un big-bang, cada una compiló por sí sola.
-->

---
layout: section
---

# <T en="Try it on one endpoint" es="Pruébala en un solo endpoint" />

<div class="hero-code">

```java
Result<User, UserError> findUser(UserId id)
```

</div>

<p><Tk k="closing.ask" /></p>

<div class="cols closing-cols mt-6">
  <Qr name="tutorial" url="domix.github.io/dmx-fun/guide/tutorial"><Tk k="closing.tutorial" /></Qr>
  <Qr name="repo" url="github.com/domix/dmx-fun"><Tk k="closing.repo" /></Qr>
</div>

<p class="handle"><Tk k="closing.speaker" /></p>

<Mascot class="closing-mascot" :size="240" />

<!--
EN: Leave this slide up during questions. The signature is the one from the hook, forty minutes later, now telling the truth: that is the whole talk in one line. Two sentences about the project and the speaker go here, at the end: who maintains it, since when, and that every decision is an ADR anyone can argue with in an issue.
ES: Dejar esta slide durante las preguntas. La firma es la del gancho, cuarenta minutos después, ahora diciendo la verdad: esa línea es toda la charla. Las dos frases sobre el proyecto y el ponente van aquí, al final: quién lo mantiene, desde cuándo, y que cada decisión es un ADR que cualquiera puede discutir en un issue.
-->

---

# <T en="Three questions that always come" es="Tres preguntas que siempre llegan" />

<div class="qa">
  <v-click><div class="row"><h3><Tk k="qa.immutability.q" /></h3><p><Tk k="qa.immutability.a" /></p></div></v-click>
  <v-click><div class="row"><h3><Tk k="qa.checked.q" /></h3><p><Tk k="qa.checked.a" /></p></div></v-click>
  <v-click><div class="row"><h3><Tk k="qa.kotlin.q" /></h3><p><Tk k="qa.kotlin.a" /></p></div></v-click>
</div>

<p class="caption"><Tk k="qa.caption" /> <a href="https://domix.github.io/dmx-fun/blog/performance-cost-of-immutability-when-it-matters" target="_blank">immutability</a> · <a href="https://domix.github.io/dmx-fun/blog/why-just-use-exceptions-persists" target="_blank">exceptions</a> · <a href="https://domix.github.io/dmx-fun/blog/vavr-arrow-cats-fp-ts-what-problems-they-solve" target="_blank">Vavr, Arrow</a></p>

<!--
EN: [35–45] Show only if one of these is asked, or if the room is quiet. Immutability: records are small, escape analysis removes most short-lived allocations, and Valhalla is coming; the real costs are in large collections copied in loops, which this library does not ask you to do. Checked exceptions: they do solve the "visible in the signature" problem, but they do not compose: no lambdas, no map, no chaining, and every layer re-declares or wraps them. Kotlin: Arrow is the right answer in Kotlin; the question here is what to do in the Java you already have.
ES: [35–45] Mostrar solo si preguntan una de estas, o si la sala está callada. Inmutabilidad: los records son pequeños, el escape analysis elimina casi todas las asignaciones de vida corta, y Valhalla viene en camino; los costes reales están en colecciones grandes copiadas en bucles, que esta librería no te pide hacer. Excepciones checked: sí resuelven lo de "visible en la firma", pero no componen: sin lambdas, sin map, sin encadenar, y cada capa las vuelve a declarar o las envuelve. Kotlin: Arrow es la respuesta correcta en Kotlin; la pregunta aquí es qué hacer con el Java que ya tienes.
-->
