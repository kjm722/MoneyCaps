package com.example.moneycaps;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class LogActivity extends AppCompatActivity {

    private static final String TAG = "LogActivity";
    private FirebaseFirestore db;
    private ArrayList<String> motionList;
    private ArrayAdapter<String> adapter;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log);

        db = FirebaseFirestore.getInstance();
        motionList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,motionList);

        ListView logList = findViewById(R.id.logList);
        Button btnlgBack = findViewById(R.id.btnlgBack);
        logList.setAdapter(adapter);

        btnlgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LogActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void fetchMotions(){
        db.collection("motion")
                .orderBy("timestamp")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null){
                            Log.w(TAG, "Listen failed", e);
                            return;
                        }

                        motionList.clear();
                        for (QueryDocumentSnapshot doc : snapshots){
                            if (doc.get("timestamp") != null){
                                motionList.add(doc.getString("timestamp"));
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}
