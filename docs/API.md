# SERVETRA JSON API — ETAP 3

Base path: `/servetra-server/api/v1`. Request and response bodies use UTF-8 JSON.

| Method | Path | Result |
|---|---|---|
| GET | `/equipment?q=` | Equipment list; optional text search |
| POST | `/equipment` | Create Equipment, HTTP 201 |
| GET / PUT / DELETE | `/equipment/{id}` | Read, update, or physically delete |
| GET / POST | `/specialists` | List or create Specialist |
| GET / PUT / DELETE | `/specialists/{id}` | Read, update, or delete |
| GET | `/requests?status=NEW` | List requests, optionally filtered |
| POST | `/requests` | Create request in `NEW`; F1 and I set by server |
| GET / PUT / DELETE | `/requests/{id}` | Read, edit ordinary fields, or delete |
| POST | `/requests/{id}/transitions` | Body `{"toStatus":"DIAGNOSED"}` |
| GET / POST | `/service-records?requestId=` | List or create records |
| GET / PUT / DELETE | `/service-records/{id}` | Read, update, or delete |
| GET | `/health` | Application and database health |

Error body:

```json
{
  "error": {
    "code": "INVALID_TRANSITION",
    "message": "Transition NEW -> COMPLETED is not allowed"
  }
}
```

Status policy: 200 success, 201 created, 204 deleted, 400 malformed request, 404 missing record, 409 duplicate/reference/transition conflict, 422 validation, 503 database failure, 500 unexpected error.

## Representative requests

Equipment create:

```json
{
  "inventoryNumber": "LAB-NEW-01",
  "name": "Digital microscope",
  "category": "LAB",
  "location": "Laboratory 112",
  "criticalityScore": 90,
  "nextServiceDue": "2027-02-01",
  "active": true
}
```

MaintenanceRequest create (server ignores client attempts to provide F1, I, status or timestamps):

```json
{
  "equipmentId": 1,
  "specialistId": 1,
  "summary": "Self-test failure",
  "description": "Error E17 after startup",
  "severityScore": 80,
  "downtimeImpactScore": 60,
  "dueAt": "2026-11-25T17:00:00"
}
```

For equipment with F1=90 the response contains `status: "NEW"` and `priorityIndex: 79.00`.

Lifecycle command:

```json
{"toStatus":"DIAGNOSED"}
```

ServiceRecord create:

```json
{
  "requestId": 1,
  "specialistId": 1,
  "workType": "DIAGNOSTICS",
  "description": "Measured supply voltages and inspected the optical path",
  "performedAt": "2026-11-21T12:00:00",
  "durationMinutes": 45,
  "costAmount": 0.00
}
```

## Stable error codes

| Code | HTTP | Meaning |
|---|---:|---|
| `BAD_JSON` | 400 | Body is not valid JSON |
| `INVALID_ID` / `MISSING_ID` | 400 | Path id is malformed or missing |
| `NOT_FOUND` | 404 | Resource does not exist |
| `DATA_CONFLICT` | 409 | Unique/FK restriction |
| `INVALID_TRANSITION` | 409 | Lifecycle edge is forbidden |
| `VALIDATION_ERROR` | 422 | Domain validation failed |
| `DATABASE_UNAVAILABLE` | 503 | JDBC/MySQL operation failed |
| `INTERNAL_ERROR` | 500 | Unexpected server failure |

