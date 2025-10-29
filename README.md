# Mobile-Application

Kotlin code of mobile application.

[Mobile Application mockups here](https://www.figma.com/proto/4QAqQ2qyKkcV0doCdqRDYu/keyfairy?node-id=1-2&t=fyl0V84YDKhtO2ov-1)

## Project structure

```bash
com.example.keyfairy
│
├── 📁 feature_auth                  # Inicios de sesión y creación de cuenta
├── 📁 feature_practice              # Selección de escala a practicar
├── 📁 feature_calibrate             # Calibración de cámara
├── 📁 feature_practice_execution    # Grabación de práctica
├── 📁 feature_check_video           # Ver video de práctica antes de enviarlo
├── 📁 feature_profile               # Perfil del usuario
├── 📁 feature_home                  # Pantalla principal
├── 📁 feature_progress              # Estadísticas de las práctias
├── 📁 feature_reports               # Historial de prácticas con sus reportes
│
├── 📁 utils
│   ├── 📁 common           # Utilidades y extensiones comunes
│   ├── 📁 network          # Retrofit, OkHttp, interceptores
│   ├── 📁 enums            # Enums de la aplicación
│   ├── 📁 worker           # Configuración y funcionalidad del Work Manager
│   └── 📁 storage          # Almacenamiento local
│
├── KeyFairyApplication.kt  # Inicializa la aplicación
└── MainActivity.kt         # Actividad principal
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