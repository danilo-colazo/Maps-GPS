package com.example.maps_gps.fragments;

import static android.content.Context.LOCATION_SERVICE;

import static androidx.core.content.ContextCompat.checkSelfPermission;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.maps_gps.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MapsFragment extends Fragment implements OnMapReadyCallback, View.OnClickListener,
        LocationListener {

    private View rootView;
    private FloatingActionButton fab_gps;
    private GoogleMap g_map;
    private MapView mv_map;
    private LocationManager locationManager;
    private Location currentLocation;
    private Marker marker;
    private CameraPosition cameraPosition;

    public MapsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_maps, container, false);

        fab_gps = rootView.findViewById(R.id.fab_gps);
        fab_gps.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mv_map = rootView.findViewById(R.id.mapView_map);

        if (mv_map != null) {
            mv_map.onCreate(null);
            mv_map.onResume();
            mv_map.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        g_map = googleMap;
        locationManager = (LocationManager) getContext().getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // do request the permission
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 8);
        } else {
            locationManager.requestLocationUpdates(LocationManager
                    .NETWORK_PROVIDER, 1000, 0, this);
            locationManager.requestLocationUpdates(LocationManager
                    .GPS_PROVIDER, 1000, 0, this);
        }
        getCurrentLocation();
    }

    @Override
    public void onClick(View v) {
        if (!this.isGPSEnable())
            showInfoAlert();
        else {
            getCurrentLocation();
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.d("LOCALIZACION", "Location provider by: " + location.getProvider());
        createOrUpdateMarketLocation(location);
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

    /**
     * Método encargado de tomar la localización actual, marcarlo en el mapa y realizar un zoom.
     */
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // do request the permission
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 8);
        } else {
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null)
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            currentLocation = location;
            if (currentLocation != null) {
                createOrUpdateMarketLocation(location);
                zoomToLocation(location);
            }
        }
    }

    /**
     * Método encargado realizar un zoom sobre cierta localización.
     *
     * @param location es la localización a la cual se le realizará el zoom.
     */
    private void zoomToLocation(Location location) {
        cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(location.getLatitude(), location.getLongitude()))
                .zoom(15)
                .bearing(0)
                .tilt(30)
                .build();
        g_map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    /**
     * Método encargado de verificar si el GPS está habilitado y enviar un valor de tipo booleano
     * de acuerdo al resultado de dicha verificación.
     *
     * @return true cuando el GPS está habilitado y false cuando el GPS está deshabilitado.
     */
    private boolean isGPSEnable() {
        try {
            int gpsSignal = Settings.Secure
                    .getInt(getActivity().getContentResolver(), Settings.Secure.LOCATION_MODE);
            return gpsSignal != 0;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Alerta que se abre cuando el GPS no está habilitado.
     */
    private void showInfoAlert() {
        new AlertDialog.Builder(getContext())
                .setTitle("GPS Signal")
                .setMessage("¿Desea activar el GPS?")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("CANCELAR", null)
                .show();
    }

    /**
     * Método cuyo objetivo es crear o editar un marcador a partir de una localización recibida
     * como parámetro.
     *
     * @param location Es la localización cuya información se utilizará para crear o editar el
     *                 marcador.
     */
    private void createOrUpdateMarketLocation(Location location) {
        if (marker == null)
            marker = g_map.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(),
                    location.getLongitude())).draggable(true));
        else
            marker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
    }

}