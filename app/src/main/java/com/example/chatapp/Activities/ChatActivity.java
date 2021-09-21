package com.example.chatapp.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.chatapp.Adapters.MessagesAdapter;
import com.example.chatapp.Models.Message;
import com.example.chatapp.R;
import com.example.chatapp.databinding.ActivityChatBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class ChatActivity extends AppCompatActivity {

    ActivityChatBinding binding;
    MessagesAdapter adapter;
    ArrayList<Message> messages;
    String senderRoom, receiverRoom;
    ImageView sendBtn;
    FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        messages = new ArrayList<>();

        sendBtn = findViewById(R.id.sendBtn);
        String name = getIntent().getStringExtra("name");
        String receiverUid = getIntent().getStringExtra("uid");
        String senderUid = FirebaseAuth.getInstance().getUid();

        senderRoom = senderUid + receiverUid;
        receiverRoom = receiverUid + senderUid;

        adapter = new MessagesAdapter(this,messages,senderRoom,receiverRoom);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        binding.recyclerView.scrollToPosition(messages.size() - 1);
        binding.recyclerView.setLayoutManager(linearLayoutManager);
        binding.recyclerView.setAdapter(adapter);
        database = FirebaseDatabase.getInstance();

        database.getReference().child("Chats")
                .child(senderRoom)
                .child("messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messages.clear();
                        for(DataSnapshot snapshot1: snapshot.getChildren()){
                            Message message = snapshot1.getValue(Message.class);
                            message.setMessageId(snapshot1.getKey());
                            messages.add(message);
                        }
                        adapter.notifyDataSetChanged();
                        binding.recyclerView.scrollToPosition(messages.size() - 1);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


        sendBtn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageTxt = binding.messageBox.getText().toString();
                Date date = new Date();
                Message message = new Message(messageTxt,senderUid,date.getTime());
                binding.messageBox.setText("");

                String randomKey = database.getReference().push().getKey();

                HashMap<String, Object> lastMsgObject = new HashMap<>();
                lastMsgObject.put("lastMsg", message.getMessage());
                lastMsgObject.put("lastMsgTime", date.getTime());

                database.getReference().child("Chats").child(senderRoom).updateChildren(lastMsgObject);
                database.getReference().child("Chats").child(receiverRoom).updateChildren(lastMsgObject);

                database.getReference().child("Chats")
                        .child(senderRoom)
                        .child("messages")
                        .child(randomKey)
                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        database.getReference().child("Chats")
                                .child(receiverRoom)
                                .child("messages")
                                .child(randomKey)
                                .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                            }
                        });
                        HashMap<String, Object> lastMsgObject = new HashMap<>();
                        lastMsgObject.put("lastMsg", message.getMessage());
                        lastMsgObject.put("lastMsgTime", date.getTime());

                        database.getReference().child("Chats").child(senderRoom).updateChildren(lastMsgObject);
                        database.getReference().child("Chats").child(receiverRoom).updateChildren(lastMsgObject);
                    }
                });

            }
        });

        getSupportActionBar().setTitle(name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}