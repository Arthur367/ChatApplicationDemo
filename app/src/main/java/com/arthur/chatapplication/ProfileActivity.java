package com.arthur.chatapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private String receiverUserID, senderUserID;
    private TextView userProfileName, userProfileStatus;
    private CircleImageView userProfileImage;
    private Button sendMessageButton, CancelRequestBtn;
    private DatabaseReference UserRef, ChatRequestRef, ContactsRef;
    private String current_state;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        InitializeFields();
        UserRef = FirebaseDatabase.getInstance().getReference().child("Users");
        receiverUserID = getIntent().getExtras().get("visit_user_id").toString();
        mAuth = FirebaseAuth.getInstance();
        senderUserID = mAuth.getCurrentUser().getUid();
        RetrieveUserInfo();
        ChatRequestRef = FirebaseDatabase.getInstance().getReference().child("Chat Request");
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");

    }

    private void RetrieveUserInfo() {
        UserRef.child(receiverUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
              if ((snapshot.exists()) && (snapshot.hasChild("image"))){

                  String userImage = snapshot.child("image").getValue().toString();
                  String userName = snapshot.child("name").getValue().toString();
                  String userStatus = snapshot.child("status").getValue().toString();

                  Picasso.get().load(userImage).placeholder(R.drawable.profile_image).into(userProfileImage);
                  userProfileName.setText(userName);
                  userProfileStatus.setText(userStatus);

                  ManageChatRequest();

              }
              else {
                  String userName = snapshot.child("name").getValue().toString();
                  String userStatus = snapshot.child("status").getValue().toString();
                  userProfileName.setText(userName);
                  userProfileStatus.setText(userStatus);
                  ManageChatRequest();
              }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void ManageChatRequest() {
        ChatRequestRef.child(senderUserID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                      if (snapshot.hasChild(receiverUserID)){
                          String request_type = snapshot.child(receiverUserID).child("request_type")
                                  .getValue().toString();
                          if (request_type.equals("sent")){
                              current_state = "request_sent";
                              sendMessageButton.setText("Cancel Chat Request");
                          }
                          else if(request_type.equals("received")){
                              current_state = "request_received";
                              sendMessageButton.setText("Accept Chat Request");

                              CancelRequestBtn.setVisibility(View.VISIBLE);
                              CancelRequestBtn.setEnabled(true);
                              CancelRequestBtn.setOnClickListener(new View.OnClickListener() {
                                  @Override
                                  public void onClick(View v) {
                                      CancelChatRequest();
                                  }
                              });
                          }
                      }
                      else {
                          ContactsRef.child(senderUserID)
                                  .addListenerForSingleValueEvent(new ValueEventListener() {
                                      @Override
                                      public void onDataChange(@NonNull DataSnapshot snapshot) {
                                          if (snapshot.hasChild(receiverUserID)){
                                              current_state = "friends";
                                              sendMessageButton.setText("Remove Contact");
                                          }
                                      }

                                      @Override
                                      public void onCancelled(@NonNull DatabaseError error) {

                                      }
                                  });
                      }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    if ( !senderUserID.equals(receiverUserID)){

    sendMessageButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
             sendMessageButton.setEnabled(false);
             if (current_state.equals("new")){
                 SendChatRequest();
             }
             if (current_state.equals("request_sent")){
                 CancelChatRequest();
             }
            if (current_state.equals("request_received")){
                AcceptChatRequest();
            }
            if (current_state.equals("friends")){
                RemoveSpecificContact();
            }
        }
    });

}
else {
    sendMessageButton.setVisibility(View.INVISIBLE);
}
    }

    private void RemoveSpecificContact() {
        ContactsRef.child(senderUserID).child(receiverUserID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            ContactsRef.child(receiverUserID).child(senderUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {

                                            if (task.isSuccessful()){
                                                sendMessageButton.setEnabled(true);
                                                current_state = "new";
                                                sendMessageButton.setText("Send Chat Request");

                                                CancelRequestBtn.setVisibility(View.INVISIBLE);
                                                CancelRequestBtn.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void AcceptChatRequest() {

      ContactsRef.child(senderUserID).child(receiverUserID)
              .child("Contacts")
              .setValue("Saved")
              .addOnCompleteListener(new OnCompleteListener<Void>() {
                  @Override
                  public void onComplete(@NonNull Task<Void> task) {
                      if (task.isSuccessful()){
                          ContactsRef.child(receiverUserID).child(senderUserID)
                                  .child("Contacts")
                                  .setValue("Saved")
                                  .addOnCompleteListener(new OnCompleteListener<Void>() {
                                      @Override
                                      public void onComplete(@NonNull Task<Void> task) {
                                          if (task.isSuccessful()){


                                              ChatRequestRef.child(senderUserID).child(receiverUserID)
                                                      .removeValue()
                                                      .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                          @Override
                                                          public void onComplete(@NonNull Task<Void> task) {
                                                              if (task.isSuccessful()){
                                                                  ChatRequestRef.child(receiverUserID).child(senderUserID)
                                                                          .removeValue()
                                                                          .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                              @Override
                                                                              public void onComplete(@NonNull Task<Void> task) {

                                                                                  sendMessageButton.setEnabled(true);
                                                                                  current_state = "friends";
                                                                                  sendMessageButton.setText("Remove Contact");
                                                                                  CancelRequestBtn.setVisibility(View.INVISIBLE);
                                                                                  CancelRequestBtn.setEnabled(false);
                                                                              }
                                                                          });
                                                              }
                                                          }
                                                      });
                                          }
                                      }
                                  });

                      }
                  }
              });
    }

    private void CancelChatRequest() {
        ChatRequestRef.child(senderUserID).child(receiverUserID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            ChatRequestRef.child(receiverUserID).child(senderUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {

                                            if (task.isSuccessful()){
                                                sendMessageButton.setEnabled(true);
                                                current_state = "new";
                                                sendMessageButton.setText("Send Chat Request");

                                                CancelRequestBtn.setVisibility(View.INVISIBLE);
                                                CancelRequestBtn.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void SendChatRequest() {

    ChatRequestRef.child(senderUserID).child(receiverUserID).child("request_type")
            .setValue("sent")
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()){
                        ChatRequestRef.child(receiverUserID).child(senderUserID)
                                .child("request_type").setValue("received")
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        if (task.isSuccessful()){
                                            sendMessageButton.setEnabled(true);
                                            current_state = "request_sent";
                                            sendMessageButton.setText("Cancel Chat Request");
                                        }
                                    }
                                });
                    }
                }
            });
    }

    private void InitializeFields() {
        userProfileImage = (CircleImageView) findViewById(R.id.visit_profile_image);
        userProfileName = (TextView) findViewById(R.id.visit_profile_user_name);
        userProfileStatus = (TextView) findViewById(R.id.visit_profile_status);
        sendMessageButton = (Button) findViewById(R.id.send_message_request_btn);
        CancelRequestBtn = (Button) findViewById(R.id.cancel_message_request_btn);
        current_state = "new";
    }
}