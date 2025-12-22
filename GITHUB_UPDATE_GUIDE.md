# GitHub Repository Update Guide

## Current Description
```
YDS Exam Tracker for 6+1 months from A2 to C Level
```

## Recommended New Description
```
Modern Android app for YDS & YÖKDİL exam preparation with 30-week structured curriculum, AI-powered analytics, live exam tracking, and gamified progress system. Built with Jetpack Compose & Material 3.
```

## Topics to Add
Add these topics to improve discoverability:

- `android`
- `kotlin`
- `jetpack-compose`
- `material-design`
- `yds`
- `yokdil`
- `exam-preparation`
- `study-planner`
- `education`
- `mvvm`
- `room-database`
- `koin`
- `material3`
- `android-app`
- `study-tracker`

## About Section Details

### Website
Leave empty or add: (none currently)

### Topics
See list above (15 topics recommended)

### Releases
Latest: v2.9.74 (December 23, 2025)

### Packages
None

### Used By
Public count

---

## How to Update on GitHub

### Method 1: Via GitHub Web Interface
1. Go to https://github.com/Metelci/YDS_Tracker
2. Click the ⚙️ gear icon next to "About" (top right of page)
3. Update the description field with the new text
4. Add topics by typing them in the "Topics" field
5. Click "Save changes"

### Method 2: Via GitHub CLI (if installed)
```bash
# Update description
gh repo edit Metelci/YDS_Tracker --description "Modern Android app for YDS & YÖKDİL exam preparation with 30-week structured curriculum, AI-powered analytics, live exam tracking, and gamified progress system. Built with Jetpack Compose & Material 3."

# Add topics
gh repo edit Metelci/YDS_Tracker --add-topic android,kotlin,jetpack-compose,material-design,yds,yokdil,exam-preparation,study-planner,education,mvvm,room-database,koin,material3,android-app,study-tracker
```

### Method 3: Via GitHub API (requires personal access token)
```bash
curl -X PATCH \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  https://api.github.com/repos/Metelci/YDS_Tracker \
  -d '{
    "description": "Modern Android app for YDS & YÖKDİL exam preparation with 30-week structured curriculum, AI-powered analytics, live exam tracking, and gamified progress system. Built with Jetpack Compose & Material 3.",
    "topics": ["android", "kotlin", "jetpack-compose", "material-design", "yds", "yokdil", "exam-preparation", "study-planner", "education", "mvvm", "room-database", "koin", "material3", "android-app", "study-tracker"]
  }'
```

---

## Key Features to Highlight (for README badges or description)

✅ **30-Week Structured Curriculum** - Raymond Murphy methodology  
✅ **AI-Powered Analytics** - Smart recommendations & pattern recognition  
✅ **Live Exam Tracking** - YDS & YÖKDİL countdown with ÖSYM integration  
✅ **Gamification System** - XP, achievements, streaks, and rewards  
✅ **Offline-First** - AES-256 encrypted local storage  
✅ **Material 3 Design** - Modern, beautiful, accessible UI  
✅ **Biometric Security** - Fingerprint/Face authentication  
✅ **Bilingual** - Full English & Turkish localization  

---

## Social Preview Image Recommendations

Consider creating a social preview image (1280x640px) showing:
- App icon/logo
- Key feature screenshots (analytics dashboard, study plan, achievements)
- Version number and "Built with Jetpack Compose" badge
- YDS/YÖKDİL branding elements

Upload via: Repository Settings → Social Preview → Upload an image
