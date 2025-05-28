# Sistema de Gestión de Incidencias - UAX

Este proyecto consiste en un sistema completo de gestión de incidencias que incluye tanto una aplicación web como una aplicación móvil. El sistema permite a los usuarios reportar, gestionar y dar seguimiento a incidencias de manera eficiente.

## Componentes del Sistema

### 1. Aplicación Web
- Panel de administración para gestores
- Interfaz de usuario para reportar incidencias
- Dashboard con estadísticas y métricas
- Gestión de usuarios y permisos
- Sistema de notificaciones

### 2. Aplicación Móvil
- Interfaz nativa para Android
- Reporte de incidencias en tiempo real
- Notificaciones push
- Acceso a la cámara para adjuntar fotos
- Geolocalización de incidencias

## Requisitos del Sistema

### Aplicación Web
- Node.js (v18 o superior)
- MySQL (v8.0 o superior)
- NPM o Yarn

### Aplicación Móvil
- Android Studio
- JDK 11 o superior
- Android SDK
- Dispositivo Android con API 21 o superior

## Instalación

### Aplicación Web
1. Clonar el repositorio:
```bash
git clone [URL_DEL_REPOSITORIO]
cd [NOMBRE_DEL_DIRECTORIO]
```

2. Instalar dependencias:
```bash
npm install
```

3. Configurar variables de entorno:
```bash
cp .env.example .env
# Editar .env con los valores correspondientes
```

4. Iniciar la aplicación:
```bash
npm run dev
```

### Aplicación Móvil
1. Abrir el proyecto en Android Studio
2. Sincronizar el proyecto con Gradle
3. Configurar las variables de entorno en `local.properties`
4. Ejecutar la aplicación en un emulador o dispositivo físico

## Documentación

La documentación completa del proyecto se encuentra en los siguientes directorios:

- `/docs/technical` - Documentación técnica
- `/docs/user` - Manual de usuario
- `/docs/deployment` - Guía de despliegue

## Estructura del Proyecto

```
├── web/                 # Aplicación web
│   ├── src/            # Código fuente
│   ├── public/         # Archivos estáticos
│   └── docs/           # Documentación web
├── mobile/             # Aplicación móvil
│   ├── app/            # Código fuente Android
│   └── docs/           # Documentación móvil
└── docs/               # Documentación general
    ├── technical/      # Documentación técnica
    ├── user/           # Manual de usuario
    └── deployment/     # Guía de despliegue
```

## Contribución

1. Fork el proyecto
2. Crear una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abrir un Pull Request

## Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE.md](LICENSE.md) para más detalles.

## Contacto

[Tu Nombre] - [Tu Email]

Link del proyecto: [https://github.com/tu-usuario/tu-repositorio](https://github.com/tu-usuario/tu-repositorio) 