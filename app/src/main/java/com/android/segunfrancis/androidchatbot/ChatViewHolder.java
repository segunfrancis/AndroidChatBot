package com.android.segunfrancis.androidchatbot;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ChatViewHolder extends RecyclerView.ViewHolder {
    TextView leftText, rightText;

    public ChatViewHolder(@NonNull View itemView) {
        super(itemView);

        leftText = itemView.findViewById(R.id.left_text);
        rightText=itemView.findViewById(R.id.right_text);
    }
}
