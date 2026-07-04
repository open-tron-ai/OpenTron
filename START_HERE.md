# RUN THIS NOW

## One Command to Start Everything

Open PowerShell and run:

```powershell
cd C:\Users\ciorica\Documents\OpenTron
powershell -ExecutionPolicy Bypass -File .\start-stack.ps1
```

## What Happens

1. PostgreSQL starts in Docker
2. Backend builds and starts in new window
3. Frontend starts in new window
4. Tauri app opens automatically

## Wait For

- Backend window: "Started OpentronBackendApplication"
- Frontend window: "Local: http://localhost:5173"
- Tauri app: Desktop window appears

## Then

1. Open Storage Dashboard in the app
2. Test by executing an agent
3. Watch data appear in real-time

---

**That's it! Everything else is automated. 🚀**
