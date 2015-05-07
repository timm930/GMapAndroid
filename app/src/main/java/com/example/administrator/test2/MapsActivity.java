package com.example.administrator.test2;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapsActivity extends Activity {

    private TextView txtOutput;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private ArrayList<LatLng> traceOfMe; // record the path
    private LocationManager locationMgr;
    private String provider;
    private Marker markerMe;
    private Location mLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        txtOutput = (TextView) findViewById(R.id.txtOutput);
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link com.google.android.gms.maps.SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                if (initLocationProvider()) {
                    whereAmI();
                    setUpMap();
                }else{
                    txtOutput.setText("請開啟定位！");
                }
            }
        }
    }

    private boolean initLocationProvider() {
        locationMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //1.選擇最佳提供器
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        //
        provider = locationMgr.getBestProvider(criteria, true);

        if (provider != null) {
            return true;
        }

        //2.選擇使用GPS提供器
        /*if (locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
            return true;
        }*/

        //3.選擇使用網路提供器
        // if (locationMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        //  provider = LocationManager.NETWORK_PROVIDER;
        //  return true;
        // }

        return false;
    }

    private void whereAmI(){
        //取得上次已知的位置
        Location location = locationMgr.getLastKnownLocation(provider);
        try {
            mLocation = location;
            updateWithNewLocation(location);
        } catch (IOException e) {
            //e.printStackTrace();
        }

        //GPS Listener
        locationMgr.addGpsStatusListener(gpsListener);

        //Location Listener
        int minTime = 5000;//ms
        int minDist = 5;//meter
        locationMgr.requestLocationUpdates(provider, minTime, minDist, (android.location.LocationListener) locationListener);
        location = locationMgr.getLastKnownLocation(provider);
        try {
            mLocation = location;
            updateWithNewLocation(location);
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    GpsStatus.Listener gpsListener = new GpsStatus.Listener() {
        @Override
        public void onGpsStatusChanged(int event) {
            switch (event) {
                case GpsStatus.GPS_EVENT_STARTED:
                    //Log.d(TAG, "GPS_EVENT_STARTED");
                    Toast.makeText(MapsActivity.this, "GPS_EVENT_STARTED", Toast.LENGTH_SHORT).show();
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    //Log.d(TAG, "GPS_EVENT_STOPPED");
                    Toast.makeText(MapsActivity.this, "GPS_EVENT_STOPPED", Toast.LENGTH_SHORT).show();
                    break;
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    //Log.d(TAG, "GPS_EVENT_FIRST_FIX");
                    Toast.makeText(MapsActivity.this, "GPS_EVENT_FIRST_FIX", Toast.LENGTH_SHORT).show();
                    break;
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    //Log.d(TAG, "GPS_EVENT_SATELLITE_STATUS");
                    break;
            }
        }
    };

    LocationListener locationListener = new LocationListener(){
        //@Override
        public void onLocationChanged(Location location) {
            try {
                mLocation = location;
                updateWithNewLocation(location);
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }

        //@Override
        public void onProviderDisabled(String provider) {
            try {
                mLocation = null;
                updateWithNewLocation(null);
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }

        //@Override
        public void onProviderEnabled(String provider) {

        }

        //@Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case LocationProvider.OUT_OF_SERVICE:
                    //Log.v(TAG, "Status Changed: Out of Service");
                    Toast.makeText(MapsActivity.this, "Status Changed: Out of Service", Toast.LENGTH_SHORT).show();
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    //Log.v(TAG, "Status Changed: Temporarily Unavailable");
                    Toast.makeText(MapsActivity.this, "Status Changed: Temporarily Unavailable", Toast.LENGTH_SHORT).show();
                    break;
                case LocationProvider.AVAILABLE:
                    //Log.v(TAG, "Status Changed: Available");
                    Toast.makeText(MapsActivity.this, "Status Changed: Available", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void showMarkerMe(double lat, double lng){
        if (markerMe != null) {
            markerMe.remove();
        }

        MarkerOptions markerOpt = new MarkerOptions();
        markerOpt.position(new LatLng(lat, lng));
        markerOpt.title("我在這裡");
        markerMe = mMap.addMarker(markerOpt);

        Toast.makeText(this, "lat:" + lat + ",lng:" + lng, Toast.LENGTH_SHORT).show();
    }

    private void updateWithNewLocation(Location location) throws IOException {
        String where = "";
        if (location != null) {
            //經度
            double lng = location.getLongitude();
            //緯度
            double lat = location.getLatitude();
            //速度
            float speed = location.getSpeed();
            //時間
            long time = location.getTime();
            String timeString = getTimeString(time);

            String addr = getAddress(lat, lng);

            where = "經度: " + lng +
                    "\n緯度: " + lat +
                    "\n速度: " + speed +
                    "\n時間: " + timeString +
                    "\nProvider: " + provider +
                    "\nAddress: " + addr;

            //"我"
            showMarkerMe(lat, lng);
            //cameraFocusOnMe(lat, lng);
            //trackToMe(lat, lng);

        }else{
            where = "No location found.";
        }

        //顯示資訊
        txtOutput.setText(where);
    }

    private String getTimeString(long timeInMilliseconds){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(timeInMilliseconds);
    }

    private String getAddress(double lat, double lng) throws IOException {
        String returnAddress = "";
        try{
            Geocoder gc = new Geocoder(this, Locale.TRADITIONAL_CHINESE);
            List<Address> lstAddress = gc.getFromLocation(lat, lng, 1);
            returnAddress=lstAddress.get(0).getAddressLine(0);
        } catch(Exception e) {

        }

        return returnAddress;
    }

    private final GoogleMap.OnMyLocationButtonClickListener mOnMyLocationButtonClickListener = new
            GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    /*if (mLocationClient.isConnected() && mLocationClient.getLastLocation() == null) {
                                        Toast.makeText(getActivity(), R.string.unable_to_locate_you, Toast.LENGTH_SHORT).show();
                                          return true;
                                             }
                                         return false;*/

                    if(mLocation != null) {
                        try {
                            //whereAmI();
                            updateWithNewLocation(mLocation);
                        } catch (IOException e) {
                            //e.printStackTrace();
                        }
                        return true;
                    }
                    return false;
                }
     };


    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationButtonClickListener(mOnMyLocationButtonClickListener);
    }

}
