package com.disdroid

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.view.Gravity
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.text.InputType
import org.json.JSONObject

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var cssEditor: EditText

    private val preferences by lazy {
        getSharedPreferences("disdroid_settings", MODE_PRIVATE)
    }

    private val defaultCss = """
/* DISDROID CUSTOM CSS */

/* Main background */
body {
    background-color: #17151f !important;
}

/* Example: rounded buttons */
button {
    border-radius: 12px !important;
}

/* Example: purple accent */
:root {
    --brand-experiment: #a855f7;
}

/* Add your own CSS below */

""".trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CookieManager.getInstance().setAcceptCookie(true)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(15, 15, 20))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14, 12, 14, 12)
            setBackgroundColor(Color.rgb(24, 22, 32))
        }

        val title = TextView(this).apply {
            text = "DisDroid"
            textSize = 21f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val cssButton = Button(this).apply {
            text = "CSS"
            setOnClickListener {
                showCssEditor()
            }
        }

        val reloadButton = Button(this).apply {
            text = "Reload"
            setOnClickListener {
                webView.reload()
            }
        }

        header.addView(
            title,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )

        header.addView(cssButton)
        header.addView(reloadButton)

        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(15, 15, 20))

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true

            webChromeClient = WebChromeClient()

            webViewClient = object : WebViewClient() {

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {

                    val url = request.url

                    if (url.scheme == "disdroid") {
                        when (url.host) {
                            "css" -> showCssEditor()
                        }
                        return true
                    }

                    val host = url.host ?: return true

                    return !(
                        host == "discord.com" ||
                        host.endsWith(".discord.com") ||
                        host == "discordapp.com" ||
                        host.endsWith(".discordapp.com")
                    )
                }

                override fun onPageFinished(
                    view: WebView,
                    url: String
                ) {
                    super.onPageFinished(view, url)

                    if (url.startsWith("https://discord.com/")) {
                        applyCustomCss()
                        injectSettingsEntry()
                    }
                }
            }
        }

        root.addView(header)

        root.addView(
            webView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)

        webView.loadUrl("https://discord.com/app")
    }

    private fun applyCustomCss() {

        val css = preferences.getString("custom_css", defaultCss)
            ?: defaultCss

        val quotedCss = JSONObject.quote(css)

        val javascript = """
            (function() {
                const css = $quotedCss;

                let style = document.getElementById('disdroid-custom-css');

                if (!style) {
                    style = document.createElement('style');
                    style.id = 'disdroid-custom-css';
                    document.head.appendChild(style);
                }

                style.textContent = css;
            })();
        """.trimIndent()

        webView.evaluateJavascript(javascript, null)
    }

    private fun injectSettingsEntry() {

        val javascript = """
            (function() {

                if (window.disdroidObserver) {
                    return;
                }

                function addDisDroidButton() {

                    if (document.getElementById('disdroid-settings-entry')) {
                        return;
                    }

                    const elements = Array.from(
                        document.querySelectorAll(
                            'button, [role="button"], [role="menuitem"]'
                        )
                    );

                    const settingsItem = elements.find(element => {
                        const text = (element.innerText || '').trim();

                        return (
                            text === 'Appearance' ||
                            text === 'My Account'
                        );
                    });

                    if (!settingsItem) return;

                    const button = settingsItem.cloneNode(true);

                    button.id = 'disdroid-settings-entry';
                    button.innerText = 'DisDroid';
                    button.removeAttribute('aria-current');

                    button.addEventListener('click', function(event) {
                        event.preventDefault();
                        event.stopPropagation();

                        window.location.href = 'disdroid://css';
                    });

                    settingsItem.parentNode.insertBefore(
                        button,
                        settingsItem.nextSibling
                    );
                }

                addDisDroidButton();

                window.disdroidObserver = new MutationObserver(function() {
                    addDisDroidButton();
                });

                window.disdroidObserver.observe(
                    document.body,
                    {
                        childList: true,
                        subtree: true
                    }
                );

            })();
        """.trimIndent()

        webView.evaluateJavascript(javascript, null)
    }

    private fun showCssEditor() {

        val savedCss = preferences.getString("custom_css", defaultCss)
            ?: defaultCss

        cssEditor = EditText(this).apply {
            setText(savedCss)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            textSize = 13f
            gravity = Gravity.TOP or Gravity.START
            typeface = android.graphics.Typeface.MONOSPACE

            inputType =
                InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

            minLines = 16
            setPadding(18, 18, 18, 18)
            setBackgroundColor(Color.rgb(29, 27, 38))
        }

        val scrollView = ScrollView(this).apply {
            setPadding(16, 8, 16, 8)
            addView(
                cssEditor,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("DisDroid CSS Editor")
            .setView(scrollView)
            .setPositiveButton("Save & Apply", null)
            .setNeutralButton("Reset", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    val css = cssEditor.text.toString()

                    preferences.edit()
                        .putString("custom_css", css)
                        .apply()

                    applyCustomCss()

                    Toast.makeText(
                        this,
                        "CSS saved and applied!",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()
                }

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setOnClickListener {

                    cssEditor.setText(defaultCss)
                }
        }

        dialog.show()
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.destroy()
        }

        super.onDestroy()
    }
}
