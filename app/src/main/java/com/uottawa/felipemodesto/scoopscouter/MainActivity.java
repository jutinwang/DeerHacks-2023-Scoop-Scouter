package com.uottawa.felipemodesto.scoopscouter;

import static java.lang.Math.round;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Looper;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.Toast;

import org.checkerframework.checker.units.qual.C;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static MainActivity instance;
    Double[] locations = new Double[2];

    ImageButton takePicture;
    Button findTrucks;
    private LocationRequest locationRequest;
    Button testButton;
    SearchView searchBar;
    GoogleMap myGoogle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.maps);
        mapFragment.getMapAsync(this);
        instance = this;

        locations[0] = 43.550553331155974;
        locations[1] = -79.66621315112504;

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);
        takePicture = findViewById(R.id.reportSightingButton);
        findTrucks = findViewById(R.id.truckInArea);
        testButton = findViewById(R.id.testbutton);
        searchBar = (SearchView) findViewById(R.id.searchForAddress);
        searchBar.setSubmitButtonEnabled(true);
        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                getQuery();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                        Manifest.permission.CAMERA
                }, 100);
            }

            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, 200);
            }
        }

        takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });

        findTrucks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("Iam the button", "here");
                CloudStorage cloud = new CloudStorage();
                float zoomLevel = myGoogle.getCameraPosition().zoom;
                GeoPoint currentLocation = new GeoPoint(getLat(),getLon());
                cloud.GetNearbyTrucks(currentLocation, round(zoomLevel));
            }
        });

        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CloudStorage cloud = new CloudStorage();
                GeoPoint currentLocation = new GeoPoint(getLat(), getLon());
                Date d = new Date();
                Timestamp currentTime = new Timestamp(d);

                cloud.AddData(currentTime,currentLocation);
            }
        });
    }

    public void getQuery(){
        CharSequence query = searchBar.getQuery();
        String queryString = query.toString();

        List<String> coordinates = Arrays.asList(queryString.split(","));

        LatLng myLocation = new LatLng(Float.parseFloat(coordinates.get(0)), Float.parseFloat(coordinates.get(1)));

        Marker tester = myGoogle.addMarker(new MarkerOptions()
                .position(myLocation)
                .title("You're Here Now!")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        myGoogle.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15));
        setLat(myLocation.latitude);
        setLon(myLocation.longitude);
        Log.e("Search Bar Message", "" + query);
    }

    public static MainActivity getInstance() {
        return instance;
    }

    //triggered to add contents to map
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        float[] results = new float[1];
        myGoogle = googleMap;
        // Set up a listener for camera idle events
        googleMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                // Check the current zoom level of the map
                float zoomLevel = googleMap.getCameraPosition().zoom;
                // Set the visibility of the button based on the zoom level
                if (zoomLevel < 15 || zoomLevel > 60) {
                    findTrucks.setVisibility(View.GONE);
                } else {
                    findTrucks.setVisibility(View.VISIBLE);
                }
            }
        });

        //current markers
        LatLng myLocation = new LatLng(getLat(), getLon());
        LatLng schoolParkingLot = new LatLng(43.553434, -79.679756);

        //method for calculating distance
        Location.distanceBetween(myLocation.latitude, myLocation.longitude,
                schoolParkingLot.latitude, schoolParkingLot.longitude, results);

        //placing the user marker
        Marker userMarker = googleMap.addMarker(new MarkerOptions()
                .position(myLocation)
                .title("DEERHACKS 2023")
                .snippet("Deerhack rules!")
        );

        //placing the marker for all avaliable trucks
        Marker tester = googleMap.addMarker(new MarkerOptions()
                        .position(schoolParkingLot)
                        .title("Distance:")
                        .snippet("" + round(results[0]) + "m." + " " + (round(results[0]) * 100)/4800 + "min to walk")
                .icon(BitmapFromVector(getApplicationContext(), R.drawable.ice_cream_marker)));

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15));

        new CountDownTimer(30000, 1000) {

            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                tester.remove();
            }
        }.start();

    }

    //method for placing markers all over the map.
    public void placeMarkers (List<GeoPoint> geoList){
        for (int x = 0; x < geoList.size(); x++){
            float[] results = new float[1];

            LatLng userLocation = new LatLng(getLat(), getLon());
            LatLng newIceCreamTruck = new LatLng(geoList.get(x).getLatitude(), geoList.get(x).getLongitude());

            Location.distanceBetween(userLocation.latitude, userLocation.longitude,
                    newIceCreamTruck.latitude, newIceCreamTruck.longitude, results);
            Marker newIceCreamTruckMarker = myGoogle.addMarker(new MarkerOptions()
                    .position(newIceCreamTruck)
                    .title("Distance")
                    .snippet("" + round(results[0]) + "m." + " " + (round(results[0]) * 100)/4800 + "min to walk")
                    .icon(BitmapFromVector(getApplicationContext(), R.drawable.ice_cream_marker)));

            new CountDownTimer(30000, 1000) {

                public void onTick(long millisUntilFinished) {
                }
                public void onFinish() {
                    newIceCreamTruckMarker.remove();
                }
            }.start();
        }
    }

    //https://www.geeksforgeeks.org/how-to-add-custom-marker-to-google-maps-in-android/
    //converts vector to bitmap
    private BitmapDescriptor BitmapFromVector(Context context, int vectorResId) {
        // below line is use to generate a drawable.
        Drawable vectorDrawable = ContextCompat.getDrawable(
                context, vectorResId);

        // below line is use to set bounds to our vector
        // drawable.
        vectorDrawable.setBounds(
                0, 0, vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight());

        // below line is use to create a bitmap for our
        // drawable which we have added.
        Bitmap bitmap = Bitmap.createBitmap(
                vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);

        // below line is use to add bitmap in our canvas.
        Canvas canvas = new Canvas(bitmap);

        // below line is use to draw our
        // vector drawable in canvas.
        vectorDrawable.draw(canvas);

        // after generating our bitmap we are returning our
        // bitmap.
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    //basic camera function to test picture taking
    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            StorageManager storageManager = (StorageManager) getSystemService(STORAGE_SERVICE);
            StorageVolume storageVolume = null; // internal Storage
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                storageVolume = storageManager.getStorageVolumes().get(0);
            }
            Bitmap bitmapInputImage = (Bitmap) data.getExtras().get("data");

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmapInputImage.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] bytesArray = byteArrayOutputStream.toByteArray();

            File fileOutput = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                fileOutput = new File(storageVolume.getDirectory().getPath() + "/Download/output1.jpeg");
            }
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(fileOutput);
                fileOutputStream.write(bytesArray);
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isGPSEnabled()) {
                    getCurrentLocation();
                } else {
                    turnOnGPS();
                }
            }
        }
    }

    private void getCurrentLocation() {
        Double[] locations = new Double[2];

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (isGPSEnabled()) {
                findCurrentLocation();
            } else {
                turnOnGPS();
            }
        }
    }

    private void findCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(MainActivity.this)
                    .requestLocationUpdates(locationRequest, new LocationCallback() {
                        @Override
                        public void onLocationResult(@NonNull LocationResult locationResult) {
                            super.onLocationResult(locationResult);

                            LocationServices.getFusedLocationProviderClient(MainActivity.this)
                                    .removeLocationUpdates(this);

                            if (locationResult != null && locationResult.getLocations().size() > 0) {

                                int index = locationResult.getLocations().size() - 1;
                                double latitude = locationResult.getLocations().get(index).getLatitude();
                                double longitude = locationResult.getLocations().get(index).getLongitude();

                                locations[0] = latitude;
                                locations[1] = longitude;
                                test(latitude, longitude);
                                Log.e("test", String.valueOf(locations[0]));
                            }
                        }
                    }, Looper.getMainLooper());
            Log.e("test2", String.valueOf(locations[0]));
        }
    }

    private void test(Double v1, Double v2){
        Log.e("please work", "Please: " + v1 + " " + v2);
        setLat(v1);
        setLon(v2);
    }

    private void setLat(Double lat){
        locations[0] = lat;
    }
    private void setLon(Double lon){
        locations[1] = lon;
    }
    private Double getLat(){
        Log.e("please work", "Please: " + locations[0] + "blah");
        return locations[0];
    }
    private Double getLon(){
        return locations[1];
    }

    private void turnOnGPS() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(getApplicationContext())
                .checkLocationSettings(builder.build());

        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {

                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    Toast.makeText(MainActivity.this, "GPS is already turned on", Toast.LENGTH_SHORT).show();

                } catch (ApiException e) {

                    switch (e.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:

                            try {
                                ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                                resolvableApiException.startResolutionForResult(MainActivity.this, 2);
                            } catch (IntentSender.SendIntentException ex) {
                                ex.printStackTrace();
                            }
                            break;

                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            //Device does not have location
                            break;
                    }
                }
            }
        });
    }

    private boolean isGPSEnabled() {
        LocationManager locationManager = null;
        boolean isEnabled = false;

        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }

        isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return isEnabled;
    }
}