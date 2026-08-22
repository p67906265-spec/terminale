# SSH Terminal

App Android indipendente (nessuna pubblicità, nessuna dipendenza esterna non necessaria)
per collegarsi via SSH a un PC Linux nella rete locale di casa, con supporto a
comandi rapidi predefiniti (es. Docker, Home Assistant).

## Funzionalità
- Login con IP, porta, nome utente e password
- Terminale con output in tempo reale (shell interattiva via PTY)
- Barra di comandi rapidi personalizzabili (gestione tramite l'icona ⚙)
- Nessuna password salvata su disco (solo host/utente/porta per comodità)

## Come aprire il progetto
1. Apri Android Studio (Koala o successivo)
2. "Open" -> seleziona questa cartella
3. Lascia sincronizzare Gradle
4. Esegui su un dispositivo/emulatore Android (minSdk 24)

## Note tecniche
- Libreria SSH: [sshj](https://github.com/hierynomus/sshj)
- Verifica host key: `PromiscuousVerifier` (accetta qualsiasi host key -
  adatto a rete domestica fidata; da rafforzare se serve più sicurezza)
- Le credenziali sono inserite ad ogni connessione, la password non viene
  mai salvata

## Caricare su GitHub
```bash
git init
git add .
git commit -m "Initial commit: SSH Terminal app"
git branch -M main
git remote add origin https://github.com/<tuo-utente>/SshTerminal.git
git push -u origin main
```
