package com.naxobrowser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText urlEditText;
    private ImageView backButton, forwardButton, refreshButton, homeButton;
    private ImageView lockIcon;
    private ImageView menuIcon;
    private ProgressBar progressBar;

    private List<String> urlHistory = new ArrayList<>();
    private int currentHistoryIndex = -1;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "BrowserPrefs";
    private static final String LAST_URL_KEY = "lastUrl";
    private static final String DEFAULT_HOME_PAGE = "https://www.google.com";
    private static final String TAG = "NaxoBrowser";

    private ActivityResultLauncher<Intent> cookieActivityLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);

        initializeViews();
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        setupActivityResultLaunchers();
        setupWebView();
        setupButtonListeners();
        setupAddressBar();
        loadInitialPage();
    }

    private void initializeViews() {
        webView = findViewById(R.id.webView);
        urlEditText = findViewById(R.id.urlEditText);
        progressBar = findViewById(R.id.progressBar);
        lockIcon = findViewById(R.id.lockIcon);
        menuIcon = findViewById(R.id.menuIcon);
        backButton = findViewById(R.id.backButton);
        forwardButton = findViewById(R.id.forwardButton);
        refreshButton = findViewById(R.id.refreshButton);
        homeButton = findViewById(R.id.homeButton);
        
        // Brave shield icon ko hata diya gaya hai kyun ke uska koi ahem kaam nahi tha
        // ImageView braveShieldIcon = findViewById(R.id.braveShieldIcon);
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

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Mobile-friendly viewport settings
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // File access (agar zaroorat ho)
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        // User Agent ko default rehne dein, WebView khud behtareen agent set karta hai
        // String mobileUserAgent = "...";
        // webSettings.setUserAgentString(mobileUserAgent);

        // Cookie settings
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new MyWebViewClient());
        webView.setWebChromeClient(new MyWebChromeClient());
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            // Non-http(s) links ko bahar handle karein
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                    startActivity(intent);
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "Could not handle URL: " + url, e);
                    return true; // WebView ko isay load karne se rokein
                }
            }
            return false; // HTTP/HTTPS links ko WebView mein hi load karein
        }

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
            addToHistory(url);
            updateNavigationButtons();
            saveCurrentUrl(url);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            // Sirf main page ke errors par action lein
            if (request.isForMainFrame()) {
                int errorCode = error.getErrorCode();
                String description = error.getDescription().toString();
                String failingUrl = request.getUrl().toString();
                
                Log.e(TAG, "Error on: " + failingUrl + " | Code: " + errorCode + " | Desc: " + description);

                // User ko error dikhayein
                Toast.makeText(MainActivity.this, "Error: " + description, Toast.LENGTH_LONG).show();

                // Error page dikhayein (optional, but good for user experience)
                // view.loadData("<html><body><h1>Connection Error</h1><p>" + description + "</p></body></html>", "text/html", "UTF-8");
            }
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            // SSL errors ko handle karna zaroori hai. `proceed()` production ke liye aacha nahi.
            // Lekin testing ke liye, hum isay allow kar rahe hain.
            Log.w(TAG, "SSL Error: " + error.toString() + ". Proceeding anyway.");
            handler.proceed();
        }
    }

    private class MyWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            progressBar.setProgress(newProgress);
        }
    }

    private void setupButtonListeners() {
        backButton.setOnClickListener(v -> goBack());
        forwardButton.setOnClickListener(v -> goForward());
        refreshButton.setOnClickListener(v -> webView.reload());
        homeButton.setOnClickListener(v -> loadUrl(DEFAULT_HOME_PAGE));
        menuIcon.setOnClickListener(this::showMenu);
    }

    private void setupAddressBar() {
        urlEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadUrlFromEditText();
                return true;
            }
            return false;
        });
    }

    private void loadUrlFromEditText() {
        hideKeyboard();
        urlEditText.clearFocus();
        String input = urlEditText.getText().toString().trim();
        if (input.isEmpty()) return;

        String urlToLoad;
        if (Patterns.WEB_URL.matcher(input).matches()) {
            urlToLoad = input.startsWith("http") ? input : "https://" + input;
        } else {
            try {
                String encodedQuery = URLEncoder.encode(input, StandardCharsets.UTF_8.name());
                urlToLoad = "https://www.google.com/search?q=" + encodedQuery;
            } catch (Exception e) {
                Log.e(TAG, "URL Encoding failed", e);
                urlToLoad = "https://www.google.com/search?q=" + input;
            }
        }
        loadUrl(urlToLoad);
    }

    private void loadUrl(String url) {
        // Custom headers ko hata diya gaya hai, kyun ke yeh connection reset ki wajah ban sakte hain.
        // WebView behtareen headers khud set karta hai.
        webView.loadUrl(url);
    }

    private void loadInitialPage() {
        String lastUrl = sharedPreferences.getString(LAST_URL_KEY, DEFAULT_HOME_PAGE);
        loadUrl(lastUrl);
    }

    private void addToHistory(String url) {
        if (url == null || url.isEmpty() || url.equals("about:blank")) return;

        // Agar hum history mein peechay ja kar naya page kholte hain, to aagay ki history clear kar dein
        if (currentHistoryIndex < urlHistory.size() - 1) {
            urlHistory.subList(currentHistoryIndex + 1, urlHistory.size()).clear();
        }

        // Duplicate entry add na karein
        if (urlHistory.isEmpty() || !urlHistory.get(urlHistory.size() - 1).equals(url)) {
            urlHistory.add(url);
            currentHistoryIndex = urlHistory.size() - 1;
        }
    }

    private void goBack() {
        if (webView.canGoBack()) {
            webView.goBack();
        }
    }

    private void goForward() {
        if (webView.canGoForward()) {
            webView.goForward();
        }
    }

    private void updateNavigationButtons() {
        backButton.setEnabled(webView.canGoBack());
        forwardButton.setEnabled(webView.canGoForward());
        backButton.setAlpha(webView.canGoBack() ? 1.0f : 0.5f);
        forwardButton.setAlpha(webView.canGoForward() ? 1.0f : 0.5f);
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
            if (id == R.id.action_clear_data) {
                clearBrowserData();
                return true;
            } else if (id == R.id.action_settings) {
                Toast.makeText(this, "Settings clicked!", Toast.LENGTH_SHORT).show();
                return true;
            }
            // Baaki menu items ka logic yahan add karein
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
        urlHistory.clear();
        currentHistoryIndex = -1;
        sharedPreferences.edit().clear().apply();
        Toast.makeText(this, "All browser data cleared!", Toast.LENGTH_SHORT).show();
        loadUrl(DEFAULT_HOME_PAGE);
    }

    private void updateLockIcon(String url) {
        if (url != null && url.startsWith("https://")) {
            lockIcon.setImageResource(R.drawable.ic_lock);
        } else {
            lockIcon.setImageResource(R.drawable.ic_search); // Ya koi aur "unlocked" icon
        }
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
            goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            saveCurrentUrl(webView.getUrl());
        }
    }
}
