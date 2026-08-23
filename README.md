# Terminale Android 1.3

Client SSH Android per rete locale.

## Novità 1.3
- Rimossi dall'output i codici di controllo ANSI/VT100 visibili (`[01;34m`, `[?2004h`, ecc.).
- Gestione anche delle sequenze ANSI spezzate tra più pacchetti SSH.
- Comandi rapidi: aggiunta, modifica ed eliminazione direttamente dall'app.
- Pulsanti rapidi più compatti e sempre scorrevoli orizzontalmente.
- Mantiene il fix X25519/BouncyCastle e l'invio SSH fuori dal main thread.


Aggiornamento 1.4: tasti con colore blu ciclamino per migliore visibilità.


Aggiornamento 1.5: Indietro/Home lasciano attiva la sessione SSH; aggiunto pulsante Disconnetti.


Aggiornamento 1.7: mantiene lo schermo attivo durante una sessione SSH aperta per evitare disconnessioni dovute allo standby.


Aggiornamento 1.8: nuova icona app ispirata all'estetica Matrix, senza usare il logo ufficiale.


Aggiornamento 1.9:
- Firma visibile nelle impostazioni: Paolo Free 1.0.
- Build release firmata con chiave permanente tramite GitHub Secrets.
- Dopo la prima installazione della versione firmata, gli aggiornamenti successivi possono essere installati sopra senza disinstallare.

Fix release 1.9: disattivata minificazione R8 per compatibilita con la libreria SSH; firma permanente invariata.


Aggiornamento 1.10:
- riattivata minificazione R8 con regola mirata per SSHJ/EdDSA;
- rimossa l'accettazione indiscriminata delle host key SSH;
- aggiunta verifica TOFU della chiave del server;
- firma release permanente invariata.


Aggiornamento 1.11: corretto commento sicurezza TOFU in SshManager e aggiornato il nome artifact GitHub Actions alla versione 1.11.


Aggiornamento 1.12:
- aggiunta scelta "Locale / Tailscale" nella schermata di connessione;
- indirizzo locale e indirizzo Tailscale vengono memorizzati separatamente;
- Tailscale accetta IP 100.x.x.x oppure nome MagicDNS;
- nessun token o API Tailscale viene salvato nell'app;
- firma release permanente invariata.

Fix 1.12: corretto XML della schermata Login/Tailscale.


Aggiornamento 1.13:
- blocco dell'app con impronta/biometria o credenziale sicura del dispositivo;
- password SSH opzionalmente salvate cifrate con AES/GCM;
- chiave di cifratura custodita nell'Android Keystore;
- password locale e Tailscale memorizzate separatamente;
- la password non viene mai scritta nel repository o nei log;
- firma release permanente invariata.


Aggiornamento 1.14:
- nuova autenticazione biometrica quando Terminale torna dal background/app recenti;
- la sessione SSH resta viva ma l'interfaccia rimane bloccata fino all'autenticazione;
- chiave AES Android Keystore vincolata a recente autenticazione utente;
- aggiornata la documentazione interna sul salvataggio password;
- firma release permanente invariata.

- migrazione automatica delle password 1.13 alla nuova chiave Keystore V2 vincolata all'autenticazione.
