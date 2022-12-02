package com.example.googlemaptest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ResultActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private String time, record, mname, UserID;
    private Double ArrivalRate;
    private Task<Void> mData;
    private DatabaseReference mRef;
    private Button TraceButton,mainBtn;

//            intent.putExtra("time", time1);
//                intent.putExtra("mname", MountName);
//                intent.putExtra("record", "It is path.");
//                intent.putExtra("UserID", UserID);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        //get record ( 추가 요소 : 총경로 아직 안됐음, arrival rate 필요)
        Intent getRecords = getIntent();
        time = getRecords.getStringExtra("time");
        record = getRecords.getStringExtra("record");
        mname = getRecords.getStringExtra("mname");
        UserID = getRecords.getStringExtra("UserID");
        ArrivalRate = getRecords.getDoubleExtra("Rate",0);
        Log.d("RRRRR",ArrivalRate+"");
        UserID = UserID.split("[.]")[0];
        Record userRecord = new Record(time, record, mname, UserID);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mRef = FirebaseDatabase.getInstance().getReference("UserRecord");
        mData = mRef.child(UserID).push().setValue(userRecord);
        TraceButton = findViewById(R.id.TraceButton);
        TraceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ResultActivity.this, RecordActivity.class);
                intent.putExtra("UserID", UserID);
                startActivity(intent);
                finish();
            }
        });
        mainBtn = findViewById(R.id.mainBtn);
        mainBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ResultActivity.this, MainActivity.class);
                intent.putExtra("UserID", UserID);
                startActivity(intent);
                finish();
            }
        });

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        drawLine(mMap, record);
    }

    // polyline을 그리는 함수, path를 받아온다.
    public void drawLine(GoogleMap Map , String path){
        PolylineOptions polylineOptions = new PolylineOptions();
        try {
            String[] path_list = path.split(",");
            for (int i = 0; i < path_list.length; i++) {
                path_list[i] = path_list[i].trim();
                Double lat = Double.parseDouble(path_list[i].split(" ")[0]);
                Double lng = Double.parseDouble(path_list[i].split(" ")[1]);
                polylineOptions.add(new LatLng(lng, lat));
                polylineOptions.width(10);
                polylineOptions.color(Color.BLUE);
            }
            Polyline polyline = Map.addPolyline(polylineOptions);

        }
        catch (Exception e){}
    }
}