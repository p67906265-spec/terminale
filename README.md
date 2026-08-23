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
