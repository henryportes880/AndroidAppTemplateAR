package com.ifpr.androidapptemplatear.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.ifpr.androidapptemplatear.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** Integração de feature-location: só acompanha a localização com a tela visível. */
class LocalizacaoController(private val fragment: Fragment) : DefaultLifecycleObserver, LocationListener {
    private var manager: LocationManager? = null
    private var owner: LifecycleOwner? = null
    private var painel: LinearLayout? = null
    private var enderecoView: TextView? = null
    private var qualidadeView: TextView? = null
    private var encontroView: TextView? = null
    private var botao: MaterialButton? = null
    private var aoLocalizar: ((Map<String, Float>, Long, Float?, List<String>) -> GameViewModel.Encontro?)? = null
    private var modelos = emptyList<String>()
    private var monitorando = false
    private var encerrado = false
    private var permissaoSolicitada = false
    private var enderecoJob: Job? = null
    private var esperaLocalizacaoJob: Job? = null
    private var ultimaLocalizacao: Location? = null
    private var ultimaConsultaEndereco: Location? = null
    private var instanteConsultaEndereco = 0L

    private val permissoes = fragment.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (visivel()) {
            if (temPermissaoPrecisa()) iniciar()
            else mostrarAcao(R.string.location_permission_denied, R.string.location_allow)
        }
    }

    fun conectar(
        parent: ViewGroup,
        aoLocalizar: (Map<String, Float>, Long, Float?, List<String>) -> GameViewModel.Encontro?
    ) {
        check(owner == null) { "O controlador já está conectado a uma tela." }
        val context = fragment.requireContext()
        this.aoLocalizar = aoLocalizar
        owner = fragment.viewLifecycleOwner
        manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        modelos = context.assets.list("").orEmpty()
            .filter { it.endsWith(".glb", ignoreCase = true) }.sorted()
        criarPainel(parent)
        owner?.lifecycle?.addObserver(this)
    }

    private fun criarPainel(parent: ViewGroup) {
        val context = fragment.requireContext()
        val novoPainel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        enderecoView = TextView(context).apply {
            id = R.id.textEndereco
            setText(R.string.location_activate)
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.myth_text))
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        qualidadeView = TextView(context).apply {
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.myth_accent))
            visibility = View.GONE
        }
        encontroView = TextView(context).apply {
            setText(R.string.location_discovery_waiting)
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.myth_muted))
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        botao = MaterialButton(context).apply {
            setText(R.string.location_allow)
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.button_secondary_tint)
            setTextColor(ContextCompat.getColor(context, R.color.myth_accent))
            strokeColor = ContextCompat.getColorStateList(context, R.color.myth_outline)
            strokeWidth = dp(1)
            setOnClickListener {
                if (encerrado || !visivel()) return@setOnClickListener
                if (temPermissaoPrecisa()) {
                    parar()
                    iniciar()
                } else if (permissaoSolicitada && !fragment.shouldShowRequestPermissionRationale(
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    fragment.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        "package:${context.packageName}".toUri()))
                } else {
                    permissaoSolicitada = true
                    permissoes.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION))
                }
            }
        }
        novoPainel.addView(enderecoView)
        novoPainel.addView(qualidadeView)
        novoPainel.addView(encontroView)
        novoPainel.addView(botao)
        parent.addView(novoPainel, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        painel = novoPainel
    }

    private fun visivel() = owner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true

    override fun onStart(owner: LifecycleOwner) {
        if (encerrado) return
        if (temPermissaoPrecisa()) iniciar()
        else mostrarAcao(R.string.location_permission_required, R.string.location_allow)
    }

    override fun onStop(owner: LifecycleOwner) = parar()

    override fun onDestroy(owner: LifecycleOwner) {
        parar()
        painel?.let { (it.parent as? ViewGroup)?.removeView(it) }
        owner.lifecycle.removeObserver(this)
        painel = null
        enderecoView = null
        qualidadeView = null
        encontroView = null
        botao = null
        aoLocalizar = null
        manager = null
        this.owner = null
    }

    /** Bloqueia também callbacks e consultas que ainda estivessem pendentes no Game Over. */
    fun encerrar() {
        encerrado = true
        parar()
    }

    /** Reutiliza apenas uma posição recebida pelo GPS; os mesmos filtros de idade/precisão valem. */
    fun reavaliarLocalizacao() {
        encontroView?.setText(R.string.location_discovery_waiting)
        ultimaLocalizacao?.let { onLocationChanged(Location(it)) }
    }

    private fun temPermissaoPrecisa(): Boolean {
        val context = fragment.context ?: return false
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun iniciar() {
        if (monitorando || encerrado || !visivel()) return
        if (!temPermissaoPrecisa()) {
            mostrarAcao(R.string.location_permission_required, R.string.location_allow)
            return
        }
        val locationManager = manager ?: return
        val provedores = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false) }
        if (provedores.isEmpty()) {
            mostrarAcao(R.string.location_disabled, R.string.location_retry)
            return
        }
        var registrado = false
        for (provedor in provedores) {
            try {
                locationManager.requestLocationUpdates(provedor, 5_000L, 5f, this, Looper.getMainLooper())
                registrado = true
            } catch (_: SecurityException) {
                // A permissão pode ter sido revogada entre a conferência e a inscrição.
            } catch (_: IllegalArgumentException) {
                // O provedor pode ter ficado indisponível.
            }
        }
        monitorando = registrado
        if (registrado) {
            botao?.visibility = View.GONE
            enderecoView?.setText(R.string.location_waiting)
            esperaLocalizacaoJob = owner?.lifecycleScope?.launch {
                delay(15_000L)
                if (monitorando && ultimaLocalizacao == null) {
                    mostrarAcao(R.string.location_unavailable, R.string.location_retry)
                }
            }
        } else mostrarAcao(R.string.location_unavailable, R.string.location_retry)
    }

    private fun parar() {
        monitorando = false
        runCatching { manager?.removeUpdates(this) }
        enderecoJob?.cancel()
        esperaLocalizacaoJob?.cancel()
        enderecoJob = null
        esperaLocalizacaoJob = null
        ultimaLocalizacao = null
        ultimaConsultaEndereco = null
        instanteConsultaEndereco = 0L
    }

    private fun mostrarAcao(mensagem: Int, textoBotao: Int) {
        enderecoView?.setText(mensagem)
        botao?.apply { setText(textoBotao); visibility = View.VISIBLE }
    }

    override fun onLocationChanged(location: Location) {
        if (!monitorando || encerrado || !visivel()) return
        val idadeMs = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L
        if (!VisitasGeograficas.recente(idadeMs)) {
            qualidadeView?.apply { setText(R.string.location_stale); visibility = View.VISIBLE }
            return
        }
        ultimaLocalizacao = Location(location)
        esperaLocalizacaoJob?.cancel()
        botao?.visibility = View.GONE
        // A falha de geocodificação não impede os encontros com coordenadas válidas.
        atualizarEndereco(location)
        val precisao = if (location.hasAccuracy()) location.accuracy else null
        val precisa = precisao != null && precisao in 0f..VisitasGeograficas.PRECISAO_MAXIMA
        qualidadeView?.apply {
            setText(R.string.location_inaccurate)
            visibility = if (precisa) View.GONE else View.VISIBLE
        }
        if (!precisa) return
        val distancias = PontosTuristicos.lugares.associate { lugar ->
            val metros = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude,
                lugar.latitude, lugar.longitude, metros)
            lugar.id to metros[0]
        }
        val encontro = aoLocalizar?.invoke(distancias, idadeMs, precisao, modelos)
        if (encerrado || !visivel()) return
        val context = fragment.requireContext()
        encontroView?.text = when {
            modelos.isEmpty() -> context.getString(R.string.location_discovery_no_models)
            encontro != null && encontro.pontosGanhos > 0 -> context.getString(
                R.string.location_discovery_new, encontro.arquivo, encontro.pontosGanhos)
            encontro != null -> context.getString(R.string.location_discovery_repeated, encontro.arquivo)
            else -> {
                val proximo = PontosTuristicos.lugares.minByOrNull { distancias.getValue(it.id) }
                if (proximo != null && distancias.getValue(proximo.id) > VisitasGeograficas.RAIO_ENTRADA) {
                    context.getString(R.string.location_discovery_outside, proximo.nome,
                        kotlin.math.ceil(distancias.getValue(proximo.id).toDouble()).toInt())
                } else context.getString(R.string.location_discovery_visited)
            }
        }
    }

    private fun atualizarEndereco(location: Location) {
        val agora = SystemClock.elapsedRealtime()
        val anterior = ultimaConsultaEndereco
        if (anterior != null && agora - instanteConsultaEndereco < 30_000L &&
            anterior.distanceTo(location) < 50f) return
        if (enderecoJob?.isActive == true) return
        ultimaConsultaEndereco = Location(location)
        instanteConsultaEndereco = agora
        val context = fragment.requireContext().applicationContext
        enderecoJob = owner?.lifecycleScope?.launch {
            val endereco = withContext(Dispatchers.IO) {
                try {
                    if (!Geocoder.isPresent()) null
                    else {
                        @Suppress("DEPRECATION")
                        val resultados = Geocoder(context, Locale("pt", "BR"))
                            .getFromLocation(location.latitude, location.longitude, 1)
                        resultados?.firstOrNull()?.getAddressLine(0)?.takeIf { it.isNotBlank() }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
            }
            enderecoJob = null
            if (!monitorando || encerrado || !visivel()) return@launch
            val atual = ultimaLocalizacao
            if (atual != null && atual.distanceTo(location) >= 50f) {
                ultimaConsultaEndereco = null
                atualizarEndereco(atual)
                return@launch
            }
            enderecoView?.text = if (endereco == null) context.getString(R.string.address_unavailable)
                else context.getString(R.string.current_address, endereco)
        }
    }

    override fun onProviderDisabled(provider: String) {
        if (!monitorando) return
        val ativo = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .any { runCatching { manager?.isProviderEnabled(it) == true }.getOrDefault(false) }
        if (!ativo) {
            parar()
            mostrarAcao(R.string.location_disabled, R.string.location_retry)
        }
    }

    override fun onProviderEnabled(provider: String) = Unit

    @Deprecated("Callback utilizado em versões antigas do Android")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

    private fun dp(valor: Int) = (valor * fragment.resources.displayMetrics.density).toInt()
}
