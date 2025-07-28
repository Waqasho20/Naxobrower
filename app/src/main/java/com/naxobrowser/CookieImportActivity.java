package com.naxobrowser;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.webkit.CookieManager; // Sahi import
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CookieImportActivity extends AppCompatActivity implements CookieAdapter.OnCookieCheckedChangeListener {

    private static final String TAG = "CookieImportActivity";
    private EditText searchEditText;
    private CheckBox selectAllCheckBox;
    private RecyclerView cookieRecyclerView;
    private Button importButton;
    private CookieAdapter cookieAdapter;
    private List<Cookie> cookieList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cookie_import);

        initViews();
        setupRecyclerView();
        setupListeners();

        String cookieData = getIntent().getStringExtra("cookie_data");
        if (cookieData != null && !cookieData.isEmpty()) {
            parseCookies(cookieData);
        } else {
            Toast.makeText(this, "No cookie data received.", Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        TextView titleTextView = findViewById(R.id.titleTextView);
        searchEditText = findViewById(R.id.searchEditText);
        selectAllCheckBox = findViewById(R.id.selectAllCheckBox);
        cookieRecyclerView = findViewById(R.id.cookieRecyclerView);
        Button closeButton = findViewById(R.id.closeButton);
        importButton = findViewById(R.id.importButton);
    }

    private void setupRecyclerView() {
        cookieList = new ArrayList<>();
        cookieAdapter = new CookieAdapter(cookieList, this);
        cookieRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cookieRecyclerView.setAdapter(cookieAdapter);
    }

    private void setupListeners() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                cookieAdapter.filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        selectAllCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                cookieAdapter.selectAll(isChecked);
            }
        });

        findViewById(R.id.closeButton).setOnClickListener(v -> finish());

        importButton.setOnClickListener(v -> {
            List<Cookie> selectedCookies = cookieAdapter.getSelectedCookies();
            if (selectedCookies.isEmpty()) {
                Toast.makeText(this, "Please select at least one cookie to import.", Toast.LENGTH_SHORT).show();
                return;
            }
            importSelectedCookies(selectedCookies);
        });
    }

    private void parseCookies(String cookieData) {
        cookieList.clear();
        // Auto-detect format
        if (cookieData.trim().startsWith("[") || cookieData.trim().startsWith("{")) {
            parseJsonCookies(cookieData);
        } else {
            parseNetscapeCookies(cookieData);
        }

        Log.d(TAG, "Total cookies parsed: " + cookieList.size());
        ((TextView) findViewById(R.id.titleTextView)).setText("Import cookies (" + cookieList.size() + ")");
        cookieAdapter.updateCookieList(cookieList);
        cookieRecyclerView.scrollToPosition(0);
    }

    private void parseJsonCookies(String cookieData) {
        try {
            // Handle case where data is a JSON array
            if (cookieData.trim().startsWith("[")) {
                JSONArray cookieArray = new JSONArray(cookieData);
                parseJsonCookieArray(cookieArray);
            } else { // Handle case where data is a JSON object
                JSONObject jsonObject = new JSONObject(cookieData);
                if (jsonObject.has("cookies") && jsonObject.get("cookies") instanceof JSONArray) {
                    parseJsonCookieArray(jsonObject.getJSONArray("cookies"));
                } else {
                    // Fallback for other object structures if needed
                    Log.w(TAG, "Unsupported JSON object structure for cookies.");
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON cookies", e);
            Toast.makeText(this, "Error parsing JSON: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void parseJsonCookieArray(JSONArray cookieArray) throws JSONException {
        for (int i = 0; i < cookieArray.length(); i++) {
            JSONObject cookieObj = cookieArray.getJSONObject(i);
            String name = cookieObj.optString("name", "");
            String value = cookieObj.optString("value", "");
            String domain = cookieObj.optString("domain", "");
            String path = cookieObj.optString("path", "/");
            boolean secure = cookieObj.optBoolean("secure", false);
            boolean httpOnly = cookieObj.optBoolean("httpOnly", false );
            // Cookie expiration can be in seconds (unix timestamp) or milliseconds
            long expiration = cookieObj.optLong("expirationDate", -1);
            if (expiration == -1) {
                expiration = cookieObj.optLong("expires", -1);
            }

            if (!name.isEmpty() && !domain.isEmpty()) {
                Cookie cookie = new Cookie(name, value, domain, path, expiration, secure, httpOnly );
                cookieList.add(cookie);
            }
        }
    }

    private void parseNetscapeCookies(String cookieData) {
        String[] lines = cookieData.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] parts = line.split("\t");
            if (parts.length >= 7) {
                try {
                    String domain = parts[0];
                    // parts[1] is tailmatch, we don't need it
                    String path = parts[2];
                    boolean secure = Boolean.parseBoolean(parts[3]);
                    long expiration = Long.parseLong(parts[4]);
                    String name = parts[5];
                    String value = parts[6];
                    Cookie cookie = new Cookie(name, value, domain, path, expiration, secure, false); // Netscape doesn't have HttpOnly flag
                    cookieList.add(cookie);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Skipping malformed Netscape line: " + line, e);
                }
            }
        }
    }

    // --- YEH FUNCTION SAB SE AHEM HAI ---
    private void importSelectedCookies(List<Cookie> selectedCookies) {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true); // Make sure cookies are enabled
        String firstValidDomain = null;

        for (Cookie cookie : selectedCookies) {
            // 1. Sahi URL banayein
            String domain = cookie.domain;
            if (domain == null || domain.isEmpty()) continue; // Skip cookies without a domain

            // Domain ke shuru se dot (.) hata dein agar hai
            if (domain.startsWith(".")) {
                domain = domain.substring(1);
            }
            // URL hamesha https:// se banayein
            String url = "https://" + domain;

            // 2. Sahi Cookie String banayein
            StringBuilder cookieString = new StringBuilder( );
            cookieString.append(cookie.name).append("=").append(cookie.value);
            cookieString.append("; domain=").append(cookie.domain); // Domain add karna bohot zaroori hai
            cookieString.append("; path=").append(cookie.path);

            // 3. Sahi Expiration Date Format
            if (cookie.expires > 0) {
                // Convert Unix timestamp (seconds) to RFC1123 format date string
                Date expiryDate = new Date(cookie.expires * 1000L);
                SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                cookieString.append("; expires=").append(sdf.format(expiryDate));
            }

            if (cookie.secure) {
                cookieString.append("; Secure");
            }
            if (cookie.httpOnly ) {
                cookieString.append("; HttpOnly");
            }

            // 4. Cookie set karein
            cookieManager.setCookie(url, cookieString.toString());
            Log.d(TAG, "Setting cookie for URL '" + url + "': " + cookieString.toString());

            // Pehla valid domain save kar lein taake WebView ko wahan redirect kar sakein
            if (firstValidDomain == null) {
                firstValidDomain = url;
            }
        }

        // 5. Aakhir mein ek baar flush karein
        cookieManager.flush();

        Toast.makeText(this, "Imported " + selectedCookies.size() + " cookies.", Toast.LENGTH_LONG).show();

        // 6. MainActivity ko batayein ke page reload karna hai
        if (firstValidDomain != null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("reload_url", firstValidDomain);
            setResult(Activity.RESULT_OK, resultIntent);
        } else {
            setResult(Activity.RESULT_CANCELED);
        }
        finish(); // Activity band kar dein
    }

    @Override
    public void onCookieCheckedChanged(Cookie cookie, boolean isChecked) {
        List<Cookie> selected = cookieAdapter.getSelectedCookies();
        List<Cookie> filtered = cookieAdapter.getFilteredCookies();
        if (filtered.isEmpty()) {
            selectAllCheckBox.setChecked(false);
        } else {
            selectAllCheckBox.setChecked(selected.size() == filtered.size());
        }
    }
}
