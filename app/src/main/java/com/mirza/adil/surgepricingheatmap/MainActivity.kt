package com.mirza.adil.surgepricingheatmap

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ch.hsr.geohash.GeoHash
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private var service: LocationManager? = null
    private var enabled: Boolean? = null
    private var mLocationRequest: LocationRequest? = null
    private var mGoogleApiClient: GoogleApiClient? = null
    private var mLastLocation: Location? = null
    private var mCurrLocationMarker: Marker? = null
    private lateinit var mMap: GoogleMap
    private var REQUEST_LOCATION_CODE = 101
    private val mListCurrentPolygons: MutableList<Polygon> = ArrayList()
    private val mListCurrentPolygonMarkers: MutableList<Marker> = ArrayList()


    override fun onLocationChanged(location: Location?) {
        mLastLocation = location
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker!!.remove()
        }

        //Place current location marker
        val latLng = LatLng(location!!.latitude, location.longitude)
        val markerOptions = MarkerOptions()
        markerOptions.position(latLng)
        markerOptions.title("Current Position")
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
        mCurrLocationMarker = mMap.addMarker(markerOptions)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

    }

    override fun onConnected(p0: Bundle?) {
        mLocationRequest = LocationRequest()
        mLocationRequest!!.interval = 1000
        mLocationRequest!!.fastestInterval = 1000
        mLocationRequest!!.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        // Check if enabled and if not send user to the GPS settings
        if (!enabled!!) {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
        // Check if permission is granted or not
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient,
                mLocationRequest,
                this
            )
        }
    }

    override fun onConnectionSuspended(p0: Int) {
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        service = this.getSystemService(LOCATION_SERVICE) as LocationManager
        enabled = service!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                //Location Permission already granted
                buildGoogleApiClient()
                mMap.isMyLocationEnabled = true
            } else {
                //Request Location Permission
                checkLocationPermission()
            }
        } else {
            buildGoogleApiClient()
            mMap.isMyLocationEnabled = true
        }

        handleHeatMapResponse()

    }

    @Synchronized
    fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build()

        mGoogleApiClient!!.connect()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient()
                        }
                        mMap.isMyLocationEnabled = true
                    }
                } else {
                    // permission denied, boo! Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                AlertDialog.Builder(this)
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs the Location permission, please accept to use location functionality")
                    .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            REQUEST_LOCATION_CODE
                        )
                    })
                    .create()
                    .show()

            } else ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_CODE
            )
        }
    }

    /*
     * TODO: In this function get Color surge pallete
     * */
    private fun getColourSurge(value: Double): Int {
        return if (value < 1.3) {
            this.resources.getColor(R.color.heatmap_one)
        } else if (value >= 1.3 && value < 1.5) {
            this.resources.getColor(R.color.heatmap_two)
        } else if (value >= 1.5 && value < 1.9) {
            this.resources.getColor(R.color.heatmap_three)
        } else if (value >= 1.9 && value < 2.3) {
            this.resources.getColor(R.color.heatmap_four)
        } else {
            this.resources.getColor(R.color.heatmap_default)
        }
    }



    /*
     * TODO: In this function handle heat map API response.
     * */
    private fun handleHeatMapResponse() {
        if (mMap != null) {

            //1st GeoHash

            var latLngsList: ArrayList<LatLng?>
            latLngsList = ArrayList()
            try {
                val geoHash: GeoHash = GeoHash.fromGeohashString(
                    java.lang.String.valueOf("ttsg7g")
                )
                val polygonOptions = PolygonOptions().geodesic(true)
                    .add(
                        LatLng(
                            geoHash.getBoundingBox().getMaxLat(),
                            geoHash.getBoundingBox().getMinLon()
                        )
                    )
                    .add(
                        LatLng(
                            geoHash.getBoundingBox().getMinLat(),
                            geoHash.getBoundingBox().getMinLon()
                        )
                    )
                    .add(
                        LatLng(
                            geoHash.getBoundingBox().getMinLat(),
                            geoHash.getBoundingBox().getMaxLon()
                        )
                    )
                    .add(
                        LatLng(
                            geoHash.getBoundingBox().getMaxLat(),
                            geoHash.getBoundingBox().getMaxLon()
                        )
                    )
                polygonOptions.strokeColor(
                    getColourSurge(
                        1.3
                    )
                )
                polygonOptions.strokeWidth(0.0f)
                polygonOptions.fillColor(
                    getColourSurge(1.3)
                )
                mListCurrentPolygons.add(mMap.addPolygon(polygonOptions))
                latLngsList.add(
                    LatLng(
                        geoHash.getPoint().getLatitude(),
                        geoHash.getPoint().getLongitude()
                    )
                )
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }

            val marker : Marker = addText(this, mMap, LatLng(31.528, 74.35), "x 1", 0, 16)!!
            mListCurrentPolygonMarkers.add(marker)
            //2nd GeoHash
            try {
                val geoHash: GeoHash = GeoHash.fromGeohashString(
                    java.lang.String.valueOf("ttsgk5")
                )
                val polygonOptions = PolygonOptions().geodesic(true)
                    .add(
                        LatLng(
                            geoHash.getBoundingBox().getMaxLat(),
                            geoHash.getBoundingBox().getMinLon()
                        )
                    )
                    .add(
                        LatLng(
                            geoHash.getBoundingBox().getMinLat(),
                            geoHash.getBoundingBox().getMinLon()
                        )
                    )
                    .add(
                        LatLng(
                            geoHash.getBoundingBox().getMinLat(),
                            geoHash.getBoundingBox().getMaxLon()
                        )
                    )
                    .add(
                        LatLng(
                            geoHash.getBoundingBox().getMaxLat(),
                            geoHash.getBoundingBox().getMaxLon()
                        )
                    )
                polygonOptions.strokeColor(
                    getColourSurge(
                        1.5
                    )
                )
                polygonOptions.strokeWidth(0.0f)
                polygonOptions.fillColor(
                    getColourSurge(1.5)
                )
                mListCurrentPolygons.add(mMap.addPolygon(polygonOptions))
                latLngsList.add(
                    LatLng(
                        geoHash.getPoint().getLatitude(),
                        geoHash.getPoint().getLongitude()
                    )
                )
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }

            val markerSecond : Marker = addText(this, mMap, LatLng(31.528, 74.361), "x 4.6", 0, 16)!!
            mListCurrentPolygonMarkers.add(markerSecond)

        //3rd GeoHash
            try {
                val geoHash: GeoHash = GeoHash.fromGeohashString(
                    java.lang.String.valueOf("ttsgk7")
                )
                val polygonOptions = PolygonOptions().geodesic(true)
                    .add(
                        LatLng(
                            geoHash.getBoundingBox().getMaxLat(),
                            geoHash.getBoundingBox().getMinLon()
                        )
                    )
                    .add(
                        LatLng(
                            geoHash.getBoundingBox().getMinLat(),
                            geoHash.getBoundingBox().getMinLon()
                        )
                    )
                    .add(
                        LatLng(
                            geoHash.getBoundingBox().getMinLat(),
                            geoHash.getBoundingBox().getMaxLon()
                        )
                    )
                    .add(
                        LatLng(
                            geoHash.getBoundingBox().getMaxLat(),
                            geoHash.getBoundingBox().getMaxLon()
                        )
                    )
                polygonOptions.strokeColor(
                    getColourSurge(
                        1.9
                    )
                )
                polygonOptions.strokeWidth(0.0f)
                polygonOptions.fillColor(
                    getColourSurge(1.9)
                )
                mListCurrentPolygons.add(mMap.addPolygon(polygonOptions))
                latLngsList.add(
                    LatLng(
                        geoHash.getPoint().getLatitude(),
                        geoHash.getPoint().getLongitude()
                    )
                )
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
            val markerThird : Marker = addText(this, mMap, LatLng(31.528, 74.372), "x 3.0", 0, 16)!!
            mListCurrentPolygonMarkers.add(markerThird)

        //4th GeoHash
            try {
                val geoHash: GeoHash = GeoHash.fromGeohashString(
                    java.lang.String.valueOf("ttsg7f")
                )
                val polygonOptions = PolygonOptions().geodesic(true)
                    .add(
                        LatLng(
                            geoHash.getBoundingBox().getMaxLat(),
                            geoHash.getBoundingBox().getMinLon()
                        )
                    )
                    .add(
                        LatLng(
                            geoHash.getBoundingBox().getMinLat(),
                            geoHash.getBoundingBox().getMinLon()
                        )
                    )
                    .add(
                        LatLng(
                            geoHash.getBoundingBox().getMinLat(),
                            geoHash.getBoundingBox().getMaxLon()
                        )
                    )
                    .add(
                        LatLng(
                            geoHash.getBoundingBox().getMaxLat(),
                            geoHash.getBoundingBox().getMaxLon()
                        )
                    )
                polygonOptions.strokeColor(
                    getColourSurge(
                        5.0
                    )
                )
                polygonOptions.strokeWidth(0.0f)
                polygonOptions.fillColor(
                    getColourSurge(5.0)
                )
                mListCurrentPolygons.add(mMap.addPolygon(polygonOptions))
                latLngsList.add(
                    LatLng(
                        geoHash.getPoint().getLatitude(),
                        geoHash.getPoint().getLongitude()
                    )
                )
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }

            val maidenform : Marker = addText(this, mMap, LatLng(31.5225, 74.35), "x 4.9", 0, 16)!!
            mListCurrentPolygonMarkers.add(maidenform)


            //5th Geohas
            try {
                val geoHash: GeoHash = GeoHash.fromGeohashString(
                        java.lang.String.valueOf("ttsgk4")
                )
                val polygonOptions = PolygonOptions().geodesic(true)
                    .add(
                        LatLng(
                            geoHash.getBoundingBox().getMaxLat(),
                            geoHash.getBoundingBox().getMinLon()
                        )
                    )
                    .add(
                        LatLng(
                            geoHash.getBoundingBox().getMinLat(),
                            geoHash.getBoundingBox().getMinLon()
                        )
                    )
                    .add(
                        LatLng(
                            geoHash.getBoundingBox().getMinLat(),
                            geoHash.getBoundingBox().getMaxLon()
                        )
                    )
                    .add(
                        LatLng(
                            geoHash.getBoundingBox().getMaxLat(),
                            geoHash.getBoundingBox().getMaxLon()
                        )
                    )
                polygonOptions.strokeColor(
                    getColourSurge(
                        1.9
                    )
                )
                polygonOptions.strokeWidth(0.0f)
                polygonOptions.fillColor(
                    getColourSurge(1.9)
                )
                mListCurrentPolygons.add(mMap.addPolygon(polygonOptions))
                latLngsList.add(
                    LatLng(
                        geoHash.getPoint().getLatitude(),
                        geoHash.getPoint().getLongitude()
                    )
                )
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        }

        val markerFifth : Marker = addText(this, mMap, LatLng(31.5225, 74.361), "x 2.7", 0, 16)!!
        mListCurrentPolygonMarkers.add(markerFifth)


    }

    /*
     * TODO: In this function we will add text in polygon.
     * */
    private fun addText(
        context: Context?, map: GoogleMap?,
        location: LatLng?, text: String?, padding: Int,
        fontSize: Int
    ): Marker? {
        var marker: Marker? = null
        if (context == null || map == null || location == null || text == null || fontSize <= 0) {
            return marker
        }
        val textView = TextView(context)
        textView.text = text
        textView.setTypeface(null, Typeface.BOLD)
        textView.textSize = fontSize.toFloat()
        val paintText: Paint = textView.paint
        val boundsText = Rect()
        try {
            paintText.getTextBounds(text, 0, text.length, boundsText)
        } catch (e: IndexOutOfBoundsException) {

        }
        paintText.textAlign = Paint.Align.CENTER
        val conf = Bitmap.Config.ARGB_8888
        val bmpText = Bitmap.createBitmap(
            boundsText.width() + 2
                    * padding, boundsText.height() + 2 * padding, conf
        )
        val canvasText = Canvas(bmpText)
        paintText.color = Color.WHITE
        canvasText.drawText(
            text, (canvasText.width / 2).toFloat(), (
                    canvasText.height - padding - boundsText.bottom).toFloat(), paintText
        )
        val markerOptions = MarkerOptions()
            .position(location)
            .icon(BitmapDescriptorFactory.fromBitmap(bmpText))
        marker = map.addMarker(markerOptions)
        return marker
    }

}