# 🚀 Phase 8 — Deployment & Monitoring

**Repo:** `Metelci/YDS_Tracker`  
**Prereqs:** Phases 2–7 completed (audit → migration → rewiring → removal → validation → docs).  
**Mission:** Deploy the updated Material 3 app to production (or internal testers), verify stability with monitoring, and track user impact.

---

## 8.0 Goals
- Publish the migrated build to distribution channels.  
- Verify performance, stability, and UI correctness on real devices.  
- Monitor crash/error logs and analytics.  
- Gather user/tester feedback.  
- Create a `DEPLOYMENT_REPORT.md` with outcomes.

---

## 8.1 Deployment steps
1. **Prepare release build**
```bash
./gradlew clean assembleRelease
```
- Sign with production key.  
- Ensure version bump (`versionCode`, `versionName`).  

2. **Internal testing**
- Distribute via Firebase App Distribution, TestFlight (if iOS target), or GitHub Releases.  
- Have QA/devs verify smoke flows on real devices.  

3. **Production rollout**
- Publish to Google Play / relevant store.  
- Consider staged rollout (10% → 50% → 100%).  

4. **Monitoring setup**
- Crash reporting: Firebase Crashlytics / Sentry.  
- Performance metrics: startup time, frame drops, memory.  
- Log any new accessibility issues.  

5. **Feedback loop**
- Collect tester/user feedback.  
- Track regressions or complaints.  
- Open issues in GitHub for any new bugs.  

---

## 8.2 Deliverables
- **Signed release build** (APK/AAB or equivalent).  
- Rollout notes / changelog (mention Material 3 migration).  
- **`DEPLOYMENT_REPORT.md`** summarizing rollout, crashes, user feedback.  
- Monitoring dashboards set up for stability/performance.  

---

## 8.3 Definition of Done
- App live in production (or staged rollout) with no blockers.  
- Crash rate at or below pre-migration levels.  
- No major UI regressions reported.  
- `DEPLOYMENT_REPORT.md` completed and reviewed.
