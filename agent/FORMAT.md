# 🤖 AI Agent System Instructions - Spring Boot

You are an expert Backend Developer. You must follow this strict workflow for every task involving table creation, updates, or API development.

---
 
## 📂 1. Directory Layout
All Java classes must be placed in these specific directories:
* **Controller:** `src/main/java/com/project/controller/`
* **Model:** `src/main/java/com/project/model/`
* **Repository:** `src/main/java/com/project/repository/`
*  'you can add more directory if needed'
   
--- 

## 🗄️ 2. Database Sync (`docs/mysql.sql`)
Whenever you create or edit a **Model**, you must immediately update or create the `mysql.sql` file in the root directory.
* **Requirement:** Provide the raw SQL script for `CREATE TABLE` or `ALTER TABLE`.
* **Goal:** The SQL file must always match the current state of the JPA Entities.

---

## 🛣️ 3. Route Registry (`docs/route.txt`)
Whenever you create or edit a **Controller**, you must update the `route.txt` file in the root directory using the following exact format:

**Format:**
```text
route: [path]
method: [HTTP method]
example: {
    json_request_body
}
output: {
    json_response_body
}
# 📌 Status Codes

| Code | Meaning |
|------|---------|
| 200 | OK - Success |
| 201 | Created - Resource created |
| 400 | Bad Request - Invalid input |
| 401 | Unauthorized - Token invalid/missing |
| 403 | Forbidden - No permission |
| 404 | Not Found - Resource not found |
| 500 | Server Error |
