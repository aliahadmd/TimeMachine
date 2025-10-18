# 📝 GitHub Repository Rename Guide

## How to Rename Your GitHub Repository from "TimeManager" to "TimeMachine"

All files in the project have been updated to use the new name "TimeMachine". Now you need to rename the GitHub repository itself.

---

## 🔄 Steps to Rename on GitHub:

### 1. **Go to Your Repository Settings**
   - Navigate to: `https://github.com/aliahadmd/TimeManager`
   - Click on **Settings** tab (near the top)

### 2. **Rename Repository**
   - Scroll down to the **Repository name** section
   - Change `TimeManager` to `TimeMachine`
   - Click **Rename** button

### 3. **GitHub Will Automatically:**
   - ✅ Update all URLs
   - ✅ Redirect old URLs to new repository
   - ✅ Update issue references
   - ✅ Update pull request references
   - ✅ Keep all stars, watchers, and forks

### 4. **Update Your Local Git Remote**
   After renaming on GitHub, update your local repository:
   
   ```bash
   cd /Volumes/essd/TimeManager
   git remote set-url origin https://github.com/aliahadmd/TimeMachine.git
   git remote -v  # Verify the change
   ```

### 5. **Push Your Changes**
   ```bash
   git add .
   git commit -m "Rename app from PomodoroTimer to TimeMachine"
   git push origin main
   ```

---

## ✅ What's Already Been Updated in the Project:

### **App Name:**
- ✅ `strings.xml` → "TimeMachine"
- ✅ APK filename → `TimeMachine-v1.0.0.apk`

### **Documentation:**
- ✅ `README.md` → All references updated
- ✅ `QUICK_START.md` → All GitHub links updated
- ✅ `GITHUB_RELEASE_GUIDE.md` → All references updated
- ✅ `PROJECT_SUMMARY.md` → All references updated
- ✅ `RELEASE_NOTES_v1.0.0.md` → All references updated
- ✅ `RELEASE_INFO.md` → All references updated

### **GitHub References:**
- ✅ All URLs changed from `/TimeManager` to `/TimeMachine`
- ✅ All APK names changed to `TimeMachine-v*.apk`
- ✅ Repository references in all markdown files

---

## 🌐 New URLs After Rename:

| Old URL | New URL |
|---------|---------|
| `github.com/aliahadmd/TimeManager` | `github.com/aliahadmd/TimeMachine` |
| `github.com/aliahadmd/TimeManager/releases` | `github.com/aliahadmd/TimeMachine/releases` |
| `github.com/aliahadmd/TimeManager/issues` | `github.com/aliahadmd/TimeMachine/issues` |

---

## 📦 What Happens to Old Links?

**Good news!** GitHub automatically redirects:
- ✅ Old repository URL → New repository URL
- ✅ Old clone URLs → New clone URLs
- ✅ All old links continue to work
- ✅ No broken links for existing users

---

## 🎯 After Renaming:

Your app will be known as:
- **App Name:** TimeMachine
- **Repository:** github.com/aliahadmd/TimeMachine
- **APK:** TimeMachine-v1.0.0.apk
- **Download Link:** github.com/aliahadmd/TimeMachine/releases/latest

---

## ⚠️ Important Notes:

1. **Rename on GitHub first** before updating your local remote
2. **All collaborators** should update their local remotes
3. **Existing clones** will continue to work with redirects
4. **Your repository URL** will be reserved (no one can take the old name immediately)

---

## 🚀 Quick Summary:

```bash
# On GitHub.com:
# Settings → Repository name → Change to "TimeMachine" → Rename

# On your computer:
cd /Volumes/essd/TimeManager
git remote set-url origin https://github.com/aliahadmd/TimeMachine.git
git add .
git commit -m "Rename app to TimeMachine"
git push origin main
```

---

**That's it!** Your repository will be successfully renamed to TimeMachine! 🎉

