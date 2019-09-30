package com.android.segunfrancis.androidchatbot;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ChatBotAdapter extends RecyclerView.Adapter<ChatViewHolder> {

    private Context mContext;
    List<ChatMessage> mMessageList;

    public ChatBotAdapter(Context context, List<ChatMessage> chatMessages) {
        mContext = context;
        mMessageList = chatMessages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.message_list, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        if (mMessageList.get(position).getMsgUser().equals("user")) {
            holder.rightText.setText(mMessageList.get(position).getMsgText());
            holder.rightText.setVisibility(View.VISIBLE);
            holder.leftText.setVisibility(View.GONE);
            //notifyDataSetChanged();
        } else {
            holder.leftText.setText(mMessageList.get(position).getMsgText());
            holder.rightText.setVisibility(View.GONE);
            holder.leftText.setVisibility(View.VISIBLE);
            //notifyDataSetChanged();
        }
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }
}
