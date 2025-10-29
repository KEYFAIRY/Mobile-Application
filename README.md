# Mobile-Application

Kotlin code of mobile application.

[Mobile Application mockups here](https://www.figma.com/proto/4QAqQ2qyKkcV0doCdqRDYu/keyfairy?node-id=1-2&t=fyl0V84YDKhtO2ov-1)

## Project structure

```bash
com.example.keyfairy
│
├── 📁 feature_auth                  # Login and account creation
├── 📁 feature_practice              # Scale selection for practice
├── 📁 feature_calibrate             # Camera calibration
├── 📁 feature_practice_execution    # Practice recording
├── 📁 feature_check_video           # Preview practice video before sending
├── 📁 feature_profile               # User profile
├── 📁 feature_home                  # Main screen
├── 📁 feature_progress              # Practice statistics
├── 📁 feature_reports               # Practice history with reports
│
├── 📁 utils
│   ├── 📁 common           # Common utilities and extensions
│   ├── 📁 network          # Retrofit, OkHttp, interceptors
│   ├── 📁 enums            # Application enums
│   ├── 📁 worker           # Work Manager configuration and functionality
│   └── 📁 storage          # Local storage
│
├── KeyFairyApplication.kt  # Initializes the application
└── MainActivity.kt         # Main activity
```

Each feature folder has the following structure:

```bash
📁 feature_<module>
├── 📁 data               # Data layer
│   ├── 📁 mapper         # Maps DTOs to domain entities
│   ├── 📁 remote         # Communication with the backend
│   │   ├── 📁 api        # Interfaces for making backend requests
│   │   └── 📁 dto        # DTOs for receiving backend responses
│   └── 📁 repository     # Repository implementations for data operations
├── 📁 domain             # Domain layer
│   ├── 📁 model          # Domain models
│   ├── 📁 repository     # Interfaces for data operations
│   └── 📁 use_case       # Use cases (business logic)
├── 📁 presentation       # Presentation layer
│   ├── 📁 activity       # Activities
│   ├── 📁 fragment       # Fragments
│   ├── 📁 state          # UI states
│   ├── 📁 viewmodel      # ViewModels for managing UI states
│   └── 📁 adapter        # Adapters
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