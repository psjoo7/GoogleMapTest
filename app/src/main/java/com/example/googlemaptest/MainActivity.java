package com.example.googlemaptest;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback, GoogleMap.OnMarkerClickListener {
    LinkedList<Location> location_info = new LinkedList<>();
    private GoogleMap mMap;
    private Marker currentMarker = null;
    private AlertDialog dialog;
    private static final String TAG = "googlemap_example";
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int UPDATE_INTERVAL_MS = 1000;  // 1???
    private static final int FASTEST_UPDATE_INTERVAL_MS = 500; // 0.5???private GpsTracker gpsTracker;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    Location mCurrentLocatiion;
    LatLng currentPosition;
    List<MountElement> mount = new ArrayList<>();
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private Location location, loc_current;
    private FragmentManager fragmentmanager;
    private FragmentTransaction transaction;
    private Button done, update;
    private DatabaseReference mMount;
    private ChildEventListener mChildEventListener;
    private LinearLayout mountainList;
    public double MaxHeight;
    private String UserID;
    private int numMount = 0;
    private final int DYNAMIC_VIEW_ID = 0x8000;

    Marker marker;

    private View mLayout;  // Snackbar ???????????? ???????????? View??? ???????????????.
    // (????????? Toast????????? Context??? ??????????????????.)

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        GpsTracker gpsTracker = new GpsTracker(this);
        double latitude = gpsTracker.getLatitude();
        double longitude = gpsTracker.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude);
        String address = getCurrentAddress(latLng);
        String[] local = address.split(" ");
        String localName = local[2];


        // date??? time??? ?????? ????????????
        // ex) date = "20210722", time = "0500"
        new Thread(() ->{
            String weather = "";
            WeatherData wd = new WeatherData();
            try {
                weather = wd.lookUpWeather(getTime(), getTime1(), x, y);
            } catch (IOException e) {
                e.printStackTrace();
                Log.i("THREE_ERROR1", e.getMessage());
            } catch (JSONException e) {
                e.printStackTrace();
                Log.i("THREE_ERROR2", e.getMessage());
            }
            Log.i("Current weather", "Current weather" + weather);
        }).start();



    getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL_MS)
                .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);

        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder();

        builder.addLocationRequest(locationRequest);

        LocationManager lm =(LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Intent getUserID = getIntent();
        UserID = getUserID.getStringExtra("UserID");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mMount = FirebaseDatabase.getInstance().getReference("MountainList");
        ChildEventListener mChildEventListener;
        mMount.push().setValue(marker);

        update = findViewById(R.id.update);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("PERMISSION","??????????????? ???????????????");
        }
        else
        {
            loc_current = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        // 1. updateBtn ???????????? ?????? ??? ?????? ????????????. (lat, lng)
        // 2. ??? ???????????? ?????? ?????? ?????? ?????? ?????? ??? ??? ???????????? ??????????????? ??????
        mountainList = (LinearLayout) findViewById(R.id.mountainList);

        update.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                double lat = loc_current.getLatitude();
                double lng = loc_current.getLongitude();

                Log.d("LLL","lat : "+lat +" lng : "+lng);
                int idx = 0;
                for(int i=0; i<mount.size(); i++)
                {
                    mount.get(i).setDistance(lat, lng);
                }
                Log.d("before",mount.toString());
                Collections.sort(mount);
                Log.d("after",mount.toString());
                if(numMount > 0)
                {
                    for(int i=0; i<mount.size(); i++) {
                        TextView mountainEle = findViewById(DYNAMIC_VIEW_ID + numMount);
                        mountainList.removeView(mountainEle);
                        numMount--;
                    }
                }
                for(int i=0; i<mount.size();i++) {
                    numMount++;
                    final TextView mountain = new TextView(getBaseContext());
                    mountain.setId(DYNAMIC_VIEW_ID + numMount);
                    mountain.setText(mount.get(i).mname);
                    mountain.setTextSize(20);
                    mountain.setTypeface(null, Typeface.BOLD);
                    mountain.setBackground(getDrawable(R.drawable.textview_custom));

////                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//                    params.bottomMargin=10;
//                    mountain.setLayoutParams(params);

                    mountainList.addView(mountain);
                    Log.d("numMountain",numMount+"");
                    mountain.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {

                            Log.e("mName",""+mountain.getText());
                            Intent intent = new Intent(MainActivity.this, SearchMountActivity.class);
                            intent.putExtra("MountName", String.valueOf(mountain.getText()));
                            intent.putExtra("UserID", UserID);
                            startActivity(intent);
                        }
                    });

                }
            }
        });

    }


    @Override
    public void onMapReady(final GoogleMap googleMap) {
        Log.d(TAG, "onMapReady :");
        Log.d("cccc", "cccc1");
        mMap = googleMap;

        //????????? ????????? ?????? ??????????????? GPS ?????? ?????? ???????????? ???????????????
        //????????? ??????????????? ????????? ??????
        setDefaultLocation();

        startLocationUpdates(); // 3. ?????? ???????????? ??????
        mMount.addListenerForSingleValueEvent(new ValueEventListener() {
            int x = 0;
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    MountElement mount_each = snapshot.getValue(MountElement.class);
                    MountElement eMount = new MountElement(mount_each.end, mount_each.length, mount_each.maxHeight, mount_each.mname, mount_each.path, mount_each.starting, x++);
                    String endPoint = mount_each.end;
                    if(mount_each.maxHeight != null)
                    {
                        MaxHeight = mount_each.maxHeight;

                    }
                    String[] endPointSplit = endPoint.split(" ");
                    Log.d("MainActivity", "ValueEventListener : " + endPointSplit[0]);
                    Log.d("MainActivity", "ValueEventListener : " + endPointSplit[1]);
                    Double lag = Double.parseDouble(endPointSplit[0]);
                    Double log = Double.parseDouble(endPointSplit[1]);
                    try {
                        mount.add(eMount);
                    } catch (NullPointerException e) {
                    }
                    Log.d("MainActivity", "ValueEventListener : " + mount_each.mname);
                    LatLng latLng = new LatLng(log, lag);
                    mMap.addMarker(new MarkerOptions().position(latLng).title(mount_each.mname).snippet(String.valueOf(mount_each.maxHeight)));
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                }
                    System.out.println("aaaaaaaa1");
                    for(int i = 0 ; i < mount.size(); i++){
                        System.out.print(mount.get(i).mname + " ");
                        System.out.println();
                        System.out.print(mount.get(i).realdist + " ");
                    }
                    System.out.println();

                    System.out.println();
                    Collections.sort(mount);
                System.out.println("aaaaaaaa2" + mount);
                    System.out.println(mount.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }

        });
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 15));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(37.52487, 126.92723)));
        //marker click event --> onMarkerClick func
        mMap.setOnMarkerClickListener(this);

        mMap.getUiSettings().setMyLocationButtonEnabled(true);
//        mMap.animateCamera(CameraUpdateFactory.zoomTo(30));
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng latLng) {
                Log.d("cccc", "cccc2");
                Log.d(TAG, "onMapClick :");
            }
        });
    }

    private String x = "", y = "", address = "";

    private String getTime() {
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyyMMdd");
        String date1 = dateFormat1.format(date);

        return date1;
    }

    private String getTime1() {
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat dateFormat2 = new SimpleDateFormat("HHmm");
        String date2 = dateFormat2.format(date);
        return date2;
    }


    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            List<Location> locationList = locationResult.getLocations();
            Log.d("cccc", "cccc3");

            if (locationList.size() > 0) {

                location = locationList.get(locationList.size() - 1);
                location_info.add(location);

                currentPosition
                        = new LatLng(location.getLatitude(), location.getLongitude());




                //?????? ????????? ?????? ???????????? ??????

                String.valueOf(location.getLatitude());
                PolylineOptions polylineOptions = new PolylineOptions();
            }
        }
    };

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Log.d("cccc", "cccc4");
        mMap.setMyLocationEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "onStart");
        Log.d("cccc", "cccc5");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        if (mMap!=null)
                mMap.setMyLocationEnabled(true);
        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
//        if (checkPermission()) {
//
//            Log.d(TAG, "onStart : call mFusedLocationClient.requestLocationUpdates");
//            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
//
//            if (mMap!=null)
//                mMap.setMyLocationEnabled(true);
//        }
    }

    @Override
    protected void onStop() {

        super.onStop();
        Log.d("cccc", "cccc6");
        mFusedLocationClient.removeLocationUpdates(locationCallback);
    }

    public String getCurrentAddress(LatLng latlng) {

        //????????????... GPS??? ????????? ??????
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;
        Log.d("cccc", "cccc7");
        try {
            addresses = geocoder.getFromLocation(
                    latlng.latitude,
                    latlng.longitude,
                    1);
        } catch (IOException ioException) {
            //???????????? ??????
            Toast.makeText(this, "???????????? ????????? ????????????", Toast.LENGTH_LONG).show();
            return "???????????? ????????? ????????????";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "????????? GPS ??????", Toast.LENGTH_LONG).show();
            return "????????? GPS ??????";
        }

        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "?????? ?????????", Toast.LENGTH_LONG).show();
            return "?????? ?????????";
        } else {
            Address address = addresses.get(0);
            return address.getAddressLine(0).toString();
        }
    }

    public void setCurrentLocation(Location location, String markerTitle, String markerSnippet) {

        if (currentMarker != null) currentMarker.remove();
        Log.d("cccc", "cccc7");
        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(currentLatLng);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);

        currentMarker = mMap.addMarker(markerOptions);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(currentLatLng);
        mMap.moveCamera(cameraUpdate);

    }


    public void setDefaultLocation() {

        //????????? ??????, Seoul
        LatLng DEFAULT_LOCATION = new LatLng(37.56, 126.97);
        String markerTitle = "???????????? ????????? ??? ??????";
        String markerSnippet = "?????? ???????????? GPS ?????? ?????? ???????????????";
        Log.d("cccc", "cccc8");

        if (currentMarker != null) currentMarker.remove();

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(DEFAULT_LOCATION);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        currentMarker = mMap.addMarker(markerOptions);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 5);
        mMap.moveCamera(cameraUpdate);

    }

    /*
     * ActivityCompat.requestPermissions??? ????????? ????????? ????????? ????????? ???????????? ??????????????????.
     */
    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {
        super.onRequestPermissionsResult(permsRequestCode, permissions, grandResults);
        startLocationUpdates();
        Log.d("cccc", "cccc9");
    }

    public void drawLine(GoogleMap map, LinkedList<Location> l)
    {
        PolylineOptions polylineOptions = new PolylineOptions();
        Log.d("cccc", "cccc10");
        for(int i=0; i<l.size() ; i++)
        {
            polylineOptions.add(new LatLng(l.get(i).getLatitude(),l.get(i).getLongitude()));
        }
        Polyline polyline = map.addPolyline(polylineOptions);
    }
    //marker????????? ????????? ????????? ??????
    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(marker.getTitle() + "\n").setMessage("???????????? ??????????????????????");
        builder.setNegativeButton("???", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(MainActivity.this, SearchMountActivity.class);
                intent.putExtra("MountName", String.valueOf(marker.getTitle()));
                intent.putExtra("UserID", UserID);
                Log.d("onMarkerClick", "touchedMountainName : " + marker.getTitle());
                startActivity(intent);
            }
        });
        builder.setPositiveButton("?????????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(getApplicationContext(),"?????? ??????????????????.", Toast.LENGTH_LONG).show();
            }
        });
        dialog = builder.create();
        dialog.show();
        return true;
    }

}