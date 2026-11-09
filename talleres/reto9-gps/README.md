# Reto 9 – Accediendo al GPS

App Android (Kotlin) que obtiene la ubicación actual del usuario y muestra en un mapa de
**OpenStreetMap** los puntos de interés cercanos (hospitales, lugares turísticos, farmacias…)
dentro de un radio en kilómetros configurable desde las **preferencias**. Cada punto muestra su
nombre directamente en el mapa; al tocarlo se ve también su categoría y la distancia.

## Cómo funciona

| Pieza | Implementación |
|---|---|
| Ubicación | `FusedLocationProviderClient` (Google Play services), con permisos en tiempo de ejecución (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`). Si no hay lectura nueva se usa la última conocida. |
| Dirección | Geocodificación inversa con `Geocoder` (se muestra en la tarjeta superior junto con lat/lon). |
| Mapa | [osmdroid](https://github.com/osmdroid/osmdroid) con teselas MAPNIK de OSM, capa `MyLocationNewOverlay` (punto azul) y un círculo con el radio de búsqueda. |
| Puntos de interés | Consulta a la [Overpass API](https://wiki.openstreetmap.org/wiki/Overpass_API) con `around:<radio>,<lat>,<lon>` filtrando por etiquetas de OSM (`amenity=hospital`, `tourism=museum`, …). Se descartan duplicados y se muestran los 150 más cercanos. |
| Preferencias | `PreferenceFragmentCompat`: radio de 1 a 10 km (`SeekBarPreference`) y tipos de puntos (`MultiSelectListPreference`). Al volver de Configuración la búsqueda se repite sola. |

## Estructura

```
app/src/main/java/co/edu/unal/puntosinteres/
├── MainActivity.kt          # Permisos, GPS, mapa, marcadores
├── SettingsActivity.kt      # Pantalla de preferencias
├── data/
│   ├── Categoria.kt         # Categorías ↔ etiquetas de OpenStreetMap
│   ├── OverpassClient.kt    # Consulta HTTP a Overpass y parseo del JSON
│   ├── Preferencias.kt      # Lectura del radio y categorías
│   └── PuntoInteres.kt
└── ui/IconoPunto.kt         # Ícono del marcador con el nombre dibujado
```

## Probarlo

- **En el celular** (recomendado): activar la ubicación y aceptar el permiso.
- **En el emulador**: el emulador no tiene GPS real; fija una ubicación en
  *Extended controls (⋯) → Location* y pulsa *Set location*, luego toca **Actualizar** en la app.
  La imagen del emulador debe incluir Google Play services.

Requiere conexión a internet (teselas del mapa y Overpass API).

Datos del mapa © colaboradores de OpenStreetMap.
