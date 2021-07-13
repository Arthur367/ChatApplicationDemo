package com.arthur.chatapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class PhoneLoginActivty extends AppCompatActivity {
    private Button sendVerificationBtn, verifyBtn;
    private EditText InputPhoneNumber, InputVerificationCode;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_login_activty);

        IntializeFields();
        mAuth = FirebaseAuth.getInstance();

        sendVerificationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                String phoneNumber = InputPhoneNumber.getText().toString();

                if (TextUtils.isEmpty(phoneNumber)){
                    Toast.makeText(PhoneLoginActivty.this, "Phone Number Required", Toast.LENGTH_SHORT).show();
                }
                else {
                    loadingBar.setTitle("Phone Verification");
                    loadingBar.setMessage("Please wait, while we are authenticating your phone");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();

                    PhoneAuthProvider.getInstance().verifyPhoneNumber(
                            phoneNumber,        // Phone number to verify
                            60,                 // Timeout duration
                            TimeUnit.SECONDS,   // Unit of timeout
                            PhoneLoginActivty.this,               // Activity (for callback binding)
                            mCallbacks);        // OnVerificationStateChangedCallbacks
                }
            }
        });
        verifyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendVerificationBtn.setVisibility(View.INVISIBLE);
                InputPhoneNumber.setVisibility(View.INVISIBLE);

                String verificationCode = InputVerificationCode.getText().toString();
                if (TextUtils.isEmpty(verificationCode)){
                    Toast.makeText(PhoneLoginActivty.this, "Please Enter Verification Code", Toast.LENGTH_SHORT).show();
                }
                else {
                    loadingBar.setTitle("Code Verification");
                    loadingBar.setMessage("Please wait, while we are verifying your code");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();

                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, verificationCode);
                    signInWithPhoneAuthCredential(credential);
                }
            }
        });
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {

                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {

                loadingBar.dismiss();

                Toast.makeText(PhoneLoginActivty.this, "Invalid Phone Number, Please Enter Correct PhoneNumber with Country Code....", Toast.LENGTH_SHORT).show();
                sendVerificationBtn.setVisibility(View.VISIBLE);
                InputPhoneNumber.setVisibility(View.VISIBLE);

                verifyBtn.setVisibility(View.INVISIBLE);
                InputVerificationCode.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;

                loadingBar.dismiss();
                Toast.makeText(PhoneLoginActivty.this, "The verification Code has been sent", Toast.LENGTH_SHORT).show();

                sendVerificationBtn.setVisibility(View.INVISIBLE);
                InputPhoneNumber.setVisibility(View.INVISIBLE);

                verifyBtn.setVisibility(View.VISIBLE);
                InputVerificationCode.setVisibility(View.VISIBLE);
            }
        };

    }
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            loadingBar.dismiss();
                            Toast.makeText(PhoneLoginActivty.this, "Congragulations your account has been set up", Toast.LENGTH_SHORT).show();
                            SendUserToMainActivity();

                        }
                        else {

                            String message = task.getException().toString();
                            Toast.makeText(PhoneLoginActivty.this, "Error: "  + message, Toast.LENGTH_SHORT).show();
                            }

                    }
                });
    }

    private void IntializeFields() {
        sendVerificationBtn = (Button) findViewById(R.id.send_verification_code_btn);
        verifyBtn = (Button) findViewById(R.id.verify_btn);
        InputPhoneNumber = (EditText) findViewById(R.id.phone_number_input);
        InputVerificationCode = (EditText) findViewById(R.id.verification_code_input);
        loadingBar = new ProgressDialog(this);
    }
    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(PhoneLoginActivty.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}