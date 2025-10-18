# 📦 GitHub Release Publishing Guide

Complete step-by-step guide to publish your Pomodoro Timer app releases on GitHub.

---

## 🚀 Quick Release Checklist

- [ ] Update version code and name
- [ ] Build release APK
- [ ] Test APK on device
- [ ] Commit and tag version
- [ ] Push to GitHub
- [ ] Create GitHub release
- [ ] Upload APK file
- [ ] Write release notes
- [ ] Publish release

---

## 📋 Detailed Steps

### Step 1: Prepare New Version

#### 1.1 Update Version Numbers

Edit `app/build.gradle.kts`:

```kotlin
defaultConfig {
    applicationId = "me.aliahad.timemanager"
    minSdk = 31
    targetSdk = 36
    versionCode = 2          // Increment by 1
    versionName = "1.0.1"    // Update version string
    // ...
}
```

**Version Code**: Must increment by 1 for each release (1, 2, 3, ...)  
**Version Name**: Semantic versioning (1.0.0, 1.0.1, 1.1.0, 2.0.0)

#### 1.2 Update RELEASE_INFO.md

Update the version and release date:
```markdown
**Version:** 1.0.1
**Release Date:** October 20, 2025
```

---

### Step 2: Build Release APK

```bash
# Navigate to project directory
cd /Volumes/essd/TimeManager

# Clean previous builds
./gradlew clean

# Build release APK
./gradlew assembleRelease

# Copy and rename APK
cp app/build/outputs/apk/release/app-release.apk PomodoroTimer-v1.0.1.apk

# Verify APK was created
ls -lh PomodoroTimer-v1.0.1.apk
```

---

### Step 3: Test Release APK

**IMPORTANT**: Always test before publishing!

```bash
# Install on connected device/emulator
adb install -r PomodoroTimer-v1.0.1.apk

# Or if previous version exists
adb uninstall me.aliahad.timemanager
adb install PomodoroTimer-v1.0.1.apk

# Launch app
adb shell am start -n me.aliahad.timemanager/.MainActivity
```

**Test these features**:
- [ ] App launches correctly
- [ ] Time picker works smoothly
- [ ] Timer counts down accurately
- [ ] Alarm sound plays
- [ ] Vibration works
- [ ] Dark/light mode switches
- [ ] Stop button works
- [ ] Dismiss alarm works

---

### Step 4: Commit Changes

```bash
# Check status
git status

# Stage changes
git add app/build.gradle.kts
git add RELEASE_INFO.md
git add README.md
# ... any other modified files

# Commit
git commit -m "Release v1.0.1

- [Brief description of changes]
- [Bug fixes]
- [New features]"

# Create version tag
git tag -a v1.0.1 -m "Version 1.0.1 - [Brief description]"

# Push to GitHub
git push origin main
git push origin v1.0.1
```

**Tag Format**: `v1.0.1` (lowercase 'v' + version number)

---

### Step 5: Create GitHub Release

#### Option A: Using GitHub Web Interface

1. **Navigate to Repository**
   - Go to `https://github.com/aliahadmd/TimeManager`

2. **Access Releases**
   - Click "Releases" (right sidebar)
   - Or go to `https://github.com/aliahadmd/TimeManager/releases`

3. **Draft New Release**
   - Click "Draft a new release" button

4. **Choose Tag**
   - Click "Choose a tag" dropdown
   - Select `v1.0.1` (the tag you just pushed)
   - Or type `v1.0.1` if it doesn't appear yet

5. **Release Title**
   - Enter: `v1.0.1 - Timer Improvements` (or your description)

6. **Release Notes** (see template below)

7. **Attach APK**
   - Drag and drop `PomodoroTimer-v1.0.1.apk` to the attachments area
   - Or click "Attach binaries by dropping them here or selecting them"

8. **Preview & Publish**
   - Click "Preview" to see how it looks
   - Click "Publish release" when ready

#### Option B: Using GitHub CLI (gh)

```bash
# Install GitHub CLI if not already
# macOS: brew install gh
# Login: gh auth login

# Create release with APK
gh release create v1.0.1 \
  PomodoroTimer-v1.0.1.apk \
  --title "v1.0.1 - Timer Improvements" \
  --notes-file RELEASE_NOTES.md
```

---

### Step 6: Release Notes Template

Create `RELEASE_NOTES.md` for each version:

```markdown
## 🎉 What's New in v1.0.1

### ✨ New Features
- Added custom time presets
- Improved timer accuracy
- Enhanced UI animations

### 🐛 Bug Fixes
- Fixed timer not stopping properly
- Fixed dark mode color issues
- Resolved alarm sound looping bug

### 🔧 Improvements
- Reduced APK size by 2MB
- Better battery optimization
- Smoother scrolling in time picker
- Updated dependencies

### 📱 Technical Details
- **APK Size**: 9MB (down from 11MB)
- **Min Android**: 12 (API 31)
- **Target Android**: 15 (API 36)

---

## 📥 Installation

1. Download `PomodoroTimer-v1.0.1.apk` below
2. Enable "Install from Unknown Sources" in Settings
3. Open and install the APK

**Upgrading from v1.0.0?** Simply install over the existing app - your settings will be preserved.

---

## 🔗 Links

- **Full Changelog**: https://github.com/aliahadmd/TimeManager/compare/v1.0.0...v1.0.1
- **Documentation**: [README.md](https://github.com/aliahadmd/TimeManager/blob/main/README.md)
- **Report Issues**: [Issue Tracker](https://github.com/aliahadmd/TimeManager/issues)

---

## ⭐ Support the Project

If you find this app useful, please:
- ⭐ Star the repository
- 🐛 Report bugs
- 💡 Suggest features
- 📢 Share with friends

Thank you for using Pomodoro Timer! 🙏
```

---

## 📊 Version Numbering Guide

### Semantic Versioning (MAJOR.MINOR.PATCH)

**Format**: `vX.Y.Z`

- **MAJOR (X)**: Breaking changes, major redesign
  - Example: `v1.0.0` → `v2.0.0`
  - Changed entire app architecture
  
- **MINOR (Y)**: New features, backwards compatible
  - Example: `v1.0.0` → `v1.1.0`
  - Added timer presets feature
  
- **PATCH (Z)**: Bug fixes, small improvements
  - Example: `v1.0.0` → `v1.0.1`
  - Fixed alarm bug

### Examples

```
v1.0.0 - Initial release
v1.0.1 - Bug fixes
v1.0.2 - More bug fixes
v1.1.0 - Added new feature
v1.1.1 - Bug fix for new feature
v1.2.0 - Another new feature
v2.0.0 - Major UI redesign
```

---

## 🏷️ Release Tags Best Practices

### Good Tag Names
✅ `v1.0.0` - Clear version number  
✅ `v1.0.1` - Patch release  
✅ `v2.0.0-beta` - Beta release  
✅ `v1.5.0-rc1` - Release candidate  

### Bad Tag Names
❌ `release` - Not specific  
❌ `latest` - Ambiguous  
❌ `1.0.0` - Missing 'v' prefix  
❌ `version-1.0.0` - Unnecessary prefix  

---

## 🔒 Security Notes

### Keystore Management

**NEVER commit your keystore to GitHub!**

The `.gitignore` file excludes:
```
*.jks
*.keystore
```

**Backup your keystore safely:**
- Store in encrypted cloud storage
- Keep offline backup
- Never lose it (can't update app without it)

**Keystore location**: `app/release-keystore.jks`

### If Keystore is Exposed

If you accidentally commit the keystore:

1. **Immediately** remove from history:
   ```bash
   git filter-branch --force --index-filter \
     "git rm --cached --ignore-unmatch app/release-keystore.jks" \
     --prune-empty --tag-name-filter cat -- --all
   ```

2. **Generate new keystore**
3. **Re-sign all future releases**
4. **Users must uninstall old version** before installing new one

---

## 📈 Release Analytics

Track your releases:

1. **Download Stats**
   - Check GitHub Insights → Traffic
   - View release download counts

2. **Star History**
   - Watch repository stars grow
   - Track community interest

3. **Issue Tracker**
   - Monitor bug reports
   - Track feature requests

---

## 🤖 Automating Releases (Advanced)

### GitHub Actions Workflow

Create `.github/workflows/release.yml`:

```yaml
name: Release APK

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 11
        uses: actions/setup-java@v3
        with:
          java-version: '11'
          
      - name: Build Release APK
        run: ./gradlew assembleRelease
        
      - name: Create Release
        uses: softprops/action-gh-release@v1
        with:
          files: app/build/outputs/apk/release/*.apk
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

**Note**: You'll need to securely store keystore credentials in GitHub Secrets.

---

## 🎯 First Release Steps

For your very first release (v1.0.0):

```bash
# 1. Ensure everything is committed
git status

# 2. Create first tag
git tag -a v1.0.0 -m "Initial release - Pomodoro Timer v1.0.0"

# 3. Push tag
git push origin v1.0.0

# 4. Copy APK to root with proper name
cp app/build/outputs/apk/release/app-release.apk PomodoroTimer-v1.0.0.apk

# 5. Create GitHub release (follow Step 5 above)
```

---

## ❓ Troubleshooting

### "Tag already exists"
```bash
# Delete local tag
git tag -d v1.0.1

# Delete remote tag
git push origin :refs/tags/v1.0.1

# Create tag again
git tag -a v1.0.1 -m "Version 1.0.1"
git push origin v1.0.1
```

### "Failed to push tag"
```bash
# Ensure main branch is pushed first
git push origin main

# Then push tag
git push origin v1.0.1
```

### "APK upload failed"
- Check file size (GitHub limit: 2GB)
- Ensure APK is not corrupted
- Try using GitHub CLI instead

---

## 📚 Additional Resources

- [GitHub Releases Documentation](https://docs.github.com/en/repositories/releasing-projects-on-github)
- [Semantic Versioning](https://semver.org/)
- [Android App Versioning](https://developer.android.com/studio/publish/versioning)
- [GitHub CLI](https://cli.github.com/)

---

## ✅ Pre-Release Checklist

Before each release:

- [ ] All tests pass
- [ ] No lint errors
- [ ] Version numbers updated
- [ ] RELEASE_INFO.md updated
- [ ] README.md updated (if needed)
- [ ] APK tested on real device
- [ ] Release notes written
- [ ] Keystore backed up
- [ ] Git tag created
- [ ] APK file renamed properly

---

**Need help?** Open an issue in the repository!

Happy releasing! 🚀

