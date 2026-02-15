# Chunked Upload API – Video/Media kubwa bila 413

## Utangulizi

Badala ya kutuma video ya 100MB kwa mpigo mmoja, client inaitatua katika vipande vidogo (chunks) vya 1MB au 500KB. Kila chunk inapita Nginx bila 413. Server inaziunganisha na kuweka kwenye storage.

**Faida:**
- Hauna 413 Request Entity Too Large
- Resumable (ukiisha chunks, unaweza kuendelea)
- Progress bar sahihi kwenye UI
- **Upload Ticket (Unique ID)** – backend inatengeneza ID; watumiaji wawili wanapoupload "video.mp4" kwa wakati mmoja hawavurugani

---

## Mchakato wa 3 Hatua

| Hatua | Jukumu la Frontend | Jukumu la Backend |
|-------|--------------------|-------------------|
| 1. Kuanza | Omba "Upload Ticket" (filename, subdirectory, totalChunks) | Tengeneza Unique ID, andaa folder la muda |
| 2. Kutuma | Kata file, tuma chunks zikiwa na uploadId | Pokea kila chunk, iweke kwenye folder ya uploadId |
| 3. Kumaliza | Uliza finalize (uploadId, filename, subdirectory) | Unganisha chunks, validate, hamisha kwenye permanent storage |

---

## API Endpoints

### Hatua 1: POST /api/v1/upload/start (Upload Ticket)

**Kabla** ya kutuma chunks, omba ticket. Backend inatengeneza **unique uploadId** na kuandaa folder la muda.

**Body (JSON):**
```json
{
  "filename": "video.mp4",
  "subdirectory": "posts",
  "totalChunks": 50
}
```

**Response:**
```json
{
  "success": true,
  "message": "Upload ticket issued",
  "data": {
    "uploadId": "uuid-from-backend",
    "filename": "video.mp4",
    "totalChunks": 50
  }
}
```

Tumia `uploadId` hii kwa chunks na complete.

---

### Hatua 2: POST /api/v1/upload/chunk

Tuma chunk moja. **Content-Type:** `multipart/form-data`

| Field       | Type | Required | Description                          |
|-------------|------|----------|--------------------------------------|
| uploadId    | string | ✅     | **Kutoka backend** (upload/start) – si client |
| chunkIndex  | int    | ✅     | 0-based index ya chunk               |
| totalChunks | int    | ✅     | Jumla ya chunks                      |
| filename    | string | ✅     | Jina la faili (video.mp4)           |
| chunk       | file   | ✅     | Chunk yenyewe (binary)               |

**Response:**
```json
{
  "success": true,
  "message": "Chunk saved",
  "data": {
    "uploadId": "...",
    "chunkIndex": 0,
    "totalChunks": 50,
    "receivedChunks": 1,
    "complete": false
  }
}
```

### Hatua 3: POST /api/v1/upload/complete

Baada ya kutuma chunks zote, tuma request hii kuunganisha na kupata URL.

**Body (JSON):**
```json
{
  "uploadId": "uuid-from-step-1",
  "filename": "video.mp4",
  "subdirectory": "posts"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Upload complete",
  "data": {
    "url": "https://storage.wakilfy.com/posts/abc-123.mp4",
    "thumbnailUrl": "https://storage.wakilfy.com/posts/abc-123_thumb.jpg"
  }
}
```
For videos, `thumbnailUrl` is also returned (FFmpeg extracts a frame). Pass it as `thumbnailUrls` when creating a post so story cards show the cover.

---

## Create Post na mediaUrls

Baada ya chunked upload, tumia **POST /api/v1/posts** na `Content-Type: application/json`:

```json
{
  "caption": "Video yangu",
  "postType": "STORY",
  "visibility": "PUBLIC",
  "mediaUrls": [
    "https://storage.wakilfy.com/posts/abc-123.mp4"
  ]
}
```

**Note:** POST /api/v1/posts inakubali mbili:
- `multipart/form-data` – data + files (upload kawaida)
- `application/json` – data + mediaUrls (baada ya chunked upload)

---

## Frontend – JavaScript/TypeScript Example


```javascript
const CHUNK_SIZE = 1024 * 1024; // 1MB (au 512 * 1024 kwa 500KB)

async function uploadFileChunked(file, subdirectory = 'posts', onProgress) {
  const totalChunks = Math.ceil(file.size / CHUNK_SIZE);

  // Step 1: Obtain Upload Ticket (backend generates unique uploadId)
  const startRes = await fetch('/api/v1/upload/start', {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ filename: file.name, subdirectory, totalChunks }),
  });
  const { data } = await startRes.json();
  const uploadId = data.uploadId;
  if (!uploadId) throw new Error('Failed to obtain upload ticket');

  if (onProgress) onProgress(2); // "Preparing..."

  // Step 2: Send chunks
  for (let i = 0; i < totalChunks; i++) {
    const start = i * CHUNK_SIZE;
    const end = Math.min(start + CHUNK_SIZE, file.size);
    const chunk = file.slice(start, end);

    const formData = new FormData();
    formData.append('uploadId', uploadId);
    formData.append('chunkIndex', i);
    formData.append('totalChunks', totalChunks);
    formData.append('filename', file.name);
    formData.append('chunk', chunk);

    const res = await fetch('/api/v1/upload/chunk', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` },
      body: formData,
    });

    if (!res.ok) throw new Error('Chunk upload failed');
    const progress = Math.round(((i + 1) / totalChunks) * 98) + 2;
    if (onProgress) onProgress(progress);
  }

  // Step 3: Finalize
  const completeRes = await fetch('/api/v1/upload/complete', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      uploadId,
      filename: file.name,
      subdirectory,
    }),
  });

  const json = await completeRes.json();
  if (!json.success) throw new Error(json.message || 'Complete failed');
  if (onProgress) onProgress(100);
  return json.data.url;
}

// Usage – Create Story with video (with progress bar)
async function createStoryWithVideo(videoFile, caption, setUploadProgress) {
  const url = await uploadFileChunked(videoFile, 'posts', (pct) => {
    setUploadProgress(pct); // React state → inaendelea progress bar
  });

  const createRes = await fetch('/api/v1/posts', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      caption,
      postType: 'STORY',
      visibility: 'PUBLIC',
      mediaUrls: [url],
    }),
  });
  if (!createRes.ok) throw new Error('Create post failed');
}
```

---

## Njia ya Kutumia

| Ukubwa wa faili | Njia                          |
|-----------------|------------------------------|
| &lt; 1MB        | Multipart kawaida (files)     |
| ≥ 1MB           | Chunked upload → mediaUrls    |

Frontend inaweza ku-check `file.size`:
- Ikiwa < CHUNK_SIZE, tumia POST /api/v1/posts na `files`
- Ikiwa ≥ CHUNK_SIZE, tumia chunked upload kisha create post na `mediaUrls`

---

## Configuration

| Property            | Default | Description                          |
|---------------------|---------|--------------------------------------|
chunk v1| upload.path         | uploads | Base path; chunks go to upload.path/chunks |
| upload.chunk-size   | 524288  | 500KB – Frontend default (stays under Nginx 1MB) |

**Chunk size:** Frontend inatumia 500KB (512*1024) ili kila request ibaki chini ya Nginx default 1MB (chunk + multipart overhead ≈ 510KB). Kama Nginx ina `client_max_body_size 5M`, unaweza kubadilisha kwa 1MB chunks kwa haraka zaidi.

---

## Troubleshooting

### 413 on POST /upload/chunk

**Sababu:** Nginx ina limit ya 1MB (default). Chunk ya 1MB + multipart overhead ≈ 1.05MB inazidi.

**Suluhisho (chaguo 1 – recommended):** Frontend tayari inatumia chunks za **500KB**. Redeploy frontend – inapaswa kufanya kazi.

**Suluhisho (chaguo 2):** Ongeza kwenye Nginx (wakilfy.com):
```nginx
location /api/ {
    client_max_body_size 5M;
    proxy_pass http://127.0.0.1:8080;
    # ... proxy headers
}
```
Kisha `sudo nginx -t && sudo systemctl reload nginx`.

---

### 500 on POST /upload/start

**Sababu:** Server haiwezi kuunda au kuandika kwenye `uploads/chunks`.

**Suluhisho:**
1. **Angalia server logs** – Tafuta "Failed to create chunk dir" au "fallback to temp dir".
2. **Ruhusa** – Folder `uploads` (au path yote ya upload.path) lazima iwe writable kwa user anayeendesha Spring Boot.
3. **Production** – Tumia path kamili, mfano:
   ```properties
   upload.path=/var/lib/wakilfy/uploads
   ```
   Kisha `mkdir -p /var/lib/wakilfy/uploads/chunks && chown app-user:app-user /var/lib/wakilfy/uploads`
4. **Fallback** – Ikiwa `upload.path/chunks` haijaandikika, backend inatumia `/tmp/wakilfy-chunks`.
