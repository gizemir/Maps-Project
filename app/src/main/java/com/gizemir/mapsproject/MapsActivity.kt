package com.gizemir.mapsproject

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.gizemir.mapsproject.databinding.ActivityMapsBinding
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener{

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    //konumun hangi servisten alınacağını koordine eden sınıf
    private lateinit var locationManager: LocationManager
    //dinleyen ve konumda bir değişiklik olduğunda haber veren yapı
    private lateinit var locationListener: LocationListener

    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    var takipBoolean: Boolean? = null
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        registerLauncher()

        sharedPreferences = getSharedPreferences("com.gizemir.mapsproject", MODE_PRIVATE)
        takipBoolean = false
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        //tıklanan yere marker eklemek için(uzun tıklandığında)
        mMap.setOnMapLongClickListener(this)
        // Kullanıcıya marker gçstermek
        /*val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney)) */

        locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
        locationListener = object : LocationListener{

            override fun onLocationChanged(location: Location) {
                takipBoolean = sharedPreferences.getBoolean("takipBoolean", false)
                if(!takipBoolean!!){
                    mMap.clear()
                    val userLocation = LatLng(location.latitude, location.longitude)
                    mMap.addMarker(MarkerOptions().position(userLocation).title("Now, you are here! "))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14f))
                    sharedPreferences.edit().putBoolean("takipBoolean", true).apply()
                }

            }
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                Snackbar.make(
                    binding.root,
                    "permission for your location",
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(
                    "Allow!"
                ) {
                    //izin isteyeceğiz
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }.show()
            } else {
                //izin isteyeceğiz
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else{
            //izin zaten verilmiş. Kullanıcının konumunu aldık
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
            //en son bilinen konumu aldık(internetin çekmeme durumları için)
            val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if(lastLocation != null){
                val lastLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLatLng, 14f))
            }
        }


    }
    private  fun registerLauncher(){
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){result ->
            if(result){
                if(ContextCompat.checkSelfPermission(this@MapsActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    //izin verildiyse
                    //izin zaten verilmiş. Kullanıcının konumunu aldık
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
                    //en son bilinen konumu aldık(internetin çekmeme durumları için)
                    val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if(lastLocation != null){
                        val lastLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLatLng, 14f))
                    }
                }
            }else{
                //İzin verilmediyse
                Toast.makeText(this@MapsActivity, "We need your permission!", Toast.LENGTH_LONG).show()
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onMapLongClick(p0: LatLng) {
        //haritaya uzun tıklanınca ne olacağı
        //tıklanan yerin enlem boylamını veren fonksiyon
        //GoogleMap.OnMapLongClickListener ile birlikte gelen fonksiyon
        mMap.clear()
        //adresi almak için
        val geocoder = Geocoder(this, Locale.getDefault())
        var adres = ""
        try {
            geocoder.getFromLocation(p0.latitude, p0.longitude, 1, Geocoder.GeocodeListener{ adresList ->
                val ilkAdres = adresList.first()
                val country = ilkAdres.countryName
                val sokak = ilkAdres.thoroughfare
                val numara = ilkAdres.subThoroughfare
                adres += sokak
                adres += numara
                println(adres)

            })
        }catch (e: Exception){
            e.printStackTrace()
        }
    }
}