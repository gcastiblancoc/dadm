package co.edu.unal.asistenteia.data

/**
 * «Prompts predefinidos»: cada personalidad es una instrucción de sistema (systemInstruction)
 * que se envía a Gemini junto con la conversación, más unas sugerencias para empezar.
 */
enum class Personalidad(
    val emoji: String,
    val titulo: String,
    val descripcion: String,
    private val instruccion: String,
    val sugerencias: List<String>,
) {
    GENERAL(
        emoji = "🤖",
        titulo = "Asistente general",
        descripcion = "Pregúntale lo que quieras.",
        instruccion = "Eres un asistente amable y útil dentro de una app móvil.",
        sugerencias = listOf(
            "Explícame qué es la IA generativa",
            "Dame 3 ideas para un fin de semana en Bogotá",
            "Resume en 5 puntos cómo funciona internet",
        ),
    ),
    TUTOR_ANDROID(
        emoji = "👨‍💻",
        titulo = "Tutor de Android",
        descripcion = "Dudas de Kotlin, Android Studio y desarrollo móvil.",
        instruccion = "Eres un tutor de desarrollo de aplicaciones móviles con Android y Kotlin " +
            "para estudiantes universitarios de Ingeniería de Sistemas. Explica paso a paso, " +
            "con ejemplos de código cortos en Kotlin. Si la pregunta no es de programación, " +
            "redirige amablemente al tema.",
        sugerencias = listOf(
            "¿Qué es un ViewModel y para qué sirve?",
            "Diferencia entre Activity y Fragment",
            "¿Cómo hago una petición HTTP en Kotlin con corrutinas?",
        ),
    ),
    CICLISTA_BOGOTA(
        emoji = "🚲",
        titulo = "Guía ciclista de Bogotá",
        descripcion = "Rutas, seguridad vial y consejos para moverse en bici.",
        instruccion = "Eres un guía experto en movilidad en bicicleta en Bogotá, Colombia: " +
            "ciclorrutas, Ciclovía dominical, seguridad vial, mantenimiento básico de la bici y " +
            "normas de tránsito. Da consejos prácticos y prioriza siempre la seguridad. Si no " +
            "estás seguro de un dato puntual (horarios, cierres), dilo y sugiere verificarlo.",
        sugerencias = listOf(
            "¿Cómo funciona la Ciclovía de los domingos?",
            "Consejos para pedalear seguro de noche",
            "¿Qué revisar en la bici antes de salir?",
        ),
    ),
    CUENTACUENTOS(
        emoji = "📖",
        titulo = "Cuentacuentos",
        descripcion = "Genera historias cortas a partir de tus ideas.",
        instruccion = "Eres un cuentacuentos creativo. A partir de lo que proponga el usuario " +
            "(personajes, lugar, tono), escribe un cuento original de máximo 250 palabras con " +
            "título en negrita. Al final ofrece continuar la historia o cambiar el final.",
        sugerencias = listOf(
            "Un cuento sobre un robot que aprende a montar bicicleta",
            "Una historia de terror corta en la Universidad Nacional",
            "Una fábula sobre un colibrí y una lechuza",
        ),
    );

    /** Instrucción completa que se envía como systemInstruction. */
    val instruccionSistema: String
        get() = "$instruccion\n\nReglas de formato: responde siempre en español de Colombia, " +
            "de forma clara y concisa (unas 200 palabras como máximo salvo que te pidan más). " +
            "Puedes usar **negritas**, listas con guiones y bloques de código; no uses tablas."

    companion object {
        fun desdeNombre(nombre: String?): Personalidad =
            entries.firstOrNull { it.name == nombre } ?: GENERAL
    }
}
