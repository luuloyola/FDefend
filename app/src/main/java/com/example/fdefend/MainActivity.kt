package com.example.fdefend

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.fdefend.databinding.ActivityMainBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException
import java.util.UUID


class MainActivity : AppCompatActivity(), OnMapReadyCallback{

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var myMap: GoogleMap
    private var FINE_PERMISSION_CODE: Int = 1
    var currentLocation: Location? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    fun connectToBluetoothDevice(): BluetoothSocket? {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

        // Verificar si el dispositivo soporta Bluetooth
        if (bluetoothAdapter == null) {
            println("El dispositivo no soporta Bluetooth")
            return null
        }

        // Verificar si Bluetooth está habilitado en el dispositivo
        if (!bluetoothAdapter.isEnabled) {
            println("Bluetooth no está habilitado")
            return null
        }

        // Iniciar el descubrimiento de dispositivos Bluetooth disponibles
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null
        }
        if (!bluetoothAdapter.startDiscovery()) {
            println("No se pudo iniciar el descubrimiento de dispositivos Bluetooth")
            return null
        }

        // Esperar hasta que se descubran dispositivos o un tiempo determinado
        Thread.sleep(5000) // Esperar 5 segundos (puedes ajustar este tiempo según tus necesidades)

        // Obtener una lista de dispositivos descubiertos
        val devices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices

        // Seleccionar un dispositivo de la lista (aquí se selecciona el primero, puedes implementar tu propia lógica de selección)
        val selectedDevice: BluetoothDevice? = devices?.firstOrNull()

        // Crear un socket Bluetooth con el dispositivo seleccionado
        val socket: BluetoothSocket? = try {
            selectedDevice?.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }

        // Conectar al dispositivo seleccionado
        try {
            socket?.connect()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return socket
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        getLastLocation();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        var bluetoothSocket = connectToBluetoothDevice()
        val dataReader = bluetoothSocket?.let { BluetoothService(it) }

        // Inicia la lectura de datos
        if (dataReader != null) {
            dataReader.startReading { message ->
                // Aquí puedes manejar el mensaje recibido, por ejemplo, mostrarlo en un TextView
                runOnUiThread {
                    add_marker_boton(myMap)
                }
            }
        }
    }



    private fun getLastLocation() {
        val task: Task<Location> = if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                FINE_PERMISSION_CODE
            )
            return
        } else {
            fusedLocationProviderClient.lastLocation
        }

        task.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
                val mapFragment = supportFragmentManager
                    .findFragmentById(R.id.map) as SupportMapFragment
                mapFragment.getMapAsync(this@MainActivity)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        val celesteColor = Color.rgb(66, 134, 245)
        myMap = googleMap
        val my_Location = LatLng(currentLocation!!.latitude,currentLocation!!.longitude)

        // Crear un marcador personalizado con un color específico y agregarlo al mapa
        val markerOptions = MarkerOptions()
            .position(my_Location) // Ubicación del marcador
            .icon(BitmapDescriptorFactory.fromBitmap(getMarkerIcon(celesteColor))) // Color del marcador

        // Agregar el marcador al mapa
        myMap.addMarker(markerOptions)
        cergy_Markers(myMap)
        // Move the camera
        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(my_Location, 15f))
    }

    private fun add_marker_boton(googleMap: GoogleMap){
        getLastLocation()
        myMap = googleMap
        val my_Location = LatLng(currentLocation!!.latitude,currentLocation!!.longitude)

        // Crear un marcador personalizado con un color específico y agregarlo al mapa
        val markerOptions = MarkerOptions()
            .position(my_Location) // Ubicación del marcador

        // Agregar el marcador al mapa
        myMap.addMarker(markerOptions)
    }

    private fun cergy_Markers(googleMap: GoogleMap){
        myMap = googleMap
        lateinit var markerOptions: MarkerOptions
        // Lugares que en Cergy no son seguros

        val dangerousParts: Map<Array<String>, DoubleArray> = mapOf(
            arrayOf("Valley near ENSEA and CROUS", "Robbery occurred at 9 p.m.") to doubleArrayOf(49.0397666604233, 2.0725052969193274),
            arrayOf("Behind Les Chenes D'or","Assault ocurred at 7 p.m.") to doubleArrayOf(49.03865416014599, 2.0736100997942026),
            arrayOf("Bridge","Attempted sexual assault at 4 a.m.") to doubleArrayOf(49.037927412758286, 2.074164399521768),
            arrayOf("Bridge","Drunk people at 3 a.m.") to doubleArrayOf(49.036916917563936, 2.075531166010214),
            arrayOf("Near Les Linandes", "Assault ocurred at 5 p.m.") to doubleArrayOf(49.04641130463903, 2.0653876663246993),
            arrayOf("Cemetery", "Attempted robbery at 1 a.m.") to doubleArrayOf(49.04038282034096, 2.065476170117093)
        )

        for ((event,location) in dangerousParts){
            val markerOptions = MarkerOptions()
                .position(LatLng(location[0],location[1])) // Ubicación del marcador
                .title(event[0])
                .snippet(event[1])
            // Agregar el marcador al mapa
            myMap.addMarker(markerOptions)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == FINE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation()
            } else {
                Toast.makeText(this, "Location permission is denied, please allow the permission to access", Toast.LENGTH_SHORT).show()
            }
        }

    }
    // Función para crear un icono personalizado con un color específico
    private fun getMarkerIcon(color: Int): Bitmap {
        val radius = 17f // Tamaño del círculo del marcador
        val strokeWidth = 2f // Grosor del borde del círculo
        val bitmap =
            Bitmap.createBitmap((radius * 2).toInt(), (radius * 2).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint()
        paint.isAntiAlias = true
        paint.color = color
        paint.style = Paint.Style.FILL

        val strokePaint = Paint()
        strokePaint.isAntiAlias = true
        strokePaint.color = Color.WHITE
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = strokeWidth

        canvas.drawCircle(radius, radius, radius - strokeWidth / 2, paint)
        canvas.drawCircle(radius, radius, radius - strokeWidth / 2, strokePaint)

        return bitmap
    }
}