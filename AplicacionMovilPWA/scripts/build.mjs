/**
 * ============================================================================
 * Archivo: scripts/build.mjs
 * Propósito:
 *   Script de empaquetado (build) de la Aplicación Móvil PWA "AutoUTA".
 *   Como la PWA está construida con módulos ES nativos del navegador, HTML5 y
 *   CSS3 sin empaquetador (bundler) externo, el proceso de compilación consiste
 *   en validar la existencia del punto de entrada y generar una copia limpia de
 *   todos los activos estáticos de `public/` dentro de `dist/` listos para
 *   despliegue o vista previa (`npm run preview`).
 *
 * Actividades de la guía APE 04 implementadas:
 *   - [Actividad 6] Construcción y verificación de artefactos estáticos de la
 *     PWA instalable (manifest, iconos, Service Worker, cliente HTTP y vistas).
 *   - [Actividad 12 - Mejora Visual AutoUTA v5] Publicación hacia `dist/` de
 *     los recursos visuales actualizados de AutoUTA v5 Minimalista Claro
 *     (`index.html`, `styles.css`, `main.js`, `sw.js`, `icons/hero-auto-uta.png`).
 * ============================================================================
 */

// [Actividad 6] Importa operaciones asíncronas del sistema de archivos de Node.js para copiar, crear, eliminar y verificar rutas.
import { cp, mkdir, rm, stat } from 'node:fs/promises';
// [Actividad 6] Importa `resolve` para construir rutas absolutas portables independientes del sistema operativo.
import { resolve } from 'node:path';

// [Actividad 6] Resuelve la ruta absoluta de la carpeta fuente que contiene los archivos de la PWA (`public/`).
const origen = resolve('public');
// [Actividad 6] Resuelve la ruta absoluta de la carpeta de salida de distribución (`dist/`).
const destino = resolve('dist');
// [Actividad 6] Verifica que exista `public/index.html` antes de compilar; si falta, aborta con error.
await stat(resolve(origen, 'index.html')); // Verifica que existe el proyecto.
// [Actividad 6] Elimina recursivamente cualquier compilación previa en `dist/` para evitar archivos obsoletos.
await rm(destino, { recursive: true, force: true });
// [Actividad 6] Crea nuevamente el directorio `dist/` (incluyendo directorios intermedios si fueran necesarios).
await mkdir(destino, { recursive: true });
// [Actividad 6] [Actividad 12 - Mejora Visual AutoUTA v5] Copia recursivamente todos los activos de `public/` (HTML, CSS v5, JS, SW v5, manifest e iconos) hacia `dist/`.
await cp(origen, destino, { recursive: true });
// [Actividad 6] Informa en consola que los artefactos estáticos fueron generados correctamente en `dist/`.
console.log('Compilación completada: archivos publicados en dist/');
// [Actividad 6] Muestra al operador el comando npm para levantar la vista previa local desde `dist/`.
console.log('Vista previa local: npm run preview');
