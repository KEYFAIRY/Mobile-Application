# Mobile-Application

Kotlin code of mobile application.

[Mobile Application mockups here](https://www.figma.com/proto/4QAqQ2qyKkcV0doCdqRDYu/keyfairy?node-id=1-2&t=fyl0V84YDKhtO2ov-1)

## Project structure

```bash
com.example.keyfairy
│
├── 📁 feature_auth
│   ├── 📁 data
│   │   ├── 📁 mapper        # Maps between DTOs and domain models
│   │   ├── 📁 remote
│   │   │   ├── 📁 api       # Auth API definitions (Retrofit interfaces)
│   │   │   ├── 📁 dto       # Data Transfer Objects for authentication
│   │   │   └── 📁 repository # Auth repository implementations
│   ├── 📁 domain
│   │   ├── 📁 model         # Auth-related domain entities
│   │   ├── 📁 repository    # Auth repository interfaces
│   │   └── 📁 usecase       # Business logic for authentication
│   └── 📁 presentation     # UI screens, ViewModels, adapters for Auth
│
├── 📁 feature_calibrate    # Pending structure for feature_calibrate
├── 📁 feature_practice    # Same structure as feature_auth but specific to feature_practice
├── 📁 feature_profile    # Same structure as feature_auth but specific to feature_profile
├── 📁 feature_progress    # Same structure as feature_auth but specific to feature_progress
├── 📁 feature_reports    # Same structure as feature_auth but specific to feature_reports
│
├── 📁 utils
│   ├── 📁 common            # General utilities and extensions
│   ├── 📁 network           # Retrofit, OkHttp config, interceptors
│   └── 📁 storage           # Local storage, SharedPrefs, encrypted storage
```

## Run the app

Set config.properties file (in assets folder), with the following content:

```bash
# Configuration file - DO NOT COMMIT SENSITIVE DATA
# Add this file to .gitignore if it contains production URLs

# API Configuration
base_url=actual api gateway url

# Network Configuration
connect_timeout=30
read_timeout=30
write_timeout=30

# Debug Configuration
enable_logging=true
```