# Firebase Backend Setup & Operational Runbook

This document outlines the final Firebase infrastructure configuration for **Dual Front TD** v0.0.1.009 (versionCode 30).

---

## 1. Firebase Project Overview
* **Project ID:** `roguetddualfront`
* **App Package Name:** `com.aistudio.roguetd.xtkp`
* **SHA-1 Fingerprint:** `17:47:75:25:23:33:dc:2e:81:59:f5:ac:6d:e1:01:96:61:c3:e8:97`
* **Tier Budget:** Strict **$0.00 Spark Free Tier** defense (no document sprawl, single score document per run, capped queries).

---

## 2. Infrastructure Declarations & Files

### A. Cloud Firestore Security Rules (`firestore.rules`)
* Strictly protects the `/leaderboards/{docId}` collection.
* Enforces native Anonymous Authentication identity verification (`request.auth.uid == request.resource.data.userId`).
* Validates honest payload parameters (`highScore <= 10000000`, `maxWave <= 1000`, `username.size() >= 3 && <= 16`, `isDevTuned == false`).
* Ensures complete score immutability (`allow update, delete: if false`).

**Deploy via Firebase CLI:**
```bash
firebase deploy --only firestore:rules
```

### B. Firestore Composite Indexes (`firestore.indexes.json`)
* Defines the high-performance composite index on `leaderboards`:
  1. `isDevTuned` (ASCENDING)
  2. `maxWave` (DESCENDING)
  3. `highScore` (DESCENDING)
  4. `timestamp` (ASCENDING)

**Deploy via Firebase CLI:**
```bash
firebase deploy --only firestore:indexes
```

### C. Remote Config Balance Template (`remoteconfig.template.json`)
* Configures live engine tuning parameters:
  * `wave_hp_multiplier`: `1.0` (Default enemy health scale)
  * `scrap_bounty_multiplier`: `1.0` (Default scrap credits from kills)
  * `boss_health_multiplier`: `1.0` (Milestone wave boss durability)
  * `harmonic_grid_damage_boost`: `0.20` (6/6 Harmonic Grid buff)

**Deploy via Firebase CLI or Console:**
```bash
firebase deploy --only remoteconfig
```
*Or in the Firebase Console:*
1. Navigate to **Run** -> **Remote Config**.
2. Click the three dots menu (**...**) in the upper right -> **Publish changes / Import template**.
3. Upload `remoteconfig.template.json` and click **Publish changes**.

### D. Firebase Anonymous Authentication Setup
1. In the [Firebase Console](https://console.firebase.google.com/project/roguetddualfront/authentication/providers):
2. Navigate to **Build** -> **Authentication** -> **Sign-in method**.
3. Under **Sign-in providers**, click **Anonymous**.
4. Toggle **Enable** to ON and click **Save**.
5. Zero PII: No user passwords, emails, or personal information are collected or stored.

---

## 3. Google Analytics for Firebase Telemetry
* Monitored lifecycle events:
  * `level_start`: Logged upon deploying waves (`wave_number`, `level_name`).
  * `level_complete`: Logged upon clearing waves (`wave_number`, `scrap_earned`).
  * `level_fail`: Captured upon run defeat with `wave_failed`, `towers_built`, `currency_remaining`, and `mvp_tower_class` to pinpoint difficulty spikes.
  * FTUE Funnel: `tutorial_start` and `tutorial_complete` tracking initial player onboarding.
