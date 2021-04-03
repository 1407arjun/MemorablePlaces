package com.example.memorableplaces;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null) {
                    centerMapOnLocation(lastKnownLocation, "Your Location");
                }
            }
        }
    }

    public void centerMapOnLocation(Location location, String title){
        Log.i("Location", location.toString());
        LatLng userLoc = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.clear();
        if (!title.equals("Your Location")) {
            mMap.addMarker(new MarkerOptions().position(userLoc).title(title));
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLoc, 15));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

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
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Intent intent = getIntent();


        if (intent.getIntExtra("place number", 0) == 0) {
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    Log.i("Location", location.toString());
                    centerMapOnLocation(location, "Your Location");
                }
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }
                @Override
                public void onProviderEnabled(@NonNull String provider) {

                }
                @Override
                public void onProviderDisabled(@NonNull String provider) {

                }
            };

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null) {
                    Log.i("LNLocation", lastKnownLocation.toString());
                    centerMapOnLocation(lastKnownLocation, "Your Location");
                }else{
                    Log.i("LNLocation", "null");
                }

            }
        }else{
            Location placeLocation = new Location(LocationManager.GPS_PROVIDER);
            placeLocation.setLatitude(MainActivity.locations.get(intent.getIntExtra("place number", 0)).latitude);
            placeLocation.setLongitude(MainActivity.locations.get(intent.getIntExtra("place number", 0)).longitude);
            centerMapOnLocation(placeLocation, (MainActivity.placesList.get(intent.getIntExtra("place number", 0))));

        }

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                Log.i("Location", latLng.toString());
                Geocoder place = new Geocoder(getApplicationContext(), Locale.getDefault());
                String address = "";
                try {
                    List<Address> addressList = place.getFromLocation(latLng.latitude, latLng.longitude, 1);

                    if (addressList != null && addressList.size() > 0){
                        if (addressList.get(0).getSubLocality() != null){
                            if (addressList.get(0).getLocality() != null) {
                                address = addressList.get(0).getSubLocality() + ", ";
                            }
                            address += addressList.get(0).getLocality();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (address.equals("")){
                    SimpleDateFormat date = (SimpleDateFormat) SimpleDateFormat.getDateInstance();
                    address = date.format(new Date());
                }
                mMap.addMarker(new MarkerOptions().position(latLng).title(address));
                MainActivity.placesList.add(address);
                MainActivity.locations.add(latLng);
                MainActivity.arrayAdapter.notifyDataSetChanged();

                SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.example.memorableplaces", Context.MODE_PRIVATE);

                try{
                    ArrayList<String> latitude = new ArrayList<>();
                    ArrayList<String> longitude = new ArrayList<>();
                    for (LatLng coord : MainActivity.locations){
                        latitude.add(Double.toString(coord.latitude));
                        longitude.add(Double.toString(coord.longitude));
                     sharedPreferences.edit().putString("places", ObjectSerializer.serialize(MainActivity.placesList)).apply();
                     sharedPreferences.edit().putString("latitude", ObjectSerializer.serialize(latitude)).apply();
                     sharedPreferences.edit().putString("longitude", ObjectSerializer.serialize(longitude)).apply();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Toast.makeText(MapsActivity.this, "Location saved!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}