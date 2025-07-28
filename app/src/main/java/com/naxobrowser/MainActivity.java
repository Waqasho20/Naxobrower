package com.naxobrowser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
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
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText urlEditText;
    private ImageView backButton, forwardButton, refreshButton, homeButton;
    private ImageView lockIcon, braveShieldIcon, menuIcon;
    private ProgressBar progressBar;

    // URL History Management
    private List<String> urlHistory;
    private int currentHistoryIndex = -1;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "BrowserPrefs";
    private static final String LAST_URL_KEY = "lastUrl";

    // ActivityResultLauncher for CookieManagerActivity
    private ActivityResultLauncher<Intent> cookieActivityLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);

        // Initialize Views
        initializeViews();
        
        // Initialize URL History
        urlHistory = new ArrayList<>();
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Register ActivityResultLauncher
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

        // Setup WebView
        setupWebView();
        
        // Setup Button Listeners
        setupButtonListeners();
        
        // Setup Address Bar
        setupAddressBar();

        // Load last URL or default homepage
        loadInitialPage();
    }

    private void initializeViews() {
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
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        
        // Basic Settings
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // Cache Settings for better connectivity
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setAppCacheEnabled(true);
        webSettings.setAppCachePath(getCacheDir().getAbsolutePath());
        
        // Enhanced User Agent
        String userAgent = "Mozilla/5.0 (Linux; Android 11; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
        webSettings.setUserAgentString(userAgent);

        // WebViewClient with enhanced error handling
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                urlEditText.setText(url);
                updateLockIcon(url);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                
                // Add to history if it's a new URL
                addToHistory(url);
                
                // Update navigation buttons
                updateNavigationButtons();
                
                // Save current URL
                saveCurrentUrl(url);
                
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                
                if (request.isForMainFrame()) {
                    String errorUrl = request.getUrl().toString();
                    String errorMessage = "Connection failed for: " + errorUrl;
                    
                    // Try to load cached version
                    webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                    
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    
                    // Reset cache mode after error
                    webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // Accept SSL errors (use with caution in production)
                handler.proceed();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // Handle special URL schemes
                if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(request.getUrl());
                        startActivity(intent);
                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
                return false;
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
    }

    private void setupButtonListeners() {
        backButton.setOnClickListener(v -> goBackInHistory());
        forwardButton.setOnClickListener(v -> goForwardInHistory());
        refreshButton.setOnClickListener(v -> webView.reload());
        homeButton.setOnClickListener(v -> loadUrl("https://www.google.com"));
        braveShieldIcon.setOnClickListener(v -> Toast.makeText(this, "Shield Clicked!", Toast.LENGTH_SHORT).show());
        menuIcon.setOnClickListener(this::showMenu);
    }

    private void setupAddressBar() {
        urlEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                
                String input = urlEditText.getText().toString().trim();
                if (!input.isEmpty()) {
                    hideKeyboard();
                    urlEditText.clearFocus();
                    
                    String urlToLoad;
                    if (Patterns.WEB_URL.matcher(input).matches() || 
                        input.contains(".") && !input.contains(" ")) {
                        urlToLoad = input.startsWith("http") ? input : "https://" + input;
                    } else {
                        try {
                            urlToLoad = "https://www.google.com/search?q=" + URLEncoder.encode(input, "UTF-8");
                        } catch (Exception e) {
                            urlToLoad = "https://www.google.com/search?q=" + input.replace(" ", "+");
                        }
                    }
                    loadUrl(urlToLoad);
                }
                return true;
            }
            return false;
        });
    }

    private void loadUrl(String url) {
        webView.loadUrl(url);
    }

    private void loadInitialPage() {
        String lastUrl = sharedPreferences.getString(LAST_URL_KEY, "https://www.google.com");
        loadUrl(lastUrl);
    }

    private void addToHistory(String url) {
        if (url != null && !url.equals("about:blank")) {
            // Remove any URLs after current index (when going back and then to new URL)
            if (currentHistoryIndex < urlHistory.size() - 1) {
                urlHistory = urlHistory.subList(0, currentHistoryIndex + 1);
            }
            
            // Add new URL if it's different from the last one
            if (urlHistory.isEmpty() || !urlHistory.get(urlHistory.size() - 1).equals(url)) {
                urlHistory.add(url);
                currentHistoryIndex = urlHistory.size() - 1;
            }
        }
    }

    private void goBackInHistory() {
        if (currentHistoryIndex > 0) {
            currentHistoryIndex--;
            String url = urlHistory.get(currentHistoryIndex);
            webView.loadUrl(url);
            updateNavigationButtons();
        } else if (webView.canGoBack()) {
            webView.goBack();
        }
    }

    private void goForwardInHistory() {
        if (currentHistoryIndex < urlHistory.size() - 1) {
            currentHistoryIndex++;
            String url = urlHistory.get(currentHistoryIndex);
            webView.loadUrl(url);
            updateNavigationButtons();
        } else if (webView.canGoForward()) {
            webView.goForward();
        }
    }

    private void updateNavigationButtons() {
        boolean canGoBack = (currentHistoryIndex > 0) || webView.canGoBack();
        boolean canGoForward = (currentHistoryIndex < urlHistory.size() - 1) || webView.canGoForward();
        
        backButton.setEnabled(canGoBack);
        forwardButton.setEnabled(canGoForward);
        backButton.setAlpha(canGoBack ? 1.0f : 0.5f);
        forwardButton.setAlpha(canGoForward ? 1.0f : 0.5f);
    }

    private void saveCurrentUrl(String url) {
        if (url != null && !url.equals("about:blank")) {
            sharedPreferences.edit().putString(LAST_URL_KEY, url).apply();
        }
    }

    private void showMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.browser_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_cookies) {
                Intent intent = new Intent(this, CookieManagerActivity.class);
                intent.putExtra("current_url", webView.getUrl());
                cookieActivityLauncher.launch(intent);
                return true;
            } else if (id == R.id.action_clear_data) {
                clearBrowserData();
                return true;
            } else if (id == R.id.action_history) {
                showHistoryMenu(view);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void showHistoryMenu(View view) {
        if (urlHistory.isEmpty()) {
            Toast.makeText(this, "No history available", Toast.LENGTH_SHORT).show();
            return;
        }

        PopupMenu historyPopup = new PopupMenu(this, view);
        
        // Add recent URLs to menu (last 10)
        int startIndex = Math.max(0, urlHistory.size() - 10);
        for (int i = urlHistory.size() - 1; i >= startIndex; i--) {
            String url = urlHistory.get(i);
            String displayUrl = url.length() > 50 ? url.substring(0, 50) + "..." : url;
            historyPopup.getMenu().add(0, i, 0, displayUrl);
        }

        historyPopup.setOnMenuItemClickListener(item -> {
            int index = item.getItemId();
            if (index >= 0 && index < urlHistory.size()) {
                currentHistoryIndex = index;
                webView.loadUrl(urlHistory.get(index));
                updateNavigationButtons();
            }
            return true;
        });

        historyPopup.show();
    }

    private void clearBrowserData() {
        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();
        android.webkit.CookieManager.getInstance().removeAllCookies(null);
        android.webkit.CookieManager.getInstance().flush();
        
        // Clear custom history
        urlHistory.clear();
        currentHistoryIndex = -1;
        
        // Clear saved URL
        sharedPreferences.edit().remove(LAST_URL_KEY).apply();
        
        Toast.makeText(this, "All browser data cleared!", Toast.LENGTH_SHORT).show();
        loadUrl("https://www.google.com");
    }

    private void updateLockIcon(String url) {
        if (url != null && url.startsWith("https://")) {
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

    @Override
    protected void onPause() {
        super.onPause();
        // Save current URL when app goes to background
        String currentUrl = webView.getUrl();
        if (currentUrl != null) {
            saveCurrentUrl(currentUrl);
        }
    }
}
