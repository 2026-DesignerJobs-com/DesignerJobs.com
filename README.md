# DesignerJobs.com

FH-Campus-Wien/VZ2028/SoSe26/web/2-Group AF

## Backend

spring boot REST api for storing and searching job listings. data persists in `jobs.json`. seeds 60 randomised job listings on first run.

## run

```sh
cd backend
mvn spring-boot:run
```

requires java 17 and maven. server starts on `http://localhost:8080`.

to reseed: delete `backend/jobs.json` and restart.

## endpoints

| method | path | description |
|--------|------|-------------|
| `POST` | `/jobs` | store a new job |
| `GET`  | `/jobs` | search jobs (all params optional) |

### search query params

| param | match | example |
|-------|-------|---------|
| `q` | substring in title or description | `q=logo` |
| `category` | exact | `category=graphic+design` |
| `location` | substring | `location=Vienna` |
| `budget` | exact | `budget=small` |
| `workMode` | exact | `workMode=remote` |

### job fields

`title`, `description`, `category`, `location`, `budget`, `workMode`
— `id` and `createdAt` are assigned by the server on POST.

### examples

```sh
# store a job
curl -X POST http://localhost:8080/jobs \
  -H "Content-Type: application/json" \
  -d '{"title":"Logo Designer","category":"graphic design","location":"Vienna","budget":"small","workMode":"remote"}'

# get all jobs
curl http://localhost:8080/jobs

# search by keyword and filter
curl "http://localhost:8080/jobs?q=logo&budget=small"

# filter by category and work mode
curl "http://localhost:8080/jobs?category=webdesign&workMode=remote"
```
