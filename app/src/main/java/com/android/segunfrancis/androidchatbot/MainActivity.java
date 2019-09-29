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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity implements AIListener {

    private static final String TAG = "MainActivity";
    RecyclerView mRecyclerView;
    EditText mEditText;
    DatabaseReference mReference;
    FirebaseRecyclerAdapter<ChatMessage, ChatViewHolder> mAdapter;
    Boolean flagFab = true;
    FloatingActionButton fab;
    private AIService mAIService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);

        mRecyclerView = findViewById(R.id.recycler_view);
        mEditText = findViewById(R.id.editText);
        fab = findViewById(R.id.floatingActionButton);

        mRecyclerView.setHasFixedSize(true);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(linearLayoutManager);

        mReference = FirebaseDatabase.getInstance().getReference();
        mReference.keepSynced(true);

        final String[] clientAccessToken = new String[1];

        mReference.child("ClientAccessToken").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                clientAccessToken[0] = dataSnapshot.getValue(String.class);
                Log.d(TAG, "onDataChange: clientAccessToken:" + clientAccessToken[0]);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        AIConfiguration config = new AIConfiguration(clientAccessToken[0],
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        mAIService = AIService.getService(this, config);
        mAIService.setListener(this);

        final AIDataService aiDataService = new AIDataService(this, config);
        final AIRequest aiRequest = new AIRequest();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                if (charSequence.toString().trim().length() != 0 && flagFab) {
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

        FirebaseRecyclerOptions<ChatMessage> options = new FirebaseRecyclerOptions.Builder<ChatMessage>().build();

        mAdapter = new FirebaseRecyclerAdapter<ChatMessage, ChatViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ChatViewHolder holder, int position, @NonNull ChatMessage model) {
                ChatMessage message = new ChatMessage();
                if (message.getMsgUser().equals("user")) {
                    holder.rightText.setText(message.getMsgText());

                    holder.rightText.setVisibility(View.VISIBLE);
                    holder.leftText.setVisibility(View.GONE);
                } else {
                    holder.leftText.setText(message.getMsgText());

                    holder.rightText.setVisibility(View.GONE);
                    holder.leftText.setVisibility(View.VISIBLE);
                }
            }

            @NonNull
            @Override
            public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_list, parent, false);
                return new ChatViewHolder(view);
            }
        };

        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);

                int msgCount = mAdapter.getItemCount();
                int lastVisiblePosition = linearLayoutManager.findLastCompletelyVisibleItemPosition();
                if (lastVisiblePosition == -1 || (positionStart >= (msgCount - 1) && lastVisiblePosition == (positionStart - 1))) {
                    mRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mRecyclerView.setAdapter(mAdapter);
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

    /**
     * Event fires if something going wrong while recognition or access to the AI server
     *
     * @param error the error description object
     */
    @Override
    public void onError(AIError error) {

    }

    /**
     * Event fires every time sound level changed. Use it to create visual feedback. There is no guarantee that this method will
     * be called.
     *
     * @param level the new RMS dB value
     */
    @Override
    public void onAudioLevel(float level) {

    }

    /**
     * Event fires when recognition engine start listening
     */
    @Override
    public void onListeningStarted() {

    }

    /**
     * Event fires when recognition engine cancel listening
     */
    @Override
    public void onListeningCanceled() {

    }

    /**
     * Event fires when recognition engine finish listening
     */
    @Override
    public void onListeningFinished() {

    }
}
