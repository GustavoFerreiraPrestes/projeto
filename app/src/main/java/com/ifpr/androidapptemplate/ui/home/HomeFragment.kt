package com.ifpr.androidapptemplate.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.ifpr.androidapptemplate.R
import java.util.Locale
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

class HomeFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private lateinit var txtTempo: TextView
    private lateinit var txtDistancia: TextView
    private lateinit var txtRitmo: TextView
    private lateinit var txtStatus: TextView
    private lateinit var btnIniciar: Button
    private lateinit var btnFim: Button

    private var isRunning = false
    private var isPaused = false
    private var lastLocation: Location? = null
    private var totalDistance = 0f
    private var tempoSegundos = 0
    private var timer: Timer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        txtTempo = view.findViewById(R.id.txtTempo)
        txtDistancia = view.findViewById(R.id.txtDistancia)
        txtRitmo = view.findViewById(R.id.txtRitmo)
        txtStatus = view.findViewById(R.id.txtStatus)
        btnIniciar = view.findViewById(R.id.btnIniciar)
        btnFim = view.findViewById(R.id.btnFim)

        btnIniciar.setOnClickListener { onStartPauseResumeClick() }
        btnFim.setOnClickListener { pararCorrida() }

        return view
    }

    private fun onStartPauseResumeClick() {
        when {
            !isRunning -> iniciarCorrida()
            isRunning && !isPaused -> pausarCorrida()
            isRunning && isPaused -> continuarCorrida()
        }
    }

    private fun iniciarCorrida() {
        isRunning = true
        isPaused = false
        totalDistance = 0f
        tempoSegundos = 0
        lastLocation = null

        btnIniciar.text = "Pausar"
        txtStatus.text = "Correndo"

        iniciarTimer()
        iniciarAtualizacaoLocalizacao()

        Toast.makeText(context, "Corrida iniciada!", Toast.LENGTH_SHORT).show()
    }

    private fun pausarCorrida() {
        isPaused = true
        btnIniciar.text = "Continuar"
        txtStatus.text = "Pausado"
        Toast.makeText(context, "Corrida pausada!", Toast.LENGTH_SHORT).show()
    }

    private fun continuarCorrida() {
        isPaused = false
        btnIniciar.text = "Pausar"
        txtStatus.text = "Correndo"
        Toast.makeText(context, "Corrida retomada!", Toast.LENGTH_SHORT).show()
    }

    private fun pararCorrida() {
        if (!isRunning) return

        isRunning = false
        isPaused = false
        timer?.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)

        btnIniciar.text = "Iniciar"
        txtStatus.text = "Parado"

        Toast.makeText(context, "Corrida finalizada!", Toast.LENGTH_SHORT).show()
    }

    private fun iniciarTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(0, 1000) {
            if (isRunning && !isPaused) {
                tempoSegundos++
                requireActivity().runOnUiThread {
                    txtTempo.text = formatarTempo(tempoSegundos)
                    atualizarRitmo()
                }
            }
        }
    }

    private fun iniciarAtualizacaoLocalizacao() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 2000L
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val novaLocalizacao = locationResult.lastLocation ?: return

                if (lastLocation != null && isRunning && !isPaused) {
                    totalDistance += lastLocation!!.distanceTo(novaLocalizacao)
                    val distanciaKm = totalDistance / 1000
                    txtDistancia.text = String.format(Locale.getDefault(), "%.2f km", distanciaKm)
                    atualizarRitmo()
                }
                lastLocation = novaLocalizacao
            }
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun atualizarRitmo() {
        val distanciaKm = totalDistance / 1000
        if (distanciaKm > 0) {
            val ritmo = tempoSegundos / 60f / distanciaKm
            txtRitmo.text = String.format(Locale.getDefault(), "%.2f min/km", ritmo)
        } else {
            txtRitmo.text = "0.00 min/km"
        }
    }

    private fun formatarTempo(segundos: Int): String {
        val h = segundos / 3600
        val m = (segundos % 3600) / 60
        val s = segundos % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
