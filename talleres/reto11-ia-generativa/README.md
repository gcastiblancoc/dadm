# Reto 11 – Integración con Inteligencia Artificial Generativa

App Android (Kotlin) tipo **chatbot** que conversa con un modelo de IA generativa de Google
(**Gemini**) a través de su API REST en la nube. Cubre dos de las funcionalidades del enunciado:

- **Chatbot:** conversación de varios turnos; el modelo "recuerda" lo dicho porque en cada
  petición se reenvía el historial.
- **Generación de texto con prompt predefinido:** cada *personalidad* es una instrucción de
  sistema fija (`systemInstruction`) que cambia el rol del modelo. La de **Cuentacuentos**
  genera historias a partir de las ideas del usuario.

| Personalidad | Qué hace |
|---|---|
| 🤖 Asistente general | Preguntas libres |
| 👨‍💻 Tutor de Android | Explica Kotlin/Android con ejemplos de código |
| 🚲 Guía ciclista de Bogotá | Ciclorrutas, Ciclovía, seguridad vial (relacionado con UrbanGO) |
| 📖 Cuentacuentos | Genera cuentos cortos a partir del prompt del usuario |

## Servicio de IA

- **Proveedor:** Gemini API (Google AI Studio), que tiene una capa gratuita.
- **Endpoint:** `POST https://generativelanguage.googleapis.com/v1beta/models/{modelo}:generateContent`
- **Autenticación:** cabecera `x-goog-api-key`.
- **Modelo por defecto:** `gemini-flash-lite-latest` (alias que Google mantiene apuntando al
  Flash-Lite más reciente). Se puede cambiar en **⋮ → Ajustes**, y el botón
  «Cargar modelos de mi clave» consulta `GET /v1beta/models` para listar los modelos que tu clave
  puede usar.

Petición que envía la app:

```json
{
  "systemInstruction": { "parts": [ { "text": "Eres un tutor de desarrollo… (prompt predefinido)" } ] },
  "contents": [
    { "role": "user",  "parts": [ { "text": "¿Qué es un ViewModel?" } ] },
    { "role": "model", "parts": [ { "text": "Un ViewModel guarda…" } ] },
    { "role": "user",  "parts": [ { "text": "¿Y cómo lo uso con corrutinas?" } ] }
  ]
}
```

La respuesta (`candidates[0].content.parts[].text`) se procesa con `org.json`; se ignoran las
partes de razonamiento interno (`"thought": true`) y se muestran los tokens usados
(`usageMetadata.totalTokenCount`). Los errores comunes (clave inválida, modelo no disponible,
cuota agotada 429, bloqueo por filtros de seguridad) se muestran en una burbuja con botón
«Reintentar». El Markdown sencillo de las respuestas (negritas, listas, código) se muestra con
estilos.

## API key (no se sube a GitHub)

1. Crea una clave gratis en <https://aistudio.google.com/apikey>.
2. Dos opciones:
   - **En `local.properties`** (en la carpeta de este reto; ese archivo está en `.gitignore`):
     ```
     GEMINI_API_KEY=tu_clave
     ```
     Se inyecta como `BuildConfig.GEMINI_API_KEY` al compilar.
   - **En la app:** ⋮ → Ajustes → pegar la clave (se guarda en `SharedPreferences` del teléfono).
     Si no hay ninguna clave, la app abre Ajustes al iniciar.

> Una clave embebida en un APK se puede extraer; para un producto real las llamadas deberían
> pasar por un backend propio. Para este reto basta con no publicar la clave en el repositorio.

## Estructura

```
app/src/main/java/co/edu/unal/asistenteia/
├── MainActivity.kt          # Chat, menú (nuevo chat, personalidad, ajustes) y diálogo de ajustes
├── MainViewModel.kt         # Conversación y estado de espera (sobrevive a la rotación)
├── data/
│   ├── GeminiClient.kt      # Petición HTTP (HttpURLConnection + corrutinas), JSON y errores
│   ├── Personalidad.kt      # Prompts de sistema predefinidos y sugerencias iniciales
│   ├── Ajustes.kt           # API key, modelo y personalidad en SharedPreferences
│   └── Modelos.kt           # Mensaje, Rol, RespuestaGemini, GeminiException
└── ui/
    ├── MensajeAdapter.kt    # RecyclerView con burbujas de usuario / Gemini / error / escribiendo
    └── Markdown.kt          # **negrita**, *cursiva*, `código`, listas y títulos → Spannable
```

Mantener presionada una burbuja copia su texto.
