# CI/CD Workflows Documentation

This directory contains GitHub Actions workflows for automating the build, test, and release process of the StudyPlan Android app.

## 📋 Available Workflows

### 1. **Android CI** (`android-ci.yml`)
**Triggers**: Push to `main`, `ui-m3-migration`, `develop` branches or PRs

**Jobs**:
- **Build & Test**:
  - Runs lint checks
  - Executes unit tests
  - Builds debug and release APKs
  - Uploads build artifacts

- **Code Quality**:
  - Kotlin code style checks
  - Static analysis
  - Security vulnerability scanning

**Artifacts**:
- Lint reports (`lint-results`)
- Test results (`test-results`)
- Debug APK (`app-debug`)
- Release APK (`app-release`)

---

### 2. **PR Validation** (`pr-checks.yml`)
**Triggers**: Pull request opened, synchronized, or reopened

**Jobs**:
- **Validate**:
  - Checks PR title follows conventional commits
  - Validates CHANGELOG.md is updated
  - Runs quick lint and tests
  - Posts results as PR comment

- **Size Check**:
  - Builds release APK
  - Validates APK size (warns if >50MB)

**Purpose**: Ensures quality before merging

---

### 3. **Release Build** (`release.yml`)
**Triggers**: Push tags matching `v*` (e.g., `v2.9.38`)

**Jobs**:
- Runs full test suite
- Builds signed release APK and AAB
- Extracts version-specific CHANGELOG
- Creates GitHub Release with artifacts
- (Optional) Uploads to Play Store

**Artifacts**:
- Release APK
- Release AAB (Android App Bundle)
- Release notes from CHANGELOG

---

## 🚀 How to Use

### Regular Development
1. **Push to branch** → CI automatically runs
2. **Check GitHub Actions tab** for build status
3. **Download artifacts** from completed workflow runs

### Creating a Pull Request
1. **Open PR** → Validation automatically runs
2. **Check PR comments** for results
3. **Fix any issues** before requesting review

### Creating a Release
1. **Update version** in `app/build.gradle.kts`
2. **Update CHANGELOG.md** with version notes
3. **Commit changes**: `git commit -m "chore: bump version to 2.9.39"`
4. **Create tag**: `git tag v2.9.39`
5. **Push tag**: `git push origin v2.9.39`
6. **Release workflow runs** automatically
7. **GitHub Release** is created with APK/AAB

---

## 📊 Build Status Badges

Add these to your main README.md:

```markdown
[![Android CI](https://github.com/Metelci/YDS_Tracker/actions/workflows/android-ci.yml/badge.svg)](https://github.com/Metelci/YDS_Tracker/actions/workflows/android-ci.yml)
[![Release](https://github.com/Metelci/YDS_Tracker/actions/workflows/release.yml/badge.svg)](https://github.com/Metelci/YDS_Tracker/actions/workflows/release.yml)
```

---

## ⚙️ Configuration

### Secrets Required
For production releases, configure these GitHub secrets:

1. **RELEASE_KEYSTORE**: Base64-encoded release keystore
2. **KEYSTORE_PASSWORD**: Keystore password
3. **KEY_ALIAS**: Key alias
4. **KEY_PASSWORD**: Key password
5. **GOOGLE_SERVICES_JSON**: Firebase/Google services config (if using)

### Optional Integrations
- **Play Store Publishing**: Configure Fastlane or Google Play API
- **Slack/Discord Notifications**: Add webhook URLs
- **Code Coverage**: Integrate Codecov or similar

---

## 🔧 Customization

### Modify Build Matrix
To test on multiple Android API levels:

```yaml
strategy:
  matrix:
    api-level: [30, 31, 33, 34]
```

### Add Custom Checks
Add new jobs in `android-ci.yml`:

```yaml
security-scan:
  runs-on: ubuntu-latest
  steps:
    - name: Run security scan
      run: ./gradlew dependencyCheckAnalyze
```

### Change Trigger Branches
Edit the `on:` section:

```yaml
on:
  push:
    branches: [ main, develop, feature/* ]
```

---

## 📈 Monitoring

### View Build Results
1. Go to **Actions** tab on GitHub
2. Click on workflow run to see details
3. Download artifacts from completed runs

### Debug Failed Builds
1. Check **logs** in failed job
2. Re-run failed jobs if needed
3. Fix issues and push again

---

## 🎯 Best Practices

1. **Always run CI before merging** PRs
2. **Keep workflows fast** (< 10 minutes for CI)
3. **Cache dependencies** to speed up builds
4. **Use artifacts** for sharing build outputs
5. **Monitor build times** and optimize as needed

---

## 📝 Workflow Maintenance

### Update Actions Versions
Periodically update GitHub Actions:

```yaml
# Update from v3 to v4
uses: actions/checkout@v4
uses: actions/setup-java@v4
```

### Review and Clean Up
- Remove unused workflows
- Archive old artifacts
- Update deprecated syntax

---

## 🆘 Troubleshooting

### Build Fails on CI but Passes Locally
- Check Java version (should be 17)
- Verify Gradle wrapper is committed
- Check for environment-specific issues

### Workflow Doesn't Trigger
- Verify branch names match
- Check if workflow is disabled
- Ensure `.github/workflows/` is committed

### Artifacts Not Uploading
- Check file paths exist
- Verify artifact name is unique
- Ensure workflow has permissions

---

## 📚 Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android CI/CD Best Practices](https://developer.android.com/studio/publish/app-signing)
- [Fastlane for Android](https://fastlane.tools/)

---

**Last Updated**: 2025-10-03
**Maintained By**: Development Team
