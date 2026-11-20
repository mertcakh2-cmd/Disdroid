package com.disdroid

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Toast
import org.json.JSONObject

class MainActivity : Activity() {

    private lateinit var webView: WebView

    private val prefs by lazy {
        getSharedPreferences("disdroid_settings", MODE_PRIVATE)
    }

    private val defaultCss = """
        /* DISDROID THEME */

        :root {
            --brand-experiment: #a855f7 !important;
            --brand-experiment-560: #9333ea !important;
        }

        body {
            background-color: #17151f !important;
        }

        button {
            border-radius: 12px !important;
        }
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.discordWebView)

        findViewById<Button>(R.id.cssButton).setOnClickListener {
            showCssEditor()
        }

        findViewById<Button>(R.id.settingsButton).setOnClickListener {
            showDisDroidSettings()
        }

        findViewById<Button>(R.id.reloadButton).setOnClickListener {
            loadDiscord()
        }

        configureWebView()
        loadDiscord()
    }

    @Suppress("SetJavaScriptEnabled")
    private fun configureWebView() {

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.apply {

            setBackgroundColor(Color.rgb(24, 22, 32))

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true

                loadsImagesAutomatically = true
                javaScriptCanOpenWindowsAutomatically = true

                setSupportMultipleWindows(false)

                useWideViewPort = true
                loadWithOverviewMode = true

                builtInZoomControls = false
                displayZoomControls = false

                mediaPlaybackRequiresUserGesture = true

                // Remove Android WebView's identifying marker.
                userAgentString = userAgentString
                    .replace("; wv)", ")")
                    .replace("Version/4.0 ", "")
            }

            webChromeClient = WebChromeClient()

            webViewClient = object : WebViewClient() {

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {

                    val uri = request.url

                    // Handle DisDroid's private navigation commands.
                    if (uri.scheme == "disdroid") {

                        when (uri.host) {
                            "settings" -> showDisDroidSettings()
                            "css" -> showCssEditor()
                        }

                        return true
                    }

                    // IMPORTANT:
                    // Allow HTTPS redirects, including Discord login,
                    // verification and authentication providers.
                    if (uri.scheme == "https") {
                        return false
                    }

                    // Never allow insecure HTTP navigation.
                    if (uri.scheme == "http") {
                        Toast.makeText(
                            this@MainActivity,
                            "Blocked insecure HTTP navigation",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    return true
                }

                override fun onPageFinished(
                    view: WebView,
                    url: String
                ) {
                    super.onPageFinished(view, url)

                    if (url.startsWith("https://")) {
                        applyCustomCss()
                        DiscordInjector.install(view)
                    }
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    super.onReceivedError(view, request, error)

                    if (request.isForMainFrame) {
                        Toast.makeText(
                            this@MainActivity,
                            "Loading failed: ${error.description}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView,
                    request: WebResourceRequest,
                    response: WebResourceResponse
                ) {
                    super.onReceivedHttpError(view, request, response)

                    if (request.isForMainFrame &&
                        response.statusCode >= 400
                    ) {
                        Toast.makeText(
                            this@MainActivity,
                            "HTTP ${response.statusCode}. Try reloading.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onReceivedSslError(
                    view: WebView,
                    handler: SslErrorHandler,
                    error: android.net.http.SslError
                ) {
                    // Never bypass SSL certificate errors.
                    handler.cancel()

                    Toast.makeText(
                        this@MainActivity,
                        "Secure connection failed.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun loadDiscord() {
        webView.loadUrl("https://discord.com/app")
    }

    private fun applyCustomCss() {

        val css = prefs.getString(
            "custom_css",
            defaultCss
        ) ?: defaultCss

        val safeCss = JSONObject.quote(css)

        val javascript = """
            (function() {

                if (!document.head) return;

                let style = document.getElementById(
                    'disdroid-custom-css'
                );

                if (!style) {
                    style = document.createElement('style');
                    style.id = 'disdroid-custom-css';
                    document.head.appendChild(style);
                }

                style.textContent = $safeCss;

            })();
        """.trimIndent()

        webView.evaluateJavascript(javascript, null)
    }

    private fun showDisDroidSettings() {

        val options = arrayOf(
            "Custom CSS Editor",
            "Reset CSS",
            "Reload Discord"
        )

        AlertDialog.Builder(this)
            .setTitle("DisDroid Settings")
            .setItems(options) { _, which ->

                when (which) {

                    0 -> showCssEditor()

                    1 -> {
                        prefs.edit()
                            .remove("custom_css")
                            .apply()

                        applyCustomCss()

                        Toast.makeText(
                            this,
                            "CSS reset",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    2 -> loadDiscord()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showCssEditor() {

        val editor = EditText(this).apply {

            setText(
                prefs.getString("custom_css", defaultCss)
                    ?: defaultCss
            )

            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)

            textSize = 13f

            typeface = android.graphics.Typeface.MONOSPACE

            gravity = Gravity.TOP or Gravity.START

            minLines = 16

            inputType =
                InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

            setPadding(18, 18, 18, 18)

            setBackgroundColor(
                Color.rgb(29, 27, 38)
            )
        }

        val scroll = ScrollView(this).apply {

            setPadding(16, 8, 16, 8)

            addView(
                editor,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("DisDroid CSS Editor")
            .setView(scroll)
            .setPositiveButton("Save & Apply", null)
            .setNeutralButton("Reset", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                prefs.edit()
                    .putString(
                        "custom_css",
                        editor.text.toString()
                    )
                    .apply()

                applyCustomCss()

                Toast.makeText(
                    this,
                    "CSS saved!",
                    Toast.LENGTH_SHORT
                ).show()

                dialog.dismiss()
            }

            dialog.getButton(
                AlertDialog.BUTTON_NEUTRAL
            ).setOnClickListener {

                editor.setText(defaultCss)
            }
        }

        dialog.show()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {

        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }

        super.onDestroy()
    }
}
