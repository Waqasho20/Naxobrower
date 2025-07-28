package com.naxobrowser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Views
    private WebView webView;
    private EditText urlEditText;
    private ProgressBar progressBar;
    private ImageView backButton, forwardButton, refreshButton, homeButton, menuIcon, lockIcon;

    // SharedPreferences for saving data
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "NaxoBrowserPrefs";
    private static final String LAST_URL_KEY = "last_url";
    private static final String DEFAULT_HOME_PAGE = "https://www.google.com";

    // For debugging
    private static final String TAG = "NaxoBrowser";

    // Activity Result Launcher
    private ActivityResultLauncher<Intent> cookieActivityLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);

        // Initialize all components
        initializeViews();
        setupWebView();
        setupListeners();
        setupActivityResultLaunchers();

        // Load the last visited page or the default homepage
        loadInitialPage();
    }

    private void initializeViews() {
        webView = findViewById(R.id.webView);
        urlEditText = findViewById(R.id.urlEditText);
        progressBar = findViewById(R.id.progressBar);
        backButton = findViewById(R.id.backButton);
        forwardButton = findViewById(R.id.forwardButton);
        refreshButton = findViewById(R.id.refreshButton);
        homeButton = findViewById(R.id.homeButton);
        menuIcon = findViewById(R.id.menuIcon);
        lockIcon = findViewById(R.id.lockIcon);
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new MyWebViewClient());
        webView.setWebChromeClient(new MyWebChromeClient());
    }

    private void setupListeners() {
        // Navigation buttons
        backButton.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        forwardButton.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
        refreshButton.setOnClickListener(v -> webView.reload());
        homeButton.setOnClickListener(v -> webView.loadUrl(DEFAULT_HOME_PAGE));
        menuIcon.setOnClickListener(this::showPopupMenu);

        // Address bar listener
        urlEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadUrlFromInput();
                return true;
            }
            return false;
        });
    }

    private void setupActivityResultLaunchers() {
        cookieActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String reloadUrl = result.getData().getStringExtra("reload_url");
                        if (reloadUrl != null && !reloadUrl.isEmpty()) {
                            webView.loadUrl(reloadUrl);
                        }
                    }
                });
    }

    private void loadUrlFromInput() {
        hideKeyboard();
        String input = urlEditText.getText().toString().trim();
        if (input.isEmpty()) return;

        String urlToLoad;

        // Check if it's a full URL
        if (input.startsWith("http://") || input.startsWith("https://")) {
            urlToLoad = input;
        }
        // Check if it's a domain name (e.g., facebook.com)
        else if (input.contains(".") && !input.contains(" ")) {
            urlToLoad = "https://" + input;
        }
        // Otherwise, treat it as a search query
        else {
            try {
                String encodedQuery = URLEncoder.encode(input, "UTF-8");
                urlToLoad = "https://www.google.com/search?q=" + encodedQuery;
            } catch (Exception e) {
                Log.e(TAG, "URL encoding failed", e);
                urlToLoad = "https://www.google.com/search?q=" + input.replace(" ", "+");
            }
        }

        Log.d(TAG, "Loading URL: " + urlToLoad);
        webView.loadUrl(urlToLoad);
    }

    private void loadInitialPage() {
        String lastUrl = sharedPreferences.getString(LAST_URL_KEY, DEFAULT_HOME_PAGE);
        webView.loadUrl(lastUrl);
    }

    private void updateNavigationButtons() {
        backButton.setEnabled(webView.canGoBack());
        forwardButton.setEnabled(webView.canGoForward());
        backButton.setAlpha(webView.canGoBack() ? 1.0f : 0.5f);
        forwardButton.setAlpha(webView.canGoForward() ? 1.0f : 0.5f);
    }

    private void updateLockIcon(String url) {
        if (url != null && url.startsWith("https://")) {
            lockIcon.setImageResource(R.drawable.ic_lock);
        } else {
            lockIcon.setImageResource(R.drawable.ic_search); // Ya koi aur "unlocked" icon
        }
    }

    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.browser_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_cookies) {
                Intent intent = new Intent(this, CookieManagerActivity.class);
                intent.putExtra("current_url", webView.getUrl());
                cookieActivityLauncher.launch(intent);
                return true;
            } else if (id == R.id.action_import_cookies) {
                Intent intent = new Intent(this, CookieImportActivity.class);
                intent.putExtra("current_url", webView.getUrl());
                cookieActivityLauncher.launch(intent);
                return true;
            } else if (id == R.id.action_clear_data) {
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
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        sharedPreferences.edit().clear().apply();
        Toast.makeText(this, "All browser data cleared!", Toast.LENGTH_SHORT).show();
        webView.loadUrl(DEFAULT_HOME_PAGE);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = this.getCurrentFocus();
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

    @Override
    protected void onPause() {
        super.onPause();
        // Save the current URL when the app is paused
        if (webView != null) {
            sharedPreferences.edit().putString(LAST_URL_KEY, webView.getUrl()).apply();
        }
    }

    // Inner class for WebViewClient
    private class MyWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
            urlEditText.setText(url);
            updateLockIcon(url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
            updateNavigationButtons();
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (request.isForMainFrame()) {
                Log.e(TAG, "Error loading page: " + request.getUrl().toString() + " | " + error.getDescription());
                Toast.makeText(MainActivity.this, "Failed to load: " + error.getDescription(), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            // This is not recommended for production apps, but for a personal browser it's okay.
            handler.proceed();
        }
    }

    // Inner class for WebChromeClient
    private class MyWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            progressBar.setProgress(newProgress);
        }
    }
}
