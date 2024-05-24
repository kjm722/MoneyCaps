package com.example.moneycaps;

import static com.google.firebase.messaging.Constants.MessageNotificationKeys.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private ImageButton btnDate, btnLock, btnCctv, btnCall;
    private int[] imageIds = {R.drawable.a, R.drawable.b, R.drawable.c};
    private ViewPager2 viewPager2;
    private int currentPage = 0;
    private Timer timer;
    private final long DELAY_MS = 1000;
    private final long PERIOD_MS = 3000;
    private static final String TAG = "WifiArduino";
    private static final String notify_TAG = "MainActivity";
    private FirebaseFirestore db;
    private static final String SERVER_URL = "http://<Arduino_IP_Address>/control";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager2 = findViewById(R.id.viewPager);
        ImageAdapter adapter = new ImageAdapter();
        viewPager2.setAdapter(adapter);
        FirebaseApp.initializeApp(this);

        startAutoScroll();
        //StartRepeatingTask();

        btnCctv = (ImageButton) findViewById(R.id.btnCctv);
        btnDate = (ImageButton) findViewById(R.id.btnDate);
        btnCall = (ImageButton) findViewById(R.id.btnCall);
        btnLock = (ImageButton) findViewById(R.id.btnLock);

        connectToWiFi("YOUR_SSID", "YOUR_PASSWORD");

        db = FirebaseFirestore.getInstance();

        fetchMotions();

        btnDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LogActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnCctv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CctvActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
                dlg.setTitle("금고 원격 잠금");
                dlg.setMessage("금고를 잠금하시겠습니까?");
                dlg.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new ControlServoTask().execute(SERVER_URL + "?position=90");
                        Toast.makeText(MainActivity.this, "금고가 잠겼습니다.", Toast.LENGTH_LONG).show();
                    }
                });
                dlg.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                dlg.show();
            }
        });

        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
                dlg.setTitle("경찰서 전화 연결");
                dlg.setMessage("경찰서에 전화 연결합니까?");
                dlg.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String police = "010-4111-0782";
                        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel: " + police));
                        startActivity(intent);
                    }
                });
                dlg.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                dlg.show();
            }
        });
    }

    private void connectToWiFi(String ssid, String password) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", ssid);
        wifiConfig.preSharedKey = String.format("\"%s\"", password);

        int netId = wifiManager.addNetwork(wifiConfig);
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private class ControlServoTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                return sendGetRequest(urls[0]);
            } catch (Exception e) {
                Log.e(TAG, "Error: ", e);
                return null;
            }
        }
    }

    //아두이노 WIFI 요청
    private String sendGetRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();
        } else {
            throw new Exception("GET request not worked");
        }
    }

    private void fetchMotions() {
        db.collection("motions")
                .orderBy("timestamp")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(notify_TAG, "Listen failed.", e);
                            return;
                        }

                        for (QueryDocumentSnapshot doc : snapshots) {
                            if (doc.get("timestamp") != null) {
                                String timestamp = doc.getString("timestamp");
                                sendFirebaseMessage(timestamp);
                            }
                        }
                    }
                });
    }

    private void sendFirebaseMessage(String timestamp){
        MyFirebaseMessagingService messagingService = new MyFirebaseMessagingService();
        messagingService.sendNotification("머니캡스","외부인이 사용자의 금고 잠금 해제를 시도하였습니다. CCTV를 확인해보세요.");
    }


    // 사진 베너 자동 슬라이드
    private void startAutoScroll() {
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable update = new Runnable() {
            @Override
            public void run() {
                if (currentPage == imageIds.length) {
                    currentPage = 0;
                }
                viewPager2.setCurrentItem(currentPage++, true);
            }
        };

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(update);
            }
        }, DELAY_MS, PERIOD_MS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.itiem_image, parent, false);
            return new ImageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            int imageId = imageIds[position % imageIds.length];
            holder.imageView.setImageResource(imageId);
        }

        @Override
        public int getItemCount() {
            return Integer.MAX_VALUE;
        }

        class ImageViewHolder extends RecyclerView.ViewHolder {

            ImageView imageView;

            public ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageView);
            }
        }
    }

}
