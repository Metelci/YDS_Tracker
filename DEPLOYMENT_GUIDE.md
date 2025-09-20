# 📋 Deployment Guide — YDS Tracker Post-Migration

This guide provides step-by-step instructions for deploying the YDS Tracker app after the Material 3 migration is complete.

---

## ⚠️ Prerequisites

### Critical Requirements
Before attempting deployment, ensure:

1. **Build Success**: `./gradlew clean assembleRelease` completes without errors
2. **Core Fixes Complete**: All compilation errors from DEPLOYMENT_REPORT.md resolved
3. **Testing Complete**: Manual testing of core user flows
4. **Version Updated**: Proper version code and name incremented

### Build Verification Checklist
- [ ] Clean build succeeds (`./gradlew clean build`)
- [ ] Release build succeeds (`./gradlew clean assembleRelease`)
- [ ] No Material 2 imports remain
- [ ] All screens render correctly
- [ ] Navigation works in all flows
- [ ] Light/dark mode switching functional

---

## 🔧 Release Build Preparation

### 1. Version Management
Update version in `app/build.gradle.kts`:

```kotlin
defaultConfig {
    applicationId = "com.mtlc.studyplan"
    versionCode = 45  // Increment from current 44
    versionName = "2.6.0"  // Or increment as needed
    // ... other config
}
```

### 2. Build Release APK/AAB
```bash
# Clean build
./gradlew clean

# Build release APK
./gradlew assembleRelease

# OR build App Bundle (recommended for Play Store)
./gradlew bundleRelease
```

### 3. Verify Build Output
```bash
# Check APK location
ls -la app/build/outputs/apk/release/

# Check AAB location (if using bundleRelease)
ls -la app/build/outputs/bundle/release/
```

### 4. Test Release Build
```bash
# Install release build on test device
adb install app/build/outputs/apk/release/app-release.apk

# Launch and verify core functionality
adb shell am start -n com.mtlc.studyplan/.MainActivity
```

---

## 📱 Testing Strategy

### Phase 1: Internal Testing

#### Device Testing Matrix
Test on multiple devices/configurations:

| Device Type | OS Version | Screen Size | Theme |
|-------------|------------|-------------|-------|
| Physical Phone | Android 11 (API 30) | Small | Light |
| Physical Phone | Android 12 (API 31) | Medium | Dark |
| Physical Tablet | Android 13 (API 33) | Large | Light |
| Emulator | Android 14 (API 34) | Medium | Dark |
| Emulator | Android 15 (API 35) | Small | Light |

#### Core User Flow Testing
- [ ] **App Launch**: Clean startup, no crashes
- [ ] **Navigation**: All bottom navigation items work
- [ ] **Today Screen**: Tasks display, interactions work
- [ ] **Tasks Screen**: Task management functions
- [ ] **Progress Screen**: Charts and stats display
- [ ] **Settings**: All settings accessible and functional
- [ ] **Theme Switching**: Light/dark mode transitions
- [ ] **Pull-to-Refresh**: Works in all applicable screens

#### Material 3 Specific Testing
- [ ] **Navigation Components**: NavigationBar styling correct
- [ ] **Tab Components**: PrimaryTabRow functions properly
- [ ] **Card Components**: Material 3 styling applied
- [ ] **Button Components**: All button variants work
- [ ] **Color Scheme**: Proper Material 3 colors applied
- [ ] **Typography**: Material 3 text styles consistent
- [ ] **Accessibility**: Content descriptions present, touch targets adequate

### Phase 2: Beta Testing

#### Firebase App Distribution Setup
1. **Configure Firebase Project**
   ```bash
   # Install Firebase CLI
   npm install -g firebase-tools

   # Login and initialize
   firebase login
   firebase init
   ```

2. **Upload Release Build**
   ```bash
   firebase appdistribution:distribute app/build/outputs/apk/release/app-release.apk \
     --app YOUR_FIREBASE_APP_ID \
     --groups "internal-testers" \
     --release-notes "Material 3 migration - Beta release for testing"
   ```

#### Beta Testing Checklist
- [ ] Distribute to 5-10 internal testers
- [ ] Collect feedback on UI changes
- [ ] Monitor crash reports
- [ ] Test on various device configurations
- [ ] Verify no major regressions

---

## 🚀 Production Deployment

### Google Play Console Setup

#### 1. Prepare Release Notes
```markdown
# Version 2.6.0 Release Notes

## 🎨 Material 3 Design Upgrade
We've completely redesigned the app with Google's latest Material 3 design system:
- Modern, consistent visual design throughout the app
- Enhanced navigation with updated components
- Improved accessibility and touch interactions
- Better dark mode experience with dynamic theming

## ✨ What's New
- Refreshed user interface with contemporary design language
- Smoother animations and improved visual feedback
- Enhanced color palette optimized for readability
- Updated typography for better content hierarchy

## 🔧 Technical Improvements
- Upgraded to latest Android design components
- Improved app performance and stability
- Enhanced accessibility features
- Better support for various screen sizes

## 📱 Compatibility
- Requires Android 11+ (API level 30)
- Optimized for all screen sizes
- Supports system-wide dark mode
```

#### 2. Upload to Play Console
1. **Create New Release**
   - Go to Google Play Console
   - Select "Production" track
   - Create new release

2. **Upload App Bundle**
   ```bash
   # Upload the AAB file generated by:
   ./gradlew bundleRelease
   ```

3. **Configure Release**
   - Add release notes
   - Set rollout percentage (start with 10%)
   - Configure device targeting if needed

#### 3. Staged Rollout Strategy
```
Day 1: 10% rollout
- Monitor crash rates
- Check user feedback
- Verify core functionality

Day 3: 50% rollout (if metrics are good)
- Continue monitoring
- Address any reported issues

Day 7: 100% rollout
- Full deployment
- Ongoing monitoring
```

---

## 📊 Monitoring Setup

### Firebase Crashlytics Integration

#### 1. Add Dependencies
In `app/build.gradle.kts`:
```kotlin
dependencies {
    // Firebase
    implementation platform('com.google.firebase:firebase-bom:32.2.0')
    implementation 'com.google.firebase:firebase-crashlytics-ktx'
    implementation 'com.google.firebase:firebase-analytics-ktx'
    implementation 'com.google.firebase:firebase-perf-ktx'
}
```

#### 2. Initialize in Application Class
```kotlin
class StudyPlanApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Enable Crashlytics collection
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
    }
}
```

#### 3. Custom Crash Tracking
```kotlin
// Track Material 3 specific errors
try {
    // Material 3 component usage
} catch (e: Exception) {
    FirebaseCrashlytics.getInstance().recordException(e)
    FirebaseCrashlytics.getInstance().setCustomKey("material3_component", "NavigationBar")
}
```

### Key Metrics Dashboard

#### Critical Metrics to Monitor
1. **Crash Rate**: Target < 1%
2. **ANR Rate**: Target < 0.1%
3. **App Start Time**: Target < 3 seconds
4. **Memory Usage**: Monitor for leaks
5. **User Retention**: Compare to previous version

#### Alert Configuration
Set up alerts for:
- Crash rate > 2%
- ANR rate > 0.5%
- Significant performance degradation
- User complaints about Material 3 UI

---

## 🔄 Rollback Procedures

### Immediate Rollback Triggers
- Crash rate exceeds 2%
- Critical functionality broken
- Widespread user complaints
- Performance significantly degraded

### Rollback Steps
1. **Stop Staged Rollout**
   - Pause rollout in Play Console
   - Prevent new users from getting update

2. **Assess Impact**
   - Review crash reports
   - Analyze user feedback
   - Determine scope of issues

3. **Execute Rollback** (if necessary)
   - Revert to previous stable version
   - Communicate with affected users
   - Plan hotfix for issues

4. **Post-Rollback Actions**
   - Analyze root cause
   - Fix identified issues
   - Plan re-deployment strategy

---

## 📈 Success Metrics

### 24-Hour Success Criteria
- [ ] Crash rate ≤ baseline (previous version)
- [ ] No critical user flow failures
- [ ] App store rating maintained
- [ ] No emergency rollback required

### 7-Day Success Criteria
- [ ] User retention rate maintained or improved
- [ ] Positive feedback on Material 3 design
- [ ] Performance metrics within acceptable range
- [ ] Feature adoption rates normal

### 30-Day Success Criteria
- [ ] Long-term stability demonstrated
- [ ] User satisfaction with new design
- [ ] No degradation in core metrics
- [ ] Successful foundation for future updates

---

## 📞 Support & Communication

### Internal Communication
- **Slack/Teams Channel**: #deployment-material3
- **Email Updates**: Send status to stakeholders
- **Meeting Schedule**: Daily standups during rollout

### User Communication
- **Play Store**: Release notes explaining changes
- **In-App**: Optional onboarding for Material 3 features
- **Support**: Updated help documentation

### Emergency Contacts
- **Technical Lead**: For critical issues
- **Product Manager**: For user impact decisions
- **DevOps/Release**: For deployment issues

---

## 🔗 Related Documentation

- **DEPLOYMENT_REPORT.md**: Current status and blockers
- **MIGRATION_LESSONS.md**: Migration insights and learnings
- **DEVELOPING.md**: Material 3 development guidelines
- **DESIGN_SYSTEM.md**: Design system documentation

---

**⚠️ Remember: Only proceed with deployment after all compilation errors are resolved and thorough testing is complete.**