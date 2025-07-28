package com.naxobrowser;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CookieAdapter extends RecyclerView.Adapter<CookieAdapter.CookieViewHolder> {

    private List<Cookie> cookieList;
    private List<Cookie> filteredCookieList;
    private OnCookieCheckedChangeListener listener;

    public interface OnCookieCheckedChangeListener {
        void onCookieCheckedChanged(Cookie cookie, boolean isChecked);
    }

    public CookieAdapter(List<Cookie> cookieList, OnCookieCheckedChangeListener listener) {
        this.cookieList = cookieList;
        this.filteredCookieList = new ArrayList<>(cookieList);
        this.listener = listener;
    }

    public void updateCookieList(List<Cookie> newCookieList) {
        this.cookieList = newCookieList;
        this.filteredCookieList = new ArrayList<>(newCookieList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CookieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cookie_item, parent, false);
        return new CookieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CookieViewHolder holder, int position) {
        if (position >= filteredCookieList.size()) {
            return;
        }
        
        Cookie cookie = filteredCookieList.get(position);
        holder.domainTextView.setText(cookie.domain.isEmpty() ? "Default Domain" : cookie.domain);
        
        // Format cookie details for display
        String details = cookie.name + "=" + cookie.value;
        if (!cookie.domain.isEmpty()) {
            details += "; Domain=" + cookie.domain;
        }
        if (!cookie.path.isEmpty()) {
            details += "; Path=" + cookie.path;
        }
        if (cookie.secure) {
            details += "; Secure";
        }
        if (cookie.httpOnly) {
            details += "; HttpOnly";
        }
        
        holder.cookieDetailsTextView.setText(details);
        holder.cookieCheckBox.setChecked(cookie.selected);

        holder.cookieCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            cookie.selected = isChecked;
            if (listener != null) {
                listener.onCookieCheckedChanged(cookie, isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredCookieList.size();
    }

    public void filter(String text) {
        filteredCookieList.clear();
        if (text.isEmpty()) {
            filteredCookieList.addAll(cookieList);
        } else {
            text = text.toLowerCase();
            for (Cookie cookie : cookieList) {
                if (cookie.name.toLowerCase().contains(text) ||
                    cookie.value.toLowerCase().contains(text) ||
                    cookie.domain.toLowerCase().contains(text) ||
                    cookie.path.toLowerCase().contains(text)) {
                    filteredCookieList.add(cookie);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void selectAll(boolean isChecked) {
        for (Cookie cookie : filteredCookieList) {
            cookie.selected = isChecked;
        }
        notifyDataSetChanged();
    }

    public List<Cookie> getSelectedCookies() {
        List<Cookie> selectedCookies = new ArrayList<>();
        for (Cookie cookie : filteredCookieList) {
            if (cookie.selected) {
                selectedCookies.add(cookie);
            }
        }
        return selectedCookies;
    }

    public List<Cookie> getFilteredCookies() {
        return new ArrayList<>(filteredCookieList);
    }

    static class CookieViewHolder extends RecyclerView.ViewHolder {
        TextView domainTextView;
        TextView cookieDetailsTextView;
        CheckBox cookieCheckBox;

        public CookieViewHolder(@NonNull View itemView) {
            super(itemView);
            domainTextView = itemView.findViewById(R.id.domainTextView);
            cookieDetailsTextView = itemView.findViewById(R.id.cookieDetailsTextView);
            cookieCheckBox = itemView.findViewById(R.id.cookieCheckBox);
        }
    }
}

