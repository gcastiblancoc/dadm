# Reto 10 – Consumiendo Webservices del Catálogo de Datos Colombiano

App Android (Kotlin) que consume el servicio web del conjunto de datos
**DIVIPOLA – Códigos municipios** (DANE), publicado en [datos.gov.co](https://www.datos.gov.co/d/gdxc-w37w).
El usuario elige parámetros en la interfaz, la app los envía al servicio y muestra la respuesta.

## Servicio web

- **Endpoint:** `https://www.datos.gov.co/resource/gdxc-w37w.json` (API SODA de Socrata)
- **Columnas:** `cod_dpto`, `dpto`, `cod_mpio`, `nom_mpio`, `tipo_municipio`, `longitud`, `latitud`
- **Parámetros enviados (SoQL en la URL):**

| Parámetro en la interfaz | Parámetro del servicio |
|---|---|
| Nombre del municipio | `$where=upper(nom_mpio) like '%…%'` |
| Departamento | `$where=cod_dpto = '05'` |
| Tipo (Municipio / Isla / Área no municipalizada) | `$where=tipo_municipio = '…'` |
| Ordenar por nombre o código | `$order=nom_mpio ASC` / `$order=cod_mpio ASC` |

La lista de departamentos también se pide al servicio al abrir la app
(`$select=cod_dpto,dpto&$group=cod_dpto,dpto`). La app muestra la URL exacta de cada petición.

**Tildes:** la fuente guarda los nombres con tilde (`MEDELLÍN`) y `like` distingue tildes. Por eso
la app envía solo los fragmentos sin vocales ni Ñ (`medellin` → `like '%M%' AND like '%D%' AND like '%LL%'`)
y termina de filtrar en el teléfono sin tener en cuenta las tildes. Así «medellin», «Medellín» y
«MEDELLIN» encuentran el mismo municipio.

## Procesamiento del JSON

La respuesta es un arreglo JSON y se procesa con `org.json.JSONArray` / `JSONObject` del SDK de
Android (`DatosGovClient.kt`). Las coordenadas llegan como texto con coma decimal (`"-75,581775"`) y
se convierten a número. La petición HTTP se hace con `HttpURLConnection` en un hilo de fondo (corrutinas).

## Estructura

```
app/src/main/java/co/edu/unal/municipios/
├── MainActivity.kt          # Interfaz: parámetros, URL enviada y resultados
├── MainViewModel.kt         # Estado de la pantalla (sobrevive a la rotación)
├── data/
│   ├── DatosGovClient.kt    # Construcción de la consulta SoQL, HTTP y parseo del JSON
│   ├── Modelos.kt           # Municipio, Departamento, parámetros de la consulta
│   └── Formato.kt           # "SAN JUAN DE ARAMA" → "San Juan de Arama"
└── ui/
    ├── MunicipioAdapter.kt  # RecyclerView de resultados
    └── ListaSinFiltro.kt    # Adaptador del desplegable de departamentos
```

Al tocar un municipio se abre su ubicación en la app de mapas del teléfono.

Requiere conexión a internet. Datos: DANE – DIVIPOLA, licencia CC BY-SA 4.0.
