# Database Documentation

This project uses an H2 database to store and manage job posts.

H2 is a lightweight Java database. In this project, it is used as a file-based database, which means the data is saved locally and remains available after restarting the backend.

The database is used for storing jobs, loading jobs, searching jobs, updating jobs, deleting jobs, and getting a random job.

---

## Overview

The database system is split into several scripts/classes:

- `Database.java`
- `DatabaseInitializer.java`
- `JobRepository.java`
- `JobController.java`

The general flow is:

```text
Frontend
   ↓
JobController
   ↓
JobRepository
   ↓
H2 Database
```

The frontend sends HTTP requests to the backend.  
`JobController` receives these requests.  
`JobRepository` handles the database logic.  
The H2 database stores the actual job data.

---

## Database.java

`Database.java` is responsible for creating a connection to the H2 database.

It contains the database URL, username, and password.

The database URL used in this project is:

```java
jdbc:h2:file:./data/designerjobs
```

This means the database is stored locally inside the `data` folder.

The most important method is:

```java
Database.getConnection()
```

This method returns a database connection. Other classes, especially `JobRepository`, use this connection whenever they need to read from or write to the database.

---

## DatabaseInitializer.java

`DatabaseInitializer.java` creates the required database table if it does not already exist.

It uses the SQL command:

```sql
CREATE TABLE IF NOT EXISTS jobs (...)
```

This prevents the table from being recreated every time the backend starts.

The `jobs` table stores the following job data:

- `id`
- `client_id`
- `title`
- `description`
- `category`
- `design_type`
- `location`
- `budget`
- `work_mode`
- `deadline`
- `tags`
- `created_at`

The initializer should be called once when the backend starts:

```java
DatabaseInitializer.init();
```

This makes sure the database and the `jobs` table are ready before the application starts handling requests.

---

## JobRepository.java

`JobRepository.java` contains all database operations for jobs.

The controller does not directly write SQL queries. Instead, it calls methods from `JobRepository`.

This keeps the code cleaner because the controller only handles HTTP requests, while the repository handles database logic.

### add(Job job)

```java
add(Job job)
```

Adds a new job to the database.

This method uses an SQL `INSERT` statement and creates a new row in the `jobs` table.

---

### findAll()

```java
findAll()
```

Loads all jobs from the database.

The jobs are ordered by `created_at`, so newer jobs appear first.

---

### findById(String id)

```java
findById(String id)
```

Loads one specific job from the database by its id.

If no job with the given id exists, it returns `null`.

---

### getRandomJob()

```java
getRandomJob()
```

Returns one random job from the database.

This can be used for features like showing a random job recommendation.

---

### search(...)

```java
search(q, category, designType, location, budget, workMode, tags)
```

Searches jobs using optional filters.

Supported filters:

- `q`
- `category`
- `designType`
- `location`
- `budget`
- `workMode`
- `tags`

The `q` parameter searches inside the job title and description.

Example:

```http
GET /jobs?q=logo
```

The other filters can be used to narrow down the results.

Example:

```http
GET /jobs?category=graphic design&budget=small&workMode=remote
```

If a filter is not provided, it is ignored.

---

### update(String id, Job updated)

```java
update(String id, Job updated)
```

Updates an existing job in the database.

This method uses an SQL `UPDATE` statement.

If the job exists, the updated job is saved and returned.  
If the job does not exist, it returns `null`.

---

### deleteById(String id)

```java
deleteById(String id)
```

Deletes a job from the database by its id.

It returns `true` if a job was deleted and `false` if no job with that id was found.

---

## JobController.java

`JobController.java` exposes the job database through HTTP endpoints.

The frontend can call these endpoints using JavaScript `fetch()`.

The controller uses `JobRepository` to access the database.

---

### Add a new job

```http
POST /jobs
```

Adds a new job to the database.

The backend automatically generates:

- `id`
- `createdAt`

So the frontend does not need to send these values.

---

### Load or search jobs

```http
GET /jobs
```

Loads all jobs if no query parameters are provided.

It can also search jobs using optional query parameters.

Examples:

```http
GET /jobs
GET /jobs?q=logo
GET /jobs?category=graphic design
GET /jobs?category=graphic design&budget=small&workMode=remote
```

---

### Load a random job

```http
GET /jobs/random
```

Loads one random job from the database.

---

### Load one job by id

```http
GET /jobs/{id}
```

Loads one specific job by its id.

Example:

```http
GET /jobs/12345
```

---

### Update a job

```http
PUT /jobs/{id}
```

Updates an existing job.

Example:

```http
PUT /jobs/12345
```

---

### Delete a job

```http
DELETE /jobs/{id}
```

Deletes a job from the database.

Example:

```http
DELETE /jobs/12345
```

---

## Frontend Interaction

The frontend communicates with the backend through HTTP requests.

Example for loading all jobs:

```js
fetch("http://localhost:8080/jobs")
    .then(response => response.json())
    .then jobs => console.log(jobs));
```

Correct version:

```js
fetch("http://localhost:8080/jobs")
    .then(response => response.json())
    .then(jobs => console.log(jobs));
```

Example for adding a new job:

```js
fetch("http://localhost:8080/jobs", {
    method: "POST",
    headers: {
        "Content-Type": "application/json"
    },
    body: JSON.stringify({
        clientId: "client-1",
        title: "Logo design for coffee brand",
        description: "Need a clean logo for a small local coffee shop.",
        category: "graphic design",
        designType: "logo",
        location: "Vienna",
        budget: "small",
        workMode: "remote",
        deadline: "2026-06-01",
        tags: "logo, branding, coffee"
    })
});
```

---

## Summary

The H2 database stores all job posts locally.

`Database.java` creates the database connection.  
`DatabaseInitializer.java` creates the `jobs` table.  
`JobRepository.java` handles all SQL database operations.  
`JobController.java` exposes the database functionality through HTTP endpoints.

This structure keeps the project organized by separating the database logic from the controller logic.