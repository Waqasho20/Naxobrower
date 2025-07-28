package com.naxobrowser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                            @Override
    protected void onPause() {
        super.onPause();
        String currentUrl = webView.getUrl();
        if (currentUrl != null) {
            saveCurrentUrl(currentUrl);
        }
    }
}
                    }
                });

        // Setup WebView with COMPLETE configuration
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
        // Enable debugging for testing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        WebSettings webSettings = webView.getSettings();
        
        // COMPLETE WebView Settings for maximum compatibility
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // Network Settings - CRITICAL for connection issues
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setBlockNetworkImage(false);
        webSettings.setBlockNetworkLoads(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        
        // Media and Content Settings
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setGeolocationEnabled(true);
        webSettings.setSaveFormData(true);
        webSettings.setSavePassword(true);
        
        // User Agent - Mobile for better compatibility with mobile sites
        String mobileUserAgent = "Mozilla/5.0 (Linux; Android 11; SM-A307FN Build/RP1A.200720.012; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/138.0.7204.157 Mobile Safari/537.36";
        webSettings.setUserAgentString(mobileUserAgent);

        // Cookie Management - ESSENTIAL
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // WebViewClient with COMPLETE error handling and URL interception
        webView.setWebViewClient(new WebViewClient() {
            
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // Add custom headers to all requests
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // Handle special schemes
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
                
                // Load all HTTP/HTTPS URLs in WebView
                return false;
            }

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
                
                // Inject CSS to improve mobile experience
                String css = "javascript:(function() {" +
                        "var meta = document.createElement('meta');" +
                        "meta.name = 'viewport';" +
                        "meta.content = 'width=device-width, initial-scale=1.0';" +
                        "document.getElementsByTagName('head')[0].appendChild(meta);" +
                        "})()";
                view.loadUrl(css);
                
                // Add to history
                addToHistory(url);
                updateNavigationButtons();
                saveCurrentUrl(url);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                
                if (request.isForMainFrame()) {
                    String failedUrl = request.getUrl().toString();
                    String errorDescription = error.getDescription().toString();
                    
                    // Try alternative approaches
                    if (failedUrl.contains("facebook.com") && !failedUrl.contains("m.facebook.com")) {
                        String mobileUrl = failedUrl.replace("www.facebook.com", "m.facebook.com")
                                                   .replace("facebook.com", "m.facebook.com");
                        Toast.makeText(MainActivity.this, "Trying mobile version...", Toast.LENGTH_SHORT).show();
                        view.loadUrl(mobileUrl);
                        return;
                    }
                    
                    // Try HTTPS if HTTP failed
                    if (failedUrl.startsWith("http://")) {
                        String httpsUrl = failedUrl.replace("http://", "https://");
                        Toast.makeText(MainActivity.this, "Trying secure connection...", Toast.LENGTH_SHORT).show();
                        view.loadUrl(httpsUrl);
                        return;
                    }
                    
                    // Try without www
                    if (failedUrl.contains("www.")) {
                        String noWwwUrl = failedUrl.replace("www.", "");
                        Toast.makeText(MainActivity.this, "Trying without www...", Toast.LENGTH_SHORT).show();
                        view.loadUrl(noWwwUrl);
                        return;
                    }
                    
                    Toast.makeText(MainActivity.this, "Connection failed: " + errorDescription, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // Always proceed with SSL errors to avoid connection blocks
                handler.proceed();
            }
        });

        // WebChromeClient for better JavaScript support
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

            @Override
            public boolean onJsAlert(WebView view, String url, String message, android.webkit.JsResult result) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                result.confirm();
                return true;
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
        
        // Long press on menu icon to show history
        menuIcon.setOnLongClickListener(v -> {
            showHistoryMenu(v);
            return true;
        });
    }

    private void setupAddressBar() {
        urlEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                
                String input = urlEditText.getText().toString().trim();
                if (!input.isEmpty()) {
                    hideKeyboard();
                    urlEditText.clearFocus();
                    
                    String urlToLoad = processUrlInput(input);
                    loadUrl(urlToLoad);
                }
                return true;
            }
            return false;
        });
    }

    private String processUrlInput(String input) {
        // If it looks like a URL
        if (Patterns.WEB_URL.matcher(input).matches() || 
            (input.contains(".") && !input.contains(" "))) {
            
            if (!input.startsWith("http://") && !input.startsWith("https://")) {
                return "https://" + input;
            }
            return input;
        } 
        // Otherwise, search
        else {
            try {
                return "https://www.google.com/search?q=" + URLEncoder.encode(input, "UTF-8");
            } catch (Exception e) {
                return "https://www.google.com/search?q=" + input.replace(" ", "+");
            }
        }
    }

    private void loadUrl(String url) {
        // Add proper headers matching the user agent
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        headers.put("Accept-Language", "en-US,en;q=0.5");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("DNT", "1");
        headers.put("Connection", "keep-alive");
        headers.put("Upgrade-Insecure-Requests", "1");
        headers.put("Sec-Fetch-Dest", "document");
        headers.put("Sec-Fetch-Mode", "navigate");
        headers.put("Sec-Fetch-Site", "none");
        headers.put("Sec-Fetch-User", "?1");
        
        webView.loadUrl(url, headers);
    }

    private void loadInitialPage() {
        String lastUrl = sharedPreferences.getString(LAST_URL_KEY, "https://www.google.com");
        loadUrl(lastUrl);
    }

    private void addToHistory(String url) {
        if (url != null && !url.equals("about:blank") && !url.startsWith("javascript:")) {
            if (currentHistoryIndex < urlHistory.size() - 1) {
                urlHistory = urlHistory.subList(0, currentHistoryIndex + 1);
            }
            
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
            } else if (id == R.id.action_import_cookies) {
                // Import cookies functionality
                Intent intent = new Intent(this, CookieManagerActivity.class);
                intent.putExtra("import_mode", true);
                intent.putExtra("current_url", webView.getUrl());
                cookieActivityLauncher.launch(intent);
                return true;
            } else if (id == R.id.action_clear_data) {
                clearBrowserData();
                return true;
            } else if (id == R.id.action_settings) {
                showSettingsDialog();
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
                loadUrl(urlHistory.get(index));
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
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        
        urlHistory.clear();
        currentHistoryIndex = -1;
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

    private void showSettingsDialog() {
        PopupMenu settingsPopup = new PopupMenu(this, menuIcon);
        settingsPopup.getMenu().add(0, 1, 0, "Switch to Desktop Mode");
        settingsPopup.getMenu().add(0, 2, 0, "Switch to Mobile Mode");
        settingsPopup.getMenu().add(0, 3, 0, "Enable JavaScript");
        settingsPopup.getMenu().add(0, 4, 0, "Disable JavaScript");
        
        settingsPopup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            WebSettings settings = webView.getSettings();
            
            switch (id) {
                case 1: // Desktop Mode
                    String desktopUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
                    settings.setUserAgentString(desktopUA);
                    webView.reload();
                    Toast.makeText(this, "Switched to Desktop Mode", Toast.LENGTH_SHORT).show();
                    break;
                case 2: // Mobile Mode
                    String mobileUA = "Mozilla/5.0 (Linux; Android 11; SM-A307FN Build/RP1A.200720.012; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/138.0.7204.157 Mobile Safari/537.36";
                    settings.setUserAgentString(mobileUA);
                    webView.reload();
                    Toast.makeText(this, "Switched to Mobile Mode", Toast.LENGTH_SHORT).show();
                    break;
                case 3: // Enable JavaScript
                    settings.setJavaScriptEnabled(true);
                    webView.reload();
                    Toast.makeText(this, "JavaScript Enabled", Toast.LENGTH_SHORT).show();
                    break;
                case 4: // Disable JavaScript
                    settings.setJavaScriptEnabled(false);
                    webView.reload();
                    Toast.makeText(this, "JavaScript Disabled", Toast.LENGTH_SHORT).show();
                    break;
            }
            return true;
        });
        
        settingsPopup.show();
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
