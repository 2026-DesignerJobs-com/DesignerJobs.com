# designerjobs backend — documentation

## overview

minimal spring boot REST api. two responsibilities: store job listings and search them. no auth, no database — jobs persist as a json array in `jobs.json` in the working directory. on first run, 60 randomised job listings are seeded across four designer categories.

---

## spring boot

### what it is

spring boot is a framework built on top of spring (a large java ecosystem for building applications). it removes boilerplate — repetitive setup code that every server needs but that has nothing to do with what your application actually does.

**boilerplate** is the code you have to write just to make the infrastructure work, before you can write a single line of real logic. in the prog2 version of this project (the `HttpServer` implementation), all of the following had to be written by hand before a single endpoint could respond:

```
HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
server.createContext("/api/jobs/", new JobController(jobs));
server.setExecutor(null);
server.start();
```

and inside each controller, every handler had to manually:
- check which http method was used (`GET`, `POST`, etc.) with an `if/else` chain
- read the raw request body byte-by-byte and convert it to a string
- parse that string into a map because there was no json library (`JsonUtils.parseJson()`)
- write the response headers manually (`Content-Type`, `Access-Control-Allow-Origin`, status code)
- send the response bytes back

that is all boilerplate. it is the same in every project, it is not specific to job listings, and it is easy to get wrong (e.g. forgetting a CORS header, misreading the byte stream).

**CORS (cross-origin resource sharing)** is a browser security rule. by default, a browser blocks javascript on one domain (e.g. `myapp.com`) from making requests to a server on a different domain (e.g. `api.myapp.com`). the server opts in by sending a response header:

```
Access-Control-Allow-Origin: *
```

`*` means any domain is allowed. without this header the browser rejects the response before your javascript ever sees it. in the prog2 version, `ApiUtils.sendResponse()` had to manually attach this header to every single response — one missed call and the frontend breaks. spring boot handles it automatically once configured.

**what spring boot replaces:**

| prog2 manual code | spring boot equivalent |
|---|---|
| `HttpServer.create(...)` + `server.start()` | `SpringApplication.run()` — server starts automatically |
| `server.createContext("/api/jobs/", handler)` | `@RequestMapping("/jobs")` on the controller class |
| `if (method.equals("POST"))` checks per handler | `@PostMapping` / `@GetMapping` on individual methods |
| `ApiUtils.readRequestBody(exchange)` | `@RequestBody` — spring reads and parses it |
| `JsonUtils.parseJson(body)` | jackson — spring deserialises directly into a `Job` object |
| `ApiUtils.sendResponse(exchange, 200, json)` | returning a value from the method — spring serialises and sends it |
| `ApiUtils.getQueryParam(exchange, "q")` | `@RequestParam String q` — spring binds it automatically |
| `ApiUtils.authenticate(exchange)` | (not needed here — auth was removed entirely) |

the current `Main.java` is 7 lines. the prog2 `Main.java` was 84 lines and didn't contain any business logic — only wiring.

### how it starts

```
Main.java → @SpringBootApplication → SpringApplication.run()
```

`@SpringBootApplication` tells spring boot to scan the package for annotated classes and auto-configure everything it finds (web server, jackson json parser, etc.). `SpringApplication.run()` boots an embedded tomcat server on port 8080.

there is no separate server install — the server is bundled inside the jar.

### annotations used in this project

annotations are the `@` symbols before classes and method parameters. they are instructions to the framework — declarations of intent that spring reads at startup to wire everything together.

| annotation | where | what it does |
|---|---|---|
| `@SpringBootApplication` | `Main` | enables component scan + auto-config |
| `@Component` | `JobStorage`, `DummyGenerator` | marks a class for spring to manage (instantiate and inject) |
| `@RestController` | `JobController` | marks a class as an http controller; return values are written as json automatically |
| `@RequestMapping("/jobs")` | `JobController` | sets the base url path for all methods in this controller |
| `@PostMapping` | `store()` | maps `POST /jobs` to this method |
| `@GetMapping` | `search()` | maps `GET /jobs` to this method |
| `@RequestBody` | `store()` param | tells spring to parse the request body json into a `Job` object |
| `@RequestParam` | `search()` params | binds a url query param (e.g. `?q=logo`) to a method argument |
| `@ResponseStatus(CREATED)` | `store()` | sends http 201 instead of 200 on success |

### dependency injection

instead of writing `new JobStorage()` yourself, spring creates one instance of `JobStorage` and passes it into any class that declares it as a constructor parameter. this is called dependency injection (DI).

```java
// spring sees @Component on JobStorage — it creates one instance
// spring sees JobController needs a JobStorage — it injects it
public JobController(JobStorage storage) {
    this.storage = storage;
}
```

in the prog2 version this was done manually in `Main.java`:
```java
// prog2: you created and passed dependencies yourself
List<JobPost> jobs = DummyGenerator.generateJobPosts(users);
new JobController(jobs);
```

spring's DI means you never call `new` on managed classes — spring owns their lifecycle.

### jackson (json)

spring boot includes jackson by default. it converts java objects to json and back automatically:
- `@RequestBody Job job` — jackson reads the request json and fills a `Job` object
- returning a `Job` or `List<Job>` — jackson serialises it to json in the response

`JobStorage` also uses jackson's `ObjectMapper` directly to read/write `jobs.json` on disk.

---

## architecture

```
Main.java           spring boot entry point — 7 lines
Job.java            data model — plain public fields, no annotations needed
JobStorage.java     reads and writes jobs.json using jackson ObjectMapper
JobController.java  two endpoints: POST /jobs and GET /jobs
DummyGenerator.java seeds jobs.json on first run via CommandLineRunner
```

---

## data model

`Job` fields:

| field       | type   | values / notes                                   |
|-------------|--------|--------------------------------------------------|
| id          | string | uuid — assigned by server on POST                |
| title       | string | free text                                        |
| description | string | free text                                        |
| category    | string | `webdesign`, `graphic design`, `interior design` |
| location    | string | free text                                        |
| budget      | string | `small`, `medium`, `big`                         |
| workMode    | string | `remote`, `on site`, `hybrid`                    |
| createdAt   | string | iso-8601 timestamp — assigned by server on POST  |

---

## endpoints

### POST /jobs

stores a new job. `id` and `createdAt` are always overwritten by the server regardless of what is sent.

**request body** (json):
```json
{
  "title": "Logo Designer",
  "description": "minimalist logo for a tech startup",
  "category": "graphic design",
  "location": "Vienna",
  "budget": "small",
  "workMode": "remote"
}
```

**response** `201 Created`:
```json
{
  "id": "3f1a9c...",
  "title": "Logo Designer",
  "description": "minimalist logo for a tech startup",
  "category": "graphic design",
  "location": "Vienna",
  "budget": "small",
  "workMode": "remote",
  "createdAt": "2026-05-01T10:00:00Z"
}
```

---

### GET /jobs

returns all jobs. filter with any combination of query params. no params = return all.

**query params** (all optional):

| param     | match type          | example                        |
|-----------|---------------------|--------------------------------|
| q         | substring           | `q=logo` (title + description) |
| category  | exact, ignore case  | `category=webdesign`           |
| location  | substring           | `location=Vienna`              |
| budget    | exact, ignore case  | `budget=small`                 |
| workMode  | exact, ignore case  | `workMode=remote`              |

**response** `200 OK` — array of matching jobs, or `[]` if none match.

---

## storage

`jobs.json` sits in the working directory (`backend/` when running via maven from that directory). it is a pretty-printed json array. every POST appends to it; every GET reads the whole file. no in-memory cache — acceptable at demo scale, not suitable for production load.

### jackson objectmapper

`JobStorage` uses jackson's `ObjectMapper` directly to handle file i/o. here's what each call does:

**reading — `mapper.readValue()`**

```java
mapper.readValue(f, new TypeReference<List<Job>>() {});
```

`readValue` takes a source (here a `File`) and a target type, then deserialises the json into that type.

the second argument `new TypeReference<List<Job>>() {}` is how you tell jackson the exact generic type to produce. you can't write `List<Job>.class` in java because of type erasure — generics are stripped at compile time, so the jvm has no `Class` object for `List<Job>`. `TypeReference` works around this by capturing the generic type at the point where the anonymous subclass is defined, before erasure kicks in.

without it, jackson would only know you want a `List` and would fill it with raw `LinkedHashMap` objects instead of `Job` instances.

**writing — `mapper.writerWithDefaultPrettyPrinter().writeValue()`**

```java
mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE), jobs);
```

broken into two steps:

1. `writerWithDefaultPrettyPrinter()` — returns an `ObjectWriter` configured to indent the output. without this, jackson writes compact single-line json, which is fine for machines but unreadable. the "default" pretty printer uses 2-space indentation.

2. `.writeValue(file, jobs)` — serialises the `jobs` list to json and writes it directly to the file, overwriting whatever was there. jackson reflects over the `Job` class fields (`id`, `title`, etc.) and maps them to json keys by name.

the chain exists because `ObjectMapper` has no `writeValuePretty()` shortcut — you configure a writer first, then write with it.

---

## seed data — DummyGenerator

### role

`DummyGenerator` is a spring `@Component` that implements the `CommandLineRunner` interface. spring automatically calls `run()` once after the application has fully started — after all beans are wired, after the server is listening. this makes it the right place for startup tasks like seeding data.

**what a bean is**

a bean is any object that spring creates and manages for you. when spring scans the package at startup and finds a class annotated with `@Component`, `@RestController`, or similar, it instantiates that class itself and keeps a reference to it — that instance is the bean.

in this project there are three beans:

```
JobStorage      → created because of @Component
JobController   → created because of @RestController
DummyGenerator  → created because of @Component
```

**wiring** is the step where spring looks at each bean's constructor, sees what other beans it needs, and passes them in. `JobController` declares it needs a `JobStorage` — spring holds the `JobStorage` bean it already created and injects it. same for `DummyGenerator`. this is dependency injection from the framework's perspective.

"after all beans are wired" means spring has finished creating every managed object and connecting them together. only then does it call `run()` — so `DummyGenerator` can safely use `storage` knowing it is fully initialised and not null.

### guard check

```java
if (!storage.load().isEmpty()) return;
```

the first thing `run()` does is load `jobs.json` and check if it already has entries. if it does, the method returns immediately and nothing is seeded. this means the seed runs exactly once — on the very first start when the file is absent or empty. every subsequent restart skips it, so existing data is never overwritten.

### generation loop

```java
for (int i = 0; i < 15; i++) {
    jobs.add(uiuxJob());
    jobs.add(graphicJob());
    jobs.add(interiorJob());
    jobs.add(webJob());
}
```

the loop runs 15 times, calling one generator per job type each iteration. this produces 60 jobs (15 × 4). all 60 are collected in a list and written to disk in a single `storage.save()` call — one file write instead of 60.

### word pools

seven static string arrays hold the raw vocabulary:

| array | size | used in |
|---|---|---|
| `ADJECTIVES` | 12 | titles and descriptions |
| `ACTIONS` | 8 | descriptions (verb: "create", "revamp"…) |
| `CLIENTS` | 8 | descriptions (employer type) |
| `UIUX_PROJECTS` | 8 | ui/ux titles and descriptions |
| `GRAPHIC_PROJECTS` | 8 | graphic design titles and descriptions |
| `INTERIOR_SPACES` | 8 | interior design titles and descriptions |
| `WEB_PROJECTS` | 8 | web design titles and descriptions |

`LOCATIONS`, `BUDGETS`, and `WORKMODES` are also arrays — picked randomly per job so the generated listings vary in all dimensions.

### type functions

each job type has its own method that picks from its specific project pool:

```java
private Job uiuxJob() {
    String project = pick(UIUX_PROJECTS);         // e.g. "design system"
    return job(
        pick(ADJECTIVES) + " UI/UX Designer for " + project,   // title
        sentence("ui/ux designer", project),                     // description
        "webdesign"                                              // category
    );
}
```

the four functions (`uiuxJob`, `graphicJob`, `interiorJob`, `webJob`) all follow the same structure — only the project pool and category string differ.

### sentence generator

```java
private String sentence(String role, String subject) {
    return "seeking a " + pick(ADJECTIVES) + " " + role
        + " to " + pick(ACTIONS) + " a " + subject
        + " for our " + pick(CLIENTS);
}
```

`sentence()` assembles a description from three independent random picks — adjective, action verb, and client type. the `role` and `subject` are passed in from the type function so each category produces contextually relevant descriptions.

example output: `"seeking a bold graphic designer to revamp a logo for our boutique studio"`

### pick helper

```java
private String pick(String[] arr) {
    return arr[(int) (Math.random() * arr.length)];
}
```

`Math.random()` returns a `double` in `[0.0, 1.0)` — never exactly 1.0. multiplying by the array length gives a value in `[0.0, length)`. casting to `int` truncates the decimal, producing a valid index from `0` to `length - 1`.

### job builder

```java
private Job job(String title, String description, String category) {
    Job j = new Job();
    j.id = UUID.randomUUID().toString();
    j.title = title;
    j.description = description;
    j.category = category;
    j.location = pick(LOCATIONS);
    j.budget = pick(BUDGETS);
    j.workMode = pick(WORKMODES);
    j.createdAt = Instant.now().toString();
    return j;
}
```

the shared `job()` builder handles the three fields that are always random regardless of type (`location`, `budget`, `workMode`) and assigns `id` and `createdAt`. the type functions only need to supply title, description, and category.

---

## running

```sh
cd backend
mvn spring-boot:run
```

requires java 17 and maven. server starts on port 8080.

to reseed with fresh random data: delete `jobs.json` and restart.
