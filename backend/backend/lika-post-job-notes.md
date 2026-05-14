Kopiere diesen Text in eine neue Datei, zum Beispiel:

```text
backend/lika-post-job-notes.md
```

````md
# Meine Notizen: Verbindung zwischen Frontend und Backend bei „Post a Job“

## Überblick

Ich habe an der Funktion „Post a Job“ gearbeitet. Ziel war es, das bestehende Frontend-Formular mit dem Java-Spring-Backend zu verbinden. Ein Client kann auf der Webseite einen Job eintragen. Diese Daten werden danach vom Backend empfangen und gespeichert.

Wir verwenden aktuell noch keine SQL-Datenbank, sondern speichern die Jobs zuerst in einer JSON-Datei. Das ist für den ersten Lernschritt gut, weil man daran den Ablauf zwischen Frontend und Backend klar verstehen kann.

Der Ablauf funktioniert jetzt so:

```text
post-a-job.html
→ JavaScript liest die Formulardaten
→ fetch() sendet die Daten als JSON an das Backend
→ JobController empfängt den POST-Request
→ JobStorage speichert den Job in jobs.json
→ GET /jobs zeigt die gespeicherten Jobs an
````

---

## Geänderte Dateien

Ich habe an folgenden Dateien gearbeitet:

```text
backend/src/main/java/at/ac/fhcampuswien/Job.java
backend/src/main/java/at/ac/fhcampuswien/JobController.java
backend/src/main/resources/application.properties
frontend/design3/post-a-job.html
```

Außerdem wird durch das Backend eine Datei für gespeicherte Jobs verwendet beziehungsweise erweitert:

```text
backend/jobs.json
```

---

## 1. Änderung in Job.java

### Datei

```text
backend/src/main/java/at/ac/fhcampuswien/Job.java
```

In dieser Datei befindet sich das Datenmodell für einen Job. Das bedeutet: Diese Klasse beschreibt, welche Informationen ein Job im System haben kann.

Ich habe die Klasse so erweitert, dass sie besser zum Formular in `post-a-job.html` passt. Das Formular enthält zum Beispiel ein Feld für den Namen der Firma oder des Clients und ein Feld für die Kontakt-E-Mail. Deshalb wurden diese Felder im Backend-Modell ergänzt:

```java
public String companyName;
public String contactEmail;
```

Das ist wichtig, weil Spring die JSON-Daten aus dem Frontend automatisch in ein `Job`-Objekt umwandelt. Wenn ein Feld im Frontend gesendet wird, aber in `Job.java` nicht existiert, kann dieses Feld nicht sinnvoll im Backend-Modell gespeichert werden.

Die wichtigsten Felder in `Job.java` sind jetzt:

```java
public String id;
public String clientId;
public String companyName;
public String contactEmail;
public String title;
public String description;
public String category;
public String designType;
public String location;
public String budget;
public String workMode;
public String deadline;
public String tags;
public String createdAt;
```

Die Felder `id` und `createdAt` werden nicht vom Frontend geschickt. Diese Werte werden automatisch im Backend erzeugt.

---

## 2. Bedeutung von JobController.java

### Datei

```text
backend/src/main/java/at/ac/fhcampuswien/JobController.java
```

Der `JobController` ist die Schnittstelle zwischen Frontend und Backend. Er stellt HTTP-Endpunkte bereit. Für meine Aufgabe war besonders dieser Endpoint wichtig:

```text
POST http://localhost:8080/jobs
```

Dieser Endpoint empfängt neue Jobdaten vom Frontend.

Im Controller gibt es diese Methode:

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public Job store(@RequestBody Job job) {
    job.id = UUID.randomUUID().toString();
    job.createdAt = Instant.now().toString();
    return storage.add(job);
}
```

Diese Methode macht Folgendes:

1. Sie empfängt ein JSON-Objekt vom Frontend.
2. Spring Boot wandelt dieses JSON automatisch in ein `Job`-Objekt um.
3. Das Backend erstellt automatisch eine eindeutige ID.
4. Das Backend erstellt automatisch einen Zeitstempel mit `createdAt`.
5. Der Job wird an `JobStorage` weitergegeben.
6. Der gespeicherte Job wird als JSON-Antwort zurückgegeben.

Dadurch muss das Frontend keine ID und keinen Zeitstempel selbst erstellen. Diese Verantwortung liegt beim Backend.

---

## 3. CORS-Freigabe im JobController

Beim ersten Test wurde die Anfrage vom Browser blockiert. Die Fehlermeldung war eine CORS-Fehlermeldung. Das lag daran, dass Frontend und Backend auf unterschiedlichen Ports laufen:

```text
Frontend: http://localhost:63342
Backend:  http://localhost:8080
```

Der Browser schützt Webseiten davor, einfach Daten von anderen Quellen zu laden oder zu senden. Deshalb musste das Backend explizit erlauben, dass Anfragen vom Frontend-Port `63342` akzeptiert werden.

Dafür wurde im `JobController` diese Annotation ergänzt:

```java
@CrossOrigin(origins = "http://localhost:63342")
```

Der Anfang der Klasse sieht dadurch so aus:

```java
@RestController
@CrossOrigin(origins = "http://localhost:63342")
@RequestMapping("/jobs")
public class JobController {
```

Damit erlaubt das Backend Anfragen von meinem lokalen Frontend.

---

## 4. Bedeutung von JobStorage.java

### Datei

```text
backend/src/main/java/at/ac/fhcampuswien/JobStorage.java
```

`JobStorage` ist aktuell für das Speichern der Jobs zuständig. Wir verwenden noch keine SQL-Datenbank, sondern zuerst eine einfache JSON-Datei.

Die Datei heißt:

```text
jobs.json
```

Die Klasse `JobStorage` kann:

1. Jobs aus `jobs.json` laden.
2. Neue Jobs zur Liste hinzufügen.
3. Die aktualisierte Liste wieder in `jobs.json` speichern.

Die Methode `add()` funktioniert so:

```java
public Job add(Job job) {
    List<Job> jobs = load();
    jobs.add(job);
    save(jobs);
    return job;
}
```

Das bedeutet:

1. Bestehende Jobs werden geladen.
2. Der neue Job wird hinzugefügt.
3. Die neue Liste wird gespeichert.
4. Der gespeicherte Job wird zurückgegeben.

Für den ersten Lernschritt ist das eine gute Lösung, weil man den Ablauf zwischen Frontend, Backend und Speicherung gut verstehen kann.

---

## 5. Änderung in application.properties

### Datei

```text
backend/src/main/resources/application.properties
```

Beim Starten des Backends gab es zuerst einen Fehler, weil Spring nach einer SQL-Datei gesucht hat:

```text
schema.sql
```

Da wir im Moment noch nicht mit SQL arbeiten, sondern mit `jobs.json`, wurde die SQL-Initialisierung deaktiviert.

Dafür wurde diese Zeile ergänzt:

```properties
spring.sql.init.mode=never
```

Dadurch sucht Spring beim Start nicht mehr nach einer `schema.sql`-Datei. Das Backend kann dadurch mit der JSON-Speicherung starten.

---

## 6. Änderung in post-a-job.html

### Datei

```text
frontend/design3/post-a-job.html
```

In dieser Datei befindet sich das Formular für „Post a Job“. Vorher hat das Formular die Daten nur in der Browser-Konsole ausgegeben.

Der alte Test-Code war ungefähr:

```javascript
console.log('post-a-job:', Object.fromEntries(new FormData(e.target)));
```

Das war nur ein Test. Die Daten wurden noch nicht an das Backend gesendet.

Ich habe den JavaScript-Code so geändert, dass beim Abschicken des Formulars ein `fetch()`-Request an das Backend gesendet wird.

Der wichtigste Teil ist:

```javascript
const response = await fetch("http://localhost:8080/jobs", {
  method: "POST",
  headers: {
    "Content-Type": "application/json"
  },
  body: JSON.stringify(job)
});
```

Dieser Code sendet die Jobdaten als JSON an das Backend.

---

## 7. Wie das Frontend die Formulardaten sammelt

Im JavaScript-Code wird zuerst verhindert, dass die Seite beim Absenden neu lädt:

```javascript
event.preventDefault();
```

Dann wird das Formular ausgelesen:

```javascript
const form = event.target;
const formData = new FormData(form);
```

Mit `FormData` kann JavaScript alle Werte auslesen, die im Formular ein `name`-Attribut haben.

Zum Beispiel:

```html
<input name="title">
```

kann im JavaScript gelesen werden mit:

```javascript
formData.get("title")
```

Danach wird ein Objekt erstellt, das zu `Job.java` passt:

```javascript
const job = {
  clientId: "client-1",
  companyName: formData.get("company"),
  contactEmail: formData.get("email"),
  title: formData.get("title"),
  description: formData.get("description"),
  category: formData.get("category"),
  designType: formData.get("category"),
  location: formData.get("location"),
  workMode: formData.get("location"),
  budget: formData.get("budget"),
  deadline: formData.get("deadline"),
  tags: ""
};
```

Dieses Objekt wird dann mit `JSON.stringify(job)` in JSON umgewandelt und an das Backend geschickt.

---

## 8. Warum designType und workMode doppelt gesetzt werden

Das aktuelle Formular hat kein eigenes Feld für `designType`. Deshalb wird vorläufig derselbe Wert wie bei `category` verwendet:

```javascript
designType: formData.get("category")
```

Das aktuelle Formular verwendet außerdem bei den Radio Buttons den Namen `location`. Die Werte sind:

```text
remote
onsite
hybrid
```

Diese Werte passen aber auch zum Backend-Feld `workMode`. Deshalb wird der Wert vorläufig für beide Felder verwendet:

```javascript
location: formData.get("location"),
workMode, formData.get("location")
```

Später könnte man das verbessern, indem man ein eigenes Feld für genaue Ortsangaben ergänzt, zum Beispiel Stadt oder Land.

---

## 9. Test der Verbindung

Nach den Änderungen habe ich das Backend gestartet. In der Konsole stand:

```text
Tomcat started on port 8080
Started Main
```

Das bedeutet, dass Spring Boot erfolgreich läuft.

Danach habe ich im Browser getestet:

```text
http://localhost:8080/jobs
```

Dort wurde eine JSON-Liste angezeigt. Das bedeutet, dass der GET-Endpunkt funktioniert.

Anschließend habe ich das Formular in `post-a-job.html` ausgefüllt und auf „Publish Listing“ geklickt. Danach erschien die Erfolgsmeldung:

```text
Job was published successfully.
```

Danach war der neue Job in der JSON-Liste unter `/jobs` sichtbar. Das bedeutet, dass die Verbindung erfolgreich funktioniert.

---

## 10. Beispiel eines gespeicherten Jobs

Ein gespeicherter Job sieht zum Beispiel so aus:

```json
{
  "id": "b460f3ce-9226-4ce2-a46f-fe253d5fced5",
  "clientId": "client-1",
  "companyName": "Lila",
  "contactEmail": "kevlishvili_lika@yahoo.com",
  "title": "Senger",
  "description": "",
  "category": "Motion Design",
  "designType": "Motion Design",
  "location": "hybrid",
  "budget": "€€",
  "workMode": "hybrid",
  "deadline": "",
  "tags": "",
  "createdAt": "2026-05-13T22:14:17.860991900Z"
}
```

Die Beschreibung ist leer, weil ich das Feld beim Test leer gelassen habe. Das zeigt, dass auch leere Felder korrekt übertragen und gespeichert werden.

---

## 11. Aktueller Stand

Die Funktion „Post a Job“ funktioniert jetzt grundsätzlich.

Aktuell funktioniert:

```text
Frontend-Formular ausfüllen
POST-Request an Backend senden
Job im Backend empfangen
ID und Zeitstempel automatisch erzeugen
Job in jobs.json speichern
gespeicherte Jobs über GET /jobs anzeigen
```

Noch offen für spätere Schritte:

```text
Jobs schöner auf jobs.html anzeigen
PUT für Job-Bearbeitung implementieren
DELETE für Job-Löschen implementieren
Login mit echtem clientId verbinden
später eventuell SQL statt jobs.json verwenden
```

---


