package com.android.segunfrancis.androidchatbot;

import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIDataService;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AIListener {

    private static final String TAG = "MainActivity";
    RecyclerView mRecyclerView;
    EditText mEditText;
    DatabaseReference mReference;
    Boolean flagFab = true;
    FloatingActionButton fab;
    private AIService mAIService;
    List<ChatMessage> mList;
    ChatBotAdapter mChatBotAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);

        mRecyclerView = findViewById(R.id.recycler_view);
        mEditText = findViewById(R.id.editText);
        fab = findViewById(R.id.floatingActionButton);

        mList = new ArrayList<>();

        mRecyclerView.setHasFixedSize(true);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mReference = FirebaseDatabase.getInstance().getReference();
        mReference.keepSynced(true);


        mReference.child("chat").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ChatMessage message = snapshot.getValue(ChatMessage.class);
                    mList.add(message);
                }
                mChatBotAdapter = new ChatBotAdapter(MainActivity.this, mList);
                mRecyclerView.setAdapter(mChatBotAdapter);
                mRecyclerView.scrollToPosition(mList.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        AIConfiguration config = new AIConfiguration("8725c343946a4e13b315cb5e7ae4d1d3",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        mAIService = AIService.getService(this, config);
        mAIService.setListener(this);

        final AIDataService aiDataService = new AIDataService(this, config);
        final AIRequest aiRequest = new AIRequest();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
                mRecyclerView.scrollToPosition(lastVisiblePosition);
                String message = mEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(message)) {
                    ChatMessage chatMessage = new ChatMessage(message, "user");
                    mReference.child("chat").push().setValue(chatMessage);

                    aiRequest.setQuery(message);
                    new AsyncTask<AIRequest, Void, AIResponse>() {
                        @Override
                        protected AIResponse doInBackground(AIRequest... aiRequests) {
                            AIRequest mRequest = aiRequests[0];
                            try {
                                AIResponse response = aiDataService.request(aiRequest);
                                return response;
                            } catch (AIServiceException e) {
                                Log.e(TAG, "doInBackground: AIResponse:" + e.getMessage());
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(AIResponse aiResponse) {
                            if (aiResponse != null) {
                                Result result = aiResponse.getResult();
                                String reply = result.getFulfillment().getSpeech();
                                ChatMessage message = new ChatMessage(reply, "bot");
                                mReference.child("chat").push().setValue(message);
                            }
                        }
                    }.execute(aiRequest);
                } else {
                    mAIService.startListening();
                }
                mEditText.setText("");
            }
        });

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (charSequence.toString().trim().length() != 0) {
                    fab.setImageResource(R.drawable.ic_send);
                    flagFab = true;
                } else {
                    fab.setImageResource(R.drawable.ic_mic);
                    flagFab = false;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    /**
     * Event fires when entire process finished successfully, and returns result object
     *
     * @param response the result object, contains server answer
     */
    @Override
    public void onResult(AIResponse response) {
        Result result = response.getResult();
        String message = result.getResolvedQuery();
        ChatMessage chatMessage = new ChatMessage(message, "user");
        mReference.child("chat").push().setValue(chatMessage);

        String reply = result.getFulfillment().getSpeech();
        ChatMessage chatMessage1 = new ChatMessage(reply, "bot");
        mReference.child("chat").push().setValue(chatMessage1);
    }

    @Override
    public void onError(AIError error) {

    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {

    }


    /* ************************************ Menu ********************************************** */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.delete_all_items) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Are you sure you want to delete all chats?")
                    .setTitle("Clear All Chat")
                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            deleteAllMessages();
                            dialogInterface.dismiss();
                        }
                    })
                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
            builder.create().show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteAllMessages() {
        mReference.child("chat").removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "All Chats have been cleared", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed: \n" + task.getException(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
