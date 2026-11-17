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

# <T en="The signature that lies" es="La firma que miente" />

```java
User findUser(String id)
```

<p class="mt-10"><Tk k="hook.question" /></p>

<v-click>

<p><Tk k="hook.answer" /></p>

</v-click>

<!--
EN: [0–4] Ask the room before clicking. Someone always says "it throws", someone else "it returns null". Then the anecdote: the production incident where a caller handled the exception nobody documented, or trusted the Optional that was never empty until it was. One real story, two sentences, no statistics.
ES: [0–4] Preguntar a la sala antes de hacer clic. Alguien siempre dice "lanza", otro "devuelve null". Luego la anécdota: el incidente en producción donde un caller manejó la excepción que nadie documentó, o confió en el Optional que nunca venía vacío hasta que vino. Una historia real, dos frases, sin estadísticas.
-->

---

# <T en="Chapter 0: twenty lines, four lies" es="Capítulo 0: veinte líneas, cuatro mentiras" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter00/EnrollmentService.java#enroll java {1|2-5|10-12|13-15|16-20|all}

<!--
EN: [4–10 starts] Same enroll as tutorial chapter 0. First click: the signature promises an Enrollment. Then click by click: exception, exception, null, exception, wrapped cause. Four failure modes, zero in the signature. Nothing here is bad Java; it is the Java most of us write.
ES: [empieza 4–10] El mismo enroll del capítulo 0 del tutorial. Primer clic: la firma promete un Enrollment. Luego clic a clic: excepción, excepción, null, excepción, causa envuelta. Cuatro formas de fallar, cero en la firma. Nada de esto es mal Java; es el Java que casi todos escribimos.
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

# <T en="Two tracks" es="Dos vías" />

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

# <T en="The railway, on one slide" es="La vía del tren, en una slide" />

<Diagram name="two-tracks" />

<p class="caption"><Tk k="railway.caption" /></p>

<!--
EN: The metaphor comes after the code on purpose: now it names something they have just read. Blue is the success track, amber the failure track; those two colors mean the same thing on every slide of this deck. Nested switches are what you write when you do not have the lower track.
ES: La metáfora viene después del código a propósito: ahora nombra algo que acaban de leer. Azul es la vía del éxito, ámbar la del fallo; esos dos colores significan lo mismo en todas las slides de este deck. Los switch anidados son lo que escribes cuando no tienes la vía de abajo.
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

# <T en="Back on the tracks" es="De vuelta a las vías" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter05/EnrollmentService.java#validate java

<p class="caption"><Tk k="bridge.caption" /></p>

<!--
EN: The seam. toResult turns a Validated into a Result; mapError wraps the list of messages into the sixth EnrollmentError case, InvalidForm. From here on the pipeline from chapter 2 runs unchanged. Every type in the library converts to its neighbours, and the conversion is where you decide which failures become which.
ES: La costura. toResult convierte el Validated en un Result; mapError envuelve la lista de mensajes en el sexto caso de EnrollmentError, InvalidForm. A partir de aquí el pipeline del capítulo 2 corre sin cambios. Todos los tipos de la librería se convierten a sus vecinos, y la conversión es donde decides qué fallos se vuelven qué.
-->

---

# <T en="Why this library" es="Por qué esta librería" />

<p><Tk k="why.intro" /></p>

| | |
|---|---|
| <T en="Java 25 baseline" es="Base Java 25" /> | <T en="sealed interfaces, records, pattern matching; no class hierarchy to learn" es="interfaces selladas, records, pattern matching; sin jerarquía de clases que aprender" /> |
| <T en="Core with one dependency" es="Núcleo con una dependencia" /> | <T en="JSpecify annotations; a JPMS module from day one" es="anotaciones JSpecify; módulo JPMS desde el primer día" /> |
| <T en="Integrations as peers" es="Integraciones como peers" /> | <T en="optional modules, framework version is yours; ADR-022" es="módulos opcionales, la versión del framework es tuya; ADR-022" /> |
| <T en="Tested matrices" es="Matrices probadas" /> | <T en="Jackson 2.14 to 2.23, Jackson 3.0 to 3.3, Spring Boot 3.3 to 4.1, on every pull request" es="Jackson 2.14 a 2.23, Jackson 3.0 a 3.3, Spring Boot 3.3 a 4.1, en cada pull request" /> |
| <T en="Decisions on record" es="Decisiones por escrito" /> | <T en="24 public ADRs, from the Java version to the Jackson 2 and 3 policy" es="24 ADRs públicos, desde la versión de Java hasta la política de Jackson 2 y 3" /> |

<p class="caption"><Tk k="why.vavr" /></p>

<!--
EN: [23–27] Factual tone, two slides. No hierarchy to learn: Result is sealed, Ok and Err are records, you already know how to switch on them. Vavr and Arrow are mentioned, not attacked: they were right for Java 8 and for Kotlin. The ADRs are the honest part: every shape here has a written reason, and some of them say "we are not sure yet".
ES: [23–27] Tono factual, dos slides. Sin jerarquía que aprender: Result es sellado, Ok y Err son records, ya sabes hacer switch sobre ellos. Vavr y Arrow se mencionan, no se atacan: fueron la respuesta correcta para Java 8 y para Kotlin. Los ADRs son la parte franca: cada forma aquí tiene un porqué escrito, y algunos dicen "todavía no estamos seguros".
-->

---

# <T en="Integrations, as words" es="Integraciones, en palabras" />

| | |
|---|---|
| `fun-jackson3` · `fun-jackson2` | <T en="JSON for every type, one module per Jackson line" es="JSON para todos los tipos, un módulo por línea de Jackson" /> |
| `fun-spring-boot` · `fun-spring` | <T en="controllers return Result and Option; transactions roll back on Err" es="los controllers devuelven Result y Option; las transacciones hacen rollback en Err" /> |
| `fun-spring-webflux` · `fun-reactor` | <T en="Mono and Flux in and out" es="Mono y Flux de ida y vuelta" /> |
| `fun-quarkus` | <T en="the same transactions, on CDI" es="las mismas transacciones, sobre CDI" /> |
| `fun-micrometer` · `fun-observation` · `fun-tracing` | <T en="counters, timers and spans tagged by outcome" es="contadores, timers y spans etiquetados por resultado" /> |
| `fun-resilience4j` | <T en="retry and circuit breaker that return Try instead of throwing" es="retry y circuit breaker que devuelven Try en vez de lanzar" /> |
| `fun-jakarta-validation` · `fun-jakarta-jaxb` | <T en="Bean Validation as Validated; JSON-B and JAXB adapters" es="Bean Validation como Validated; adaptadores JSON-B y JAXB" /> |
| `fun-assertj` | <T en="assertions that read like the types" es="aserciones que se leen como los tipos" /> |

<p class="caption"><Tk k="integrations.caption" /></p>

<!--
EN: No logos. Each row is a separate artifact under codes.domix; the BOM aligns versions. The point of the slide is the shape, not the list: the core knows nothing about any of these, and each one is small enough to read in an afternoon, which matters for the "contribute" slide.
ES: Sin logos. Cada fila es un artefacto separado bajo codes.domix; el BOM alinea versiones. El sentido de la slide es la forma, no la lista: el núcleo no sabe nada de ninguno de estos, y cada uno es lo bastante pequeño para leerse en una tarde, lo que importa para la slide de "contribuir".
-->

---

# <T en="Out the door: one place decides the status" es="Hacia fuera: un solo lugar decide el status" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter09/HttpMapping.java#statusFor java {3-4|5|6-9|all}

<p class="caption"><Tk k="status.caption" /></p>

<!--
EN: [27–32 begins] Tutorial chapter 9. Everything the pipeline can produce, and the HTTP status for each, in one exhaustive switch. Click 1: the two successes, 201 and 202, because the waiting list is a legitimate answer. Click 2: a bad form is 422. Click 3: not found, conflict, and 502 when the payment provider failed. No framework in this file.
ES: [empieza 27–32] Capítulo 9 del tutorial. Todo lo que el pipeline puede producir, y el status HTTP de cada caso, en un solo switch exhaustivo. Clic 1: los dos éxitos, 201 y 202, porque la lista de espera es una respuesta legítima. Clic 2: un formulario inválido es 422. Clic 3: no encontrado, conflicto, y 502 cuando falló el proveedor de pagos. No hay framework en este archivo.
-->

---

# <T en="Failures with names count themselves" es="Los fallos con nombre se cuentan solos" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter09/EnrollmentMetrics.java#record java {1-5|6|all}

<p class="caption"><Tk k="metrics.caption" /></p>

<!--
EN: One Micrometer counter, tagged by what happened. Because failures are records, the tag is the simple class name; nobody agreed on a log format. The dashboard shows PaymentFailed next to Waitlisted the day you deploy this.
ES: Un contador de Micrometer, etiquetado por lo que pasó. Como los fallos son records, la etiqueta es el nombre simple de la clase; nadie tuvo que acordar un formato de log. El dashboard muestra PaymentFailed junto a Waitlisted el día que despliegas esto.
-->

---

# <T en="Demo: a controller that returns the Result" es="Demo: un controller que devuelve el Result" />

<p><Tk k="demo.intro" /></p>

| | | |
|---|---|---|
| `POST /items` `{"name":"Widget"}` | **200** | `Result.ok(item)` |
| `POST /items` `{"name":""}` | **500** `name must not be blank` | `Result.err(message)` |
| `GET /items/-1` | **404** | `Option.none()` |
| `GET /items/search?name=Widget` | **200** `[…]` | `Result<NonEmptyList<Item>, String>` |

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

| | |
|---|---|
| **1** | <T en="One dependency: codes.domix:fun, or the BOM if you take more than one module." es="Una dependencia: codes.domix:fun, o el BOM si tomas más de un módulo." /> |
| **2** | <T en="One endpoint returns Result. The service behind it does not change yet." es="Un endpoint devuelve Result. El servicio detrás todavía no cambia." /> |
| **3** | <T en="One sealed error type, cases carved by what the caller can do about them." es="Un tipo de error sellado, con los casos recortados por lo que el caller puede hacer con ellos." /> |

<p class="caption"><Tk k="adopt.caption" /></p>

<!--
EN: [32–35] Never big-bang. The library is designed to be adopted one seam at a time: Option.fromOptional, Try.of around the code that throws, getOrElse on the way out. The blog post "introducing FP into a legacy codebase" is the long version.
ES: [32–35] Nunca big-bang. La librería está diseñada para adoptarse costura a costura: Option.fromOptional, Try.of alrededor del código que lanza, getOrElse al salir. El post "introducing FP into a legacy codebase" es la versión larga.
-->

---

# <T en="Four ways to contribute" es="Cuatro formas de contribuir" />

| | |
|---|---|
| **1** | <Tk k="contribute.try" /> |
| **2** | <Tk k="contribute.gfi" /> |
| **3** | <Tk k="contribute.integrations" /> |
| **4** | <Tk k="contribute.posts" /> |

<p class="caption"><Tk k="contribute.gfi.where" /> <a href="https://github.com/domix/dmx-fun/labels/good%20first%20issue" target="_blank">github.com/domix/dmx-fun/labels/good first issue</a></p>

<!--
EN: The ask has to be specific. First one is the most valuable: design feedback on the error type people actually built. Second: say how many good-first-issues are open (eight when this deck was written, #575 to #582: a README row, NonEmptySet/NonEmptyMap in both Jackson lines, a JAXB adapter, three AssertJ classes, site tokens, Dependabot for talks/) and describe one in a sentence; each names the files to touch and what done looks like. Check the label the day before. Third: each integration module is a template for the next. Fourth: real adoption stories, on the site blog, with their name on it.
ES: La petición tiene que ser concreta. La primera es la más valiosa: feedback de diseño sobre el tipo de error que la gente realmente construyó. Segunda: decir cuántos good first issue hay abiertos (ocho al escribir este deck, #575 a #582: una fila del README, NonEmptySet/NonEmptyMap en las dos líneas de Jackson, un adaptador JAXB, tres clases de AssertJ, tokens del sitio, Dependabot para talks/) y describir uno en una frase; cada uno dice qué archivos tocar y cómo se ve terminado. Revisar la etiqueta el día anterior. Tercera: cada módulo de integración es la plantilla del siguiente. Cuarta: historias reales de adopción, en el blog del sitio, con su nombre.
-->

---
layout: section
---

# <T en="Try it on one endpoint" es="Pruébala en un solo endpoint" />

<p><Tk k="closing.ask" /></p>

<div class="cols mt-12">
  <Qr name="tutorial" url="domix.github.io/dmx-fun/guide/tutorial"><Tk k="closing.tutorial" /></Qr>
  <Qr name="repo" url="github.com/domix/dmx-fun"><Tk k="closing.repo" /></Qr>
</div>

<!--
EN: Leave this slide up during questions. Two sentences about the project and the speaker go here, at the end, not at the start: who maintains it, since when, and that every decision is an ADR anyone can argue with in an issue.
ES: Dejar esta slide durante las preguntas. Las dos frases sobre el proyecto y el ponente van aquí, al final, no al principio: quién lo mantiene, desde cuándo, y que cada decisión es un ADR que cualquiera puede discutir en un issue.
-->

---

# <T en="Three questions that always come" es="Tres preguntas que siempre llegan" />

| | |
|---|---|
| **<Tk k="qa.immutability.q" />** | <Tk k="qa.immutability.a" /> |
| **<Tk k="qa.checked.q" />** | <Tk k="qa.checked.a" /> |
| **<Tk k="qa.kotlin.q" />** | <Tk k="qa.kotlin.a" /> |

<p class="caption"><Tk k="qa.caption" /> <a href="https://domix.github.io/dmx-fun/blog/performance-cost-of-immutability-when-it-matters" target="_blank">immutability</a> · <a href="https://domix.github.io/dmx-fun/blog/why-just-use-exceptions-persists" target="_blank">exceptions</a> · <a href="https://domix.github.io/dmx-fun/blog/vavr-arrow-cats-fp-ts-what-problems-they-solve" target="_blank">Vavr, Arrow</a></p>

<!--
EN: [35–45] Show only if one of these is asked, or if the room is quiet. Immutability: records are small, escape analysis removes most short-lived allocations, and Valhalla is coming; the real costs are in large collections copied in loops, which this library does not ask you to do. Checked exceptions: they do solve the "visible in the signature" problem, but they do not compose: no lambdas, no map, no chaining, and every layer re-declares or wraps them. Kotlin: Arrow is the right answer in Kotlin; the question here is what to do in the Java you already have.
ES: [35–45] Mostrar solo si preguntan una de estas, o si la sala está callada. Inmutabilidad: los records son pequeños, el escape analysis elimina casi todas las asignaciones de vida corta, y Valhalla viene en camino; los costes reales están en colecciones grandes copiadas en bucles, que esta librería no te pide hacer. Excepciones checked: sí resuelven lo de "visible en la firma", pero no componen: sin lambdas, sin map, sin encadenar, y cada capa las vuelve a declarar o las envuelve. Kotlin: Arrow es la respuesta correcta en Kotlin; la pregunta aquí es qué hacer con el Java que ya tienes.
-->

---
layout: section
---

# <T en="Backup: chapters 6 and 7" es="Respaldo: capítulos 6 y 7" />

<p><Tk k="backup.intro" /></p>

<!--
EN: Not part of the 35 minutes. Jump here from the closing slide if a question leads to it.
ES: No forma parte de los 35 minutos. Saltar aquí desde la slide de cierre si una pregunta lleva a ello.
-->

---

# <T en="Two outcomes, no villain" es="Dos resultados, ningún villano" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter06/Outcome.java#outcome java

<div class="mt-6" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter06/EnrollmentMessages.java#describe java

<p class="caption"><Tk k="either.caption" /></p>

<!--
EN: Chapter 6. Business says a full course is a waiting list, not an error. Either<Waitlisted, Enrolled> inside the Result: two legitimate answers on the success track. The switch still covers everything, and CourseFull disappears from the error type.
ES: Capítulo 6. Negocio dice que un curso lleno es una lista de espera, no un error. Either<Waitlisted, Enrolled> dentro del Result: dos respuestas legítimas en la vía del éxito. El switch sigue cubriendo todo, y CourseFull desaparece del tipo de error.
-->

---

# <T en="At least one" es="Al menos uno" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter07/Offering.java#offering java

<div class="mt-6" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter07/WelcomeMail.java#compose java

<p class="caption"><Tk k="nel.caption" /></p>

<!--
EN: Chapter 7. NonEmptyList cannot be constructed empty, so head() always exists and the parameter type is the precondition. The same idea gives Result<NonEmptyList<Item>, String> in the demo's search endpoint.
ES: Capítulo 7. NonEmptyList no puede construirse vacía, así que head() siempre existe y el tipo del parámetro es la precondición. La misma idea da el Result<NonEmptyList<Item>, String> del endpoint de búsqueda de la demo.
-->
