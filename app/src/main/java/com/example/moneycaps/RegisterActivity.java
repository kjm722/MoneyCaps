package com.example.moneycaps;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private EditText rgName, rgPhone, rgEm, rgPw;
    private Button rgBack, rgSu;
    private FirebaseAuth mAuth;
    private DatabaseReference mDataRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        mAuth = FirebaseAuth.getInstance();
        mDataRef = FirebaseDatabase.getInstance().getReference();

        rgName = findViewById(R.id.rgName);
        rgPhone = findViewById(R.id.rgPhone);
        rgEm = findViewById(R.id.rgEm);
        rgPw = findViewById(R.id.rgPw);
        rgBack = findViewById(R.id.rgBack);
        rgSu = findViewById(R.id.rgSu);

        rgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this,LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
        rgSu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strEmail = rgEm.getText().toString();
                String strPassword = rgPw.getText().toString();

                mAuth.createUserWithEmailAndPassword(strEmail,strPassword).addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            UserAccount account = new UserAccount();
                            account.setIdToken(firebaseUser.getUid());
                            account.setEmailId(firebaseUser.getEmail());
                            account.setPassword(strPassword);

                            mDataRef.child("UserAccount").child(firebaseUser.getUid()).setValue(account);
                            Intent intent = new Intent(RegisterActivity.this,LoginActivity.class);
                            startActivity(intent);
                            finish();
                            Toast.makeText(RegisterActivity.this,"회원 가입에 성공했습니다.",Toast.LENGTH_LONG).show();
                        }else {
                            Toast.makeText(RegisterActivity.this,"회원 가입에 실패했습니다.",Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });

    }
}
