# Storage Server Setup (storage.wakilfy.com) – Kurekebisha 403 na OpaqueResponseBlocking

## Shida unayoona

- **403 Forbidden** – Browser inapofanya GET kwa `https://storage.wakilfy.com/posts/xxx.jpeg`, server inajibu 403.
- **OpaqueResponseBlocking** – Kwa sababu response ni 403 (au hakuna CORS), browser inazuia kutumia resource hiyo (haswa ikiwa inatumika kwenye canvas/fetch).

Sababu kuu: **kwenye VPS ya storage (storage.wakilfy.com)** – si kwenye backend ya Spring Boot. Backend inaweka faili kwa SFTP tu; kuserve faili kwa browser ni kazi ya **Nginx** (au server yoyote) kwenye storage.wakilfy.com.

---

## 1. Hakikisha path na ruhusa kwenye VPS

Faili zinawekwa na backend kwenye:

- **Path kwenye server:** `/root/wakilfy-media/uploads/`
- **Subdirectories:** `posts/`, `avatars/`, n.k.

Lazima:

1. **Folder liko na Nginx anaweza kulisoma**  
   Kawaida Nginx inakimbia kama user `www-data` au `nginx`. Files chini ya `/root/` mara nyingi zina ruhusa za `root` peke yake, hivyo `www-data` haziwezi kuzisoma.

   **Njia rahisi (recommended):** weka media **si** chini ya `/root/`:

   ```bash
   # Kwenye storage.wakilfy.com (SSH)
   sudo mkdir -p /var/www/wakilfy-media/uploads
   sudo chown -R www-data:www-data /var/www/wakilfy-media
   sudo chmod -R 755 /var/www/wakilfy-media
   ```

   Kisha katika **application.properties** (backend) badilisha path kuwa ile mpya, na kwenye Nginx tumia `/var/www/wakilfy-media/uploads` (angalia chini).

   **Ikiwa unabaki na `/root/wakilfy-media/uploads/`:**

   ```bash
   sudo chmod 755 /root
   sudo chmod -R 755 /root/wakilfy-media
   # Optional: give nginx read access
   sudo chown -R root:www-data /root/wakilfy-media
   sudo chmod -R 750 /root/wakilfy-media
   ```

2. **Backend iweke faili kwenye path ile ile** ambayo Nginx inaserve (tazama `storage.vps.upload-path` na Nginx `root`/`alias`).

---

## 2. Nginx config kwa storage.wakilfy.com

Kwenye **storage.wakilfy.com** (VPS), hakikisha kuna **server block** kwa `storage.wakilfy.com` na **CORS** ili images zifunguke kwenye frontend (kutoka domain tofauti).

**Chaguo A – Media chini ya `/var/www/wakilfy-media/uploads` (recommended)**

```nginx
server {
    listen 443 ssl http2;
    server_name storage.wakilfy.com;

    # SSL (weka paths zako)
    ssl_certificate     /etc/letsencrypt/live/storage.wakilfy.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/storage.wakilfy.com/privkey.pem;

    # Root: files live at /var/www/wakilfy-media/uploads/
    root /var/www/wakilfy-media/uploads;
    location / {
        # Allow GET from any origin (so frontend can show images)
        add_header Access-Control-Allow-Origin "*" always;
        add_header Cache-Control "public, max-age=31536000" always;
        try_files $uri $uri/ =404;
    }
}
```

**Chaguo B – Bado unatumia `/root/wakilfy-media/uploads/`**

```nginx
server {
    listen 443 ssl http2;
    server_name storage.wakilfy.com;

    ssl_certificate     /path/to/fullchain.pem;
    ssl_certificate_key /path/to/privkey.pem;

    location / {
        alias /root/wakilfy-media/uploads/;
        add_header Access-Control-Allow-Origin "*" always;
        add_header Cache-Control "public, max-age=31536000" always;
    }
}
```

**Muhimu:**

- `root` au `alias` lazima iwe **path ile ile** ambayo backend inaweka faili (`storage.vps.upload-path`).
- `Access-Control-Allow-Origin "*"` inasaidia kuepuka **OpaqueResponseBlocking** wakati frontend inatumia domain tofauti (cross-origin).

Baada ya kubadilisha:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

---

## 3. Backend (application.properties)

Ikiwa umebadilisha path kwenye VPS kwa `/var/www/wakilfy-media/uploads`, badilisha na hii:

```properties
storage.vps.upload-path=/var/www/wakilfy-media/uploads/
```

Lazima path hi iwe **sawa** na ile Nginx inayotumia (`root` au `alias`).

---

## 4. Ukaguzi wa haraka

- **403:**  
  - Path na ruhusa kwenye VPS (Nginx user anaweza kusoma folder na faili?).  
  - Nginx `root`/`alias` inalingana na `storage.vps.upload-path`?

- **OpaqueResponseBlocking:**  
  - Ongeza `Access-Control-Allow-Origin` kwenye Nginx (kama hapo juu).  
  - Hakikisha request inapata **200** (si 403) – mara 403 inapopoa, blocking mara nyingi inapungua.

---

## Muhtasari

| Kitu | Mahali | Hatua |
|------|--------|--------|
| 403 Forbidden | Nginx + VPS | Nginx serve path sahihi, ruhusa za folder/faili kwa Nginx user |
| OpaqueResponseBlocking | Nginx | Ongeza `Access-Control-Allow-Origin "*"` kwenye Nginx |
| Path | Backend + VPS | `storage.vps.upload-path` = path ile ile Nginx inayoserve |

Baada ya kufanya haya, upload za images/media zinapaswa kuonekana kwenye browser bila 403 na bila OpaqueResponseBlocking.
