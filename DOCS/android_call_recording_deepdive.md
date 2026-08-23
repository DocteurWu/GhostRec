# Fiche Technique Canonique - Analyse d'Enregistrement d'Appel (Redmi Note 8 Pro)

**Projet :** Application Android `Quick Capture` (`com.example.quickcapture`)  
**Modèle Cible :** Xiaomi Redmi Note 8 Pro (`begonia`)  
**SoC / Chipset :** MediaTek Helio G90T (`mt6785`)  
**OS / ROM :** Android 11 / MIUI 12.5 (Stock ROM)  
**Documentation Référence :** [Android Developers - Sharing Audio Input](https://developer.android.com/media/platform/sharing-audio-input) & [MediaRecorder.AudioSource](https://developer.android.com/reference/android/media/MediaRecorder.AudioSource.html)  

---

## 1. Diagnostic Inflexible du Framework Android

Conformément à la spécification officielle Android Developers :
- **`MediaRecorder.AudioSource.VOICE_CALL`** correspond conceptuellement à la capture combinée de la voix émise (**Uplink / TX**) et de la voix reçue (**Downlink / RX**).
- L'accès aux sources de téléphonie `VOICE_CALL`, `VOICE_UPLINK` et `VOICE_DOWNLINK` est **strictement protégé par la permission privilège `CAPTURE_AUDIO_OUTPUT`**, réservée aux composants système préinstallés et signés.
- Sur une application tierce ordinaire sans privilèges système sur Android 11 non-rooté :
  - `AudioSource.MIC` enregistre uniquement le microphone physique (Uplink / TX local).
  - Tenter d'utiliser `VOICE_CALL` produit un fichier de 0 octet car le framework rejette l'accès au flux.
  - Basculer sur OpenSL ES, AAudio, NDK ou `AccessibilityService` ne contourne pas cette restriction d'accès au niveau `AudioPolicy` / Audio HAL.

---

## 2. Matrice Stratégique des Solutions Possibles

| Solution / Méthode | Enregistrement 2-Way (Uplink + Downlink) | Écouteur Interne (Sans Haut-parleur) | Nécessite Root | Taux de Succès Estimé |
| :--- | :---: | :---: | :---: | :---: |
| **`MediaRecorder.MIC` (App Tierce Standard)** | ❌ (Uplink seul) | ✅ | ❌ | **0%** (pour le Downlink RX) |
| **NDK / AudioRecord / AAudio / OpenSL ES** | ❌ (Uplink seul) | ✅ | ❌ | **Très Faible** |
| **`AccessibilityService` + `MIC`** | ❌ (Uplink seul) | ✅ | ❌ | **Très Faible** |
| **Application Privilégiée (`priv-app`) + `CAPTURE_AUDIO_OUTPUT`** | ✅ (Potentiellement) | ✅ | ✅ (généralement) | **Bonne** |
| **Exploitation de l'enregistreurs natif Xiaomi / MIUI** | ✅ (Potentiellement) | ✅ | Selon méthode | **Très Bonne** (si présent sur la ROM) |
| **Modification AudioPolicy / MediaTek HAL** | ✅ | ✅ | ✅ | **Très Bonne** (niveau ingénierie) |
| **Mode Haut-parleur + Micro** | ✅ | ❌ (Hors cahier des charges) | ❌ | **Non retenu** |

---

## 3. Feuille de Route d'Investigation Technique (En 3 Étapes)

### Étape 1 — Vérification du Enregistreur Natif MIUI
Vérifier si le Dialer natif de cette ROM MIUI 12.5 (Global/EU) propose la fonction intégrée d'enregistrement d'appel et si celle-ci capture les deux voix à l'écouteur interne.

### Étape 2 — Diagnostic via Traces System (`dumpsys`)
Si l'enregistreur natif Xiaomi fonctionne en 2-way à l'écouteur interne, capturer les logs pendant un appel actif :
```bash
adb shell dumpsys media.audio_flinger
adb shell dumpsys media.audio_policy
adb shell dumpsys audio
adb logcat -b all | grep -E "AudioPolicy|AudioFlinger|AudioRecord|AudioSource"
```
**Objectif :** Identifier le routage matériel exact (`AudioSource`, `AudioDevice`, `AudioSession`, et services vendor MediaTek propriétaires) utilisé par Xiaomi pour récupérer le flux RX + TX.

### Étape 3 — Reproduction ou Élévation dans Quick Capture
Selon les résultats de la trace :
1. **Option A (Privilèges Système) :** Conversion de Quick Capture en application privilège système avec `CAPTURE_AUDIO_OUTPUT`.
2. **Option B (Patch Vendor AudioPolicy) :** Modification de `/vendor/etc/audio_policy_configuration.xml` sur l'appareil rooté.
3. **Option C (Service Vendor Xiaomi) :** Invocations des APIs/Binders propriétaires MediaTek / Xiaomi identifiés.

---

## 4. Conclusion d'Ingénierie

Pour le projet `Quick Capture`, la recherche d'une Nième combinaison `AudioSource` en Java/Kotlin sur Android 11 non-rooté est définitivement close. La suite du développement reposera exclusivement sur la reproduction du chemin d'accès privilégié MIUI / MediaTek identifié via l'analyse `dumpsys`.
