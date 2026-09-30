from fastapi import FastAPI, Depends, HTTPException, BackgroundTasks
from fastapi.security import HTTPBasic, HTTPBasicCredentials
import secrets, subprocess, uuid, json, os

app = FastAPI(title="Kairo Sync API")
security = HTTPBasic()

SYNC_USER = os.getenv("SYNC_USERNAME", "admin")
SYNC_PASS = os.getenv("SYNC_PASSWORD", "changeme")
LIDARR_URL = os.getenv("LIDARR_URL", "http://lidarr:8686")
LIDARR_KEY = os.getenv("LIDARR_API_KEY", "")
NAVIDROME_URL = os.getenv("NAVIDROME_URL", "http://navidrome:4533")
JOBS = {}

def verify(creds: HTTPBasicCredentials = Depends(security)):
    if not (secrets.compare_digest(creds.username, SYNC_USER) and
            secrets.compare_digest(creds.password, SYNC_PASS)):
        raise HTTPException(status_code=401, detail="Invalid credentials")
    return creds.username

def run_sync(job_id: str):
    JOBS[job_id] = {"status": "running", "progress": 0}
    try:
        # Trigger Lidarr to search for all missing albums
        subprocess.run([
            "curl", "-s", "-X", "POST",
            f"{LIDARR_URL}/api/v1/command",
            "-H", f"X-Api-Key: {LIDARR_KEY}",
            "-H", "Content-Type: application/json",
            "-d", '{"name": "MissingAlbumSearch"}'
        ], timeout=30)
        JOBS[job_id]["progress"] = 50
        # Trigger Navidrome library rescan
        subprocess.run([
            "curl", "-s", "-X", "POST",
            f"{NAVIDROME_URL}/api/scanner/scan"
        ], timeout=30)
        JOBS[job_id] = {"status": "completed", "progress": 100}
    except Exception as e:
        JOBS[job_id] = {"status": "failed", "error": str(e)}

@app.post("/trigger-sync")
def trigger_sync(background: BackgroundTasks, user: str = Depends(verify)):
    job_id = str(uuid.uuid4())
    background.add_task(run_sync, job_id)
    return {"job_id": job_id, "status": "started"}

@app.get("/sync-status/{job_id}")
def sync_status(job_id: str, user: str = Depends(verify)):
    return JOBS.get(job_id, {"status": "unknown"})

@app.get("/library")
def library(user: str = Depends(verify)):
    # Scan /music and return JSON library index
    tracks = []
    for root, _, files in os.walk("/music"):
        for f in files:
            if f.endswith(('.flac', '.wav')):
                tracks.append({"file": os.path.join(root, f)})
    return {"tracks": tracks, "count": len(tracks)}