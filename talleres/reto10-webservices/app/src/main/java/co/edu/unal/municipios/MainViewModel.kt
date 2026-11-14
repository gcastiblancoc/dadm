package co.edu.unal.municipios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.unal.municipios.data.ConsultaMunicipios
import co.edu.unal.municipios.data.DatosGovClient
import co.edu.unal.municipios.data.Departamento
import co.edu.unal.municipios.data.Municipio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Mantiene el estado de la pantalla (y las peticiones en curso) aunque se rote el dispositivo.
 */
class MainViewModel : ViewModel() {

    sealed interface Estado {
        data object Inicial : Estado
        data class Cargando(val url: String) : Estado
        data class Exito(val url: String, val municipios: List<Municipio>) : Estado
        data class Error(val url: String, val mensaje: String) : Estado
    }

    sealed interface EstadoDepartamentos {
        data object Cargando : EstadoDepartamentos
        data class Listos(val departamentos: List<Departamento>) : EstadoDepartamentos
        data object Error : EstadoDepartamentos
    }

    private val _estado = MutableStateFlow<Estado>(Estado.Inicial)
    val estado: StateFlow<Estado> = _estado.asStateFlow()

    private val _departamentos = MutableStateFlow<EstadoDepartamentos>(EstadoDepartamentos.Cargando)
    val departamentos: StateFlow<EstadoDepartamentos> = _departamentos.asStateFlow()

    /** Departamento elegido en el desplegable (null = todos). */
    var departamentoSeleccionado: Departamento? = null

    private var consultaActual: Job? = null
    private var ultimaConsulta: ConsultaMunicipios? = null

    init {
        cargarDepartamentos()
    }

    fun cargarDepartamentos() {
        _departamentos.value = EstadoDepartamentos.Cargando
        viewModelScope.launch {
            _departamentos.value = try {
                EstadoDepartamentos.Listos(
                    withContext(Dispatchers.IO) { DatosGovClient.obtenerDepartamentos() }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                EstadoDepartamentos.Error
            }
        }
    }

    fun consultar(consulta: ConsultaMunicipios) {
        ultimaConsulta = consulta
        consultaActual?.cancel()

        val url = DatosGovClient.urlLegible(consulta)
        _estado.value = Estado.Cargando(url)

        consultaActual = viewModelScope.launch {
            _estado.value = try {
                val resultado = withContext(Dispatchers.IO) { DatosGovClient.buscarMunicipios(consulta) }
                Estado.Exito(resultado.url, resultado.municipios)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Estado.Error(url, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun reintentar() {
        ultimaConsulta?.let { consultar(it) }
    }
}
