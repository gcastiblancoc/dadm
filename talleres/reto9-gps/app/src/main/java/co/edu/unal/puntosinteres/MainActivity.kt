package co.edu.unal.puntosinteres

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import co.edu.unal.puntosinteres.data.Categoria
import co.edu.unal.puntosinteres.data.OverpassClient
import co.edu.unal.puntosinteres.data.Preferencias
import co.edu.unal.puntosinteres.data.PuntoInteres
import co.edu.unal.puntosinteres.ui.IconoPunto
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.util.Locale

/**
 * Pantalla principal: obtiene la ubicación del usuario con el GPS, muestra su dirección y
 * pone en un mapa de OpenStreetMap los puntos de interés que hay dentro del radio (en km)
 * configurado en las preferencias, cada uno con su nombre.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var textDireccion: TextView
    private lateinit var textCoordenadas: TextView
    private lateinit var textResumen: TextView
    private lateinit var progreso: LinearProgressIndicator
    private lateinit var fabMiUbicacion: FloatingActionButton

    private lateinit var ubicacionClient: FusedLocationProviderClient

    private var capaMiUbicacion: MyLocationNewOverlay? = null
    private var capaPuntos: FolderOverlay? = null
    private var circuloRadio: Polygon? = null

    private var ubicacionActual: GeoPoint? = null
    private var busquedaActual: Job? = null
    private var snackbarError: Snackbar? = null

    /** Radio y categorías de la última búsqueda, para saber si cambiaron las preferencias. */
    private var firmaBusqueda: String? = null

    private val pedirPermisos = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        if (resultado.values.any { it }) obtenerUbicacion() else mostrarPermisoDenegado()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // osmdroid exige identificar la app (User-Agent) ante los servidores de teselas de OSM
        Configuration.getInstance().apply {
            load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
        }
        PreferenceManager.setDefaultValues(this, R.xml.preferencias, false)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))

        map = findViewById(R.id.map)
        textDireccion = findViewById(R.id.text_direccion)
        textCoordenadas = findViewById(R.id.text_coordenadas)
        textResumen = findViewById(R.id.text_resumen)
        progreso = findViewById(R.id.progreso)
        fabMiUbicacion = findViewById(R.id.fab_mi_ubicacion)

        ubicacionClient = LocationServices.getFusedLocationProviderClient(this)

        configurarMapa()

        fabMiUbicacion.setOnClickListener {
            val ubicacion = ubicacionActual
            if (ubicacion != null) {
                encuadrarRadio(ubicacion, Preferencias.radioKm(prefs()))
            } else {
                verificarPermisos()
            }
        }

        if (savedInstanceState == null || tienePermiso()) {
            verificarPermisos()
        }
    }

    private fun configurarMapa() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
        map.isTilesScaledToDpi = true
        map.controller.setZoom(14.0)
        // Mientras llega el GPS se muestra la Universidad Nacional, en Bogotá
        map.controller.setCenter(GeoPoint(4.6381, -74.0840))
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        capaMiUbicacion?.enableMyLocation()

        // Al volver de Configuración se repite la búsqueda si cambió el radio o las categorías
        val ubicacion = ubicacionActual
        if (ubicacion != null && firmaPreferencias() != firmaBusqueda) {
            buscarPuntos(ubicacion, encuadrar = true)
        }
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        capaMiUbicacion?.disableMyLocation()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_actualizar -> {
            verificarPermisos()
            true
        }
        R.id.action_configuracion -> {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    // ---------------------------------------------------------------------------------------
    // Permisos y ubicación
    // ---------------------------------------------------------------------------------------

    private fun tienePermiso(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun verificarPermisos() {
        if (tienePermiso()) {
            obtenerUbicacion()
        } else {
            pedirPermisos.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun mostrarPermisoDenegado() {
        ocultarCargando()
        textDireccion.setText(R.string.permiso_titulo)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permiso_titulo)
            .setMessage(R.string.permiso_explicacion)
            .setPositiveButton(R.string.abrir_ajustes) { _, _ ->
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null)
                    )
                )
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    @SuppressLint("MissingPermission") // Solo se llama después de verificar el permiso
    private fun obtenerUbicacion() {
        snackbarError?.dismiss()
        mostrarCargando()
        if (ubicacionActual == null) textDireccion.setText(R.string.buscando_ubicacion)
        activarCapaMiUbicacion()

        ubicacionClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { ubicacion ->
                if (ubicacion != null) alRecibirUbicacion(ubicacion) else usarUltimaUbicacion()
            }
            .addOnFailureListener { usarUltimaUbicacion() }
    }

    /** Si el GPS no da una lectura nueva (p. ej. está apagado), se usa la última conocida. */
    @SuppressLint("MissingPermission")
    private fun usarUltimaUbicacion() {
        ubicacionClient.lastLocation
            .addOnSuccessListener { ubicacion ->
                if (ubicacion != null) alRecibirUbicacion(ubicacion) else errorUbicacion()
            }
            .addOnFailureListener { errorUbicacion() }
    }

    private fun errorUbicacion() {
        ocultarCargando()
        if (ubicacionActual == null) textDireccion.setText(R.string.tu_ubicacion)
        mostrarError(R.string.error_ubicacion) { verificarPermisos() }
    }

    /** Punto azul con la posición del usuario, que se mueve si el usuario se mueve. */
    private fun activarCapaMiUbicacion() {
        if (capaMiUbicacion != null) return
        capaMiUbicacion = MyLocationNewOverlay(GpsMyLocationProvider(this), map).also {
            it.enableMyLocation()
            map.overlays.add(it)
        }
    }

    private fun alRecibirUbicacion(ubicacion: Location) {
        val punto = GeoPoint(ubicacion.latitude, ubicacion.longitude)
        ubicacionActual = punto

        textCoordenadas.text = getString(R.string.coordenadas, punto.latitude, punto.longitude)
        textCoordenadas.visibility = View.VISIBLE
        mostrarDireccion(punto)

        buscarPuntos(punto, encuadrar = true)
    }

    /** Convierte las coordenadas en una dirección legible (geocodificación inversa). */
    private fun mostrarDireccion(punto: GeoPoint) {
        if (!Geocoder.isPresent()) {
            textDireccion.setText(R.string.tu_ubicacion)
            return
        }
        // Texto provisional por si el servicio de direcciones no responde
        textDireccion.setText(R.string.tu_ubicacion)
        val geocoder = Geocoder(this, Locale.getDefault())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(punto.latitude, punto.longitude, 1) { direcciones ->
                runOnUiThread { pintarDireccion(direcciones.firstOrNull()) }
            }
        } else {
            lifecycleScope.launch {
                val direccion = withContext(Dispatchers.IO) {
                    runCatching {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(punto.latitude, punto.longitude, 1)?.firstOrNull()
                    }.getOrNull()
                }
                pintarDireccion(direccion)
            }
        }
    }

    private fun pintarDireccion(direccion: Address?) {
        if (isDestroyed) return
        textDireccion.text = direccion?.getAddressLine(0) ?: getString(R.string.tu_ubicacion)
    }

    // ---------------------------------------------------------------------------------------
    // Búsqueda y dibujo de los puntos de interés
    // ---------------------------------------------------------------------------------------

    private fun prefs() = PreferenceManager.getDefaultSharedPreferences(this)

    private fun firmaPreferencias(): String {
        val p = prefs()
        return "${Preferencias.radioKm(p)}|${Preferencias.categorias(p).joinToString(",") { it.clave }}"
    }

    private fun buscarPuntos(centro: GeoPoint, encuadrar: Boolean) {
        val radioKm = Preferencias.radioKm(prefs())
        val categorias = Preferencias.categorias(prefs())
        firmaBusqueda = firmaPreferencias()

        dibujarRadio(centro, radioKm)
        if (encuadrar) encuadrarRadio(centro, radioKm)

        busquedaActual?.cancel()
        snackbarError?.dismiss()

        if (categorias.isEmpty()) {
            pintarPuntos(emptyList())
            textResumen.setText(R.string.sin_categorias)
            textResumen.visibility = View.VISIBLE
            ocultarCargando()
            return
        }

        mostrarCargando()
        textResumen.setText(R.string.buscando_puntos)
        textResumen.visibility = View.VISIBLE

        busquedaActual = lifecycleScope.launch {
            try {
                val puntos = withContext(Dispatchers.IO) {
                    OverpassClient.buscar(centro.latitude, centro.longitude, radioKm * 1000, categorias)
                }
                pintarPuntos(puntos)
                ocultarCargando()
                textResumen.text = getString(
                    R.string.resumen_resultados,
                    resources.getQuantityString(R.plurals.puntos_encontrados, puntos.size, puntos.size),
                    radioKm
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error consultando Overpass", e)
                ocultarCargando()
                textResumen.visibility = View.GONE
                firmaBusqueda = null // Para que se reintente al volver a la app
                mostrarError(R.string.error_puntos) { buscarPuntos(centro, encuadrar = false) }
            }
        }
    }

    /** Círculo que marca el radio de búsqueda alrededor del usuario. */
    private fun dibujarRadio(centro: GeoPoint, radioKm: Int) {
        circuloRadio?.let { map.overlays.remove(it) }
        val circulo = Polygon(map).apply {
            setPoints(Polygon.pointsAsCircle(centro, radioKm * 1000.0))
            fillPaint.color = ContextCompat.getColor(this@MainActivity, R.color.radio_relleno)
            outlinePaint.color = ContextCompat.getColor(this@MainActivity, R.color.radio_borde)
            outlinePaint.strokeWidth = 2f * resources.displayMetrics.density
            infoWindow = null
        }
        map.overlays.add(0, circulo) // Debajo de todo lo demás
        circuloRadio = circulo
        map.invalidate()
    }

    private fun encuadrarRadio(centro: GeoPoint, radioKm: Int) {
        val caja = BoundingBox.fromGeoPoints(Polygon.pointsAsCircle(centro, radioKm * 1000.0))
        val margen = (24 * resources.displayMetrics.density).toInt()
        // Se espera a que el mapa tenga tamaño para poder calcular el zoom
        map.post { map.zoomToBoundingBox(caja, true, margen) }
    }

    private fun pintarPuntos(puntos: List<PuntoInteres>) {
        InfoWindow.closeAllInfoWindowsOn(map)
        capaPuntos?.let { map.overlays.remove(it) }

        val capa = FolderOverlay()
        val colores = HashMap<Categoria, Int>()
        // Se agregan del más lejano al más cercano para que los cercanos queden encima
        for (punto in puntos.asReversed()) {
            val color = colores.getOrPut(punto.categoria) {
                ContextCompat.getColor(this, punto.categoria.color)
            }
            val icono = IconoPunto.crear(this, punto.nombre, color)
            val marcador = Marker(map).apply {
                position = GeoPoint(punto.latitud, punto.longitud)
                title = punto.nombre
                snippet = "${getString(punto.categoria.nombre)} · ${formatoDistancia(punto.distanciaM)}"
                icon = icono.icono
                setAnchor(Marker.ANCHOR_CENTER, icono.anclaY)
            }
            capa.add(marcador)
        }

        map.overlays.add(capa)
        capaPuntos = capa
        map.invalidate()
    }

    private fun formatoDistancia(metros: Double): String =
        if (metros < 1000) "${metros.toInt()} m"
        else String.format(Locale.getDefault(), "%.1f km", metros / 1000)

    // ---------------------------------------------------------------------------------------
    // Estado de la interfaz
    // ---------------------------------------------------------------------------------------

    private fun mostrarCargando() {
        progreso.visibility = View.VISIBLE
    }

    private fun ocultarCargando() {
        progreso.visibility = View.GONE
    }

    private fun mostrarError(@StringRes mensaje: Int, reintentar: () -> Unit) {
        snackbarError?.dismiss()
        snackbarError = Snackbar.make(map, mensaje, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.reintentar) { reintentar() }
            .setAnchorView(fabMiUbicacion)
            .also { it.show() }
    }

    companion object {
        private const val TAG = "PuntosInteres"
    }
}
