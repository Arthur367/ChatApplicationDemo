package com.arthur.chatapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private Button UpdateAccountSettings;
    private EditText userName, userStatus;
    private CircleImageView userProfileImage;
    private Uri imageUri;
    private String currentUserID;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;
    private static final int GalleryPick = 1;
    private StorageReference UserProfileImageRef;
    private ProgressDialog loadingBar;
    private Toolbar settingsToolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        InitializeFields();
        userName.setVisibility(View.INVISIBLE);

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();

        UserProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile Images");

        UpdateAccountSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateSettings();
            }
        });

        RetrieveUserInfo();

        userProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GalleryPick);
            }
        });


    }

    private void RetrieveUserInfo() {
        RootRef.child("Users").child(currentUserID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                     if ((snapshot.exists()) && (snapshot.hasChild("name") && (snapshot.hasChild("image")))){
                         String retrieveUserName = snapshot.child("name").getValue().toString();
                         String retrieveStatus = snapshot.child("status").getValue().toString();
                         String retrieveProfileImage = snapshot.child("image").getValue().toString();

                         userName.setText(retrieveUserName);
                         userStatus.setText(retrieveStatus);
                         Picasso.get().load(retrieveProfileImage).into(userProfileImage);


                     }
                     else if ((snapshot.exists()) && (snapshot.hasChild("name"))) {
                         String retrieveUserName = snapshot.child("name").getValue().toString();
                         String retrieveStatus = snapshot.child("status").getValue().toString();


                         userName.setText(retrieveUserName);
                         userStatus.setText(retrieveStatus);
                     }
                     else {
                         userName.setVisibility(View.VISIBLE);
                         Toast.makeText(SettingsActivity.this, "Set and Update your Profile Information", Toast.LENGTH_SHORT).show();

                     }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }


    private void InitializeFields() {
        UpdateAccountSettings = (Button) findViewById(R.id.update_settings_btn);
        userName = (EditText) findViewById(R.id.set_user_name);
        userStatus = (EditText) findViewById(R.id.set_user_status);
        userProfileImage = (CircleImageView) findViewById(R.id.set_profile_image);
        loadingBar = new ProgressDialog(this);
        settingsToolbar = (Toolbar) findViewById(R.id.seetings_toolbar);
        setSupportActionBar(settingsToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setTitle("Account Settings");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GalleryPick && resultCode == RESULT_OK && data != null) {
            Uri ImageUri = data.getData();

            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(this);
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {
                loadingBar.setTitle("Set Profile Image");
                loadingBar.setMessage("Please wait, while your profile image is updating...");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();
                final Uri resultUri = result.getUri();


                final StorageReference fileRef = UserProfileImageRef.child(currentUserID + ".jpg");
                StorageTask uploadTask = fileRef.putFile(resultUri);
                uploadTask.continueWithTask(new Continuation() {
                    @Override
                    public Object then(@NonNull Task task) throws Exception {
                        if (!task.isSuccessful()){
                            throw task.getException();
                        }
                        return fileRef.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()){
                            Toast.makeText(SettingsActivity.this, "Profile Image Uploaded Succesfully", Toast.LENGTH_SHORT).show();
                            Uri downloadUrl = task.getResult();
                            final String myUrl = downloadUrl.toString();
                            RootRef.child("Users").child(currentUserID).child("image")
                                    .setValue(myUrl)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                Toast.makeText(SettingsActivity.this, "Image saved in database successfully", Toast.LENGTH_SHORT).show();
                                                loadingBar.dismiss();
                                            }
                                            else {
                                                String message = task.getException().toString();
                                                Toast.makeText(SettingsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                                                loadingBar.dismiss();
                                            }
                                        }
                                    });

                        }
                        else {
                            String message = task.getException().toString();
                            Toast.makeText(SettingsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }
                    }


                });
            }


        }
}







    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(SettingsActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
    private void UpdateSettings() {
        String setUserName = userName.getText().toString();
        String setUserStatus = userStatus.getText().toString();


        if (TextUtils.isEmpty(setUserName)) {
            Toast.makeText(this, "Please enter Username", Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(setUserStatus)) {
            Toast.makeText(this, "Please enter status", Toast.LENGTH_SHORT).show();
        } else {
            HashMap<String, Object> profileMap = new HashMap<>();
            profileMap.put("uid", currentUserID);
            profileMap.put("name", setUserName);
            profileMap.put("status", setUserStatus);


            RootRef.child("Users").child(currentUserID).updateChildren(profileMap)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                SendUserToMainActivity();
                                Toast.makeText(SettingsActivity.this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                String message = task.getException().toString();
                                Toast.makeText(SettingsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }

    }
}