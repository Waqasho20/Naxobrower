package com.naxobrowser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText urlEditText;
    private ImageView backButton, forwardButton, refreshButton, homeButton;
    private ImageView lockIcon, braveShieldIcon, menuIcon;
    private ProgressBar progressBar;

    // --- NAYA STEP 1: ActivityResultLauncher declare karein ---
    // Yeh CookieManagerActivity se wapas aane wale result ko handle karega.
    private ActivityResultLauncher<Intent> cookieActivityLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);

        // Views ko initialize karein
        webView = findViewById(R.id.webView);
        urlEditText = findViewById(R.id.urlEditText);
        progressBar = findViewById(R.id.progressBar);
        lockIcon = findViewById(R.id.lockIcon);
        braveShieldIcon = findViewById(R.id.braveShieldIcon);
        menuIcon = findViewById(R.id.menuIcon);
        backButton = findViewById(R.id.backButton);
        forwardButton = findViewById(R.id.forwardButton);
        refreshButton = findViewById(R.id.refreshButton);
        homeButton = findViewById(R.id.homeButton);

        // --- NAYA STEP 2: Launcher ko register karein ---
        // Yeh batata hai ke jab activity se result wapas aaye to kya karna hai.
        cookieActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String reloadUrl = result.getData().getStringExtra("reload_url");
                        if (reloadUrl != null && !reloadUrl.isEmpty()) {
                            webView.loadUrl(reloadUrl);
                            Toast.makeText(this, "Cookies imported. Reloading page...", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // --- WebView ki Final Settings ---
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        String userAgent = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.58 Mobile Safari/537.36";
        webSettings.setUserAgentString(userAgent);

        // WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                urlEditText.setText(url);
                updateLockIcon(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                backButton.setEnabled(view.canGoBack());
                forwardButton.setEnabled(view.canGoForward());
                backButton.setAlpha(view.canGoBack() ? 1.0f : 0.5f);
                forwardButton.setAlpha(view.canGoForward() ? 1.0f : 0.5f);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        });

        // WebChromeClient
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
        });

        // Address Bar Logic
        urlEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                String input = urlEditText.getText().toString().trim();
                if (!input.isEmpty()) {
                    hideKeyboard();
                    urlEditText.clearFocus();
                    if (Patterns.WEB_URL.matcher(input).matches() || input.contains(".")) {
                        webView.loadUrl(input.startsWith("http" ) ? input : "https://" + input );
                    } else {
                        try {
                            String searchUrl = "https://www.google.com/search?q=" + URLEncoder.encode(input, "UTF-8" );
                            webView.loadUrl(searchUrl);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                return true;
            }
            return false;
        });

        // Button Click Listeners
        backButton.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        forwardButton.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
        refreshButton.setOnClickListener(v -> webView.reload());
        homeButton.setOnClickListener(v -> webView.loadUrl("https://www.google.com" ));
        braveShieldIcon.setOnClickListener(v -> Toast.makeText(this, "Shield Clicked!", Toast.LENGTH_SHORT).show());
        menuIcon.setOnClickListener(this::showMenu); // Updated to method reference

        // Shuru mein Google load karein
        webView.loadUrl("https://www.google.com" );
    }

    private void showMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        // Aapko res/menu/browser_menu.xml file banani hogi
        // Agar nahi hai to main neeche uska code de raha hoon
        popup.getMenuInflater().inflate(R.menu.browser_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_cookies) {
                // --- NAYA STEP 3: Launcher ke zariye Activity ko start karein ---
                Intent intent = new Intent(this, CookieManagerActivity.class);
                intent.putExtra("current_url", webView.getUrl());
                cookieActivityLauncher.launch(intent); // startActivity ke bajaye launch istemal karein
                return true;
            }
            // Yahan aap Import Cookies ke liye bhi alag se option daal sakte hain
            // aur usay bhi launcher se hi call kar sakte hain.
            else if (id == R.id.action_clear_data) {
                clearBrowserData();
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void clearBrowserData() {
        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();
        android.webkit.CookieManager.getInstance().removeAllCookies(null);
        android.webkit.CookieManager.getInstance().flush();
        Toast.makeText(this, "All browser data cleared!", Toast.LENGTH_SHORT).show();
        webView.loadUrl("https://www.google.com" ); // Sab clear karke home par jayein
    }

    private void updateLockIcon(String url) {
        if (url != null && url.startsWith("https://" )) {
            lockIcon.setImageResource(R.drawable.ic_lock);
        } else {
            lockIcon.setImageResource(R.drawable.ic_search);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
