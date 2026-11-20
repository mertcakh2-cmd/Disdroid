package com.disdroid

import android.webkit.WebView

object DiscordInjector {

    fun install(webView: WebView) {

        val javascript = """
        (function() {

            if (!document.documentElement) return;

            const ENTRY_ID = 'disdroid-settings-entry';

            const labels = new Set([
                'my account',
                'profiles',
                'privacy & safety',
                'privacy and safety',
                'authorized apps',
                'connections',
                'appearance',
                'accessibility',
                'voice & video',
                'voice and video',
                'notifications',
                'keybinds',
                'language',
                'advanced',
                'hesabım',
                'profiller',
                'gizlilik ve güvenlik',
                'bağlantılar',
                'görünüm',
                'erişilebilirlik',
                'ses ve görüntü',
                'bildirimler',
                'dil',
                'gelişmiş'
            ]);

            function clean(value) {
                return (value || '')
                    .replace(/\s+/g, ' ')
                    .trim()
                    .toLowerCase();
            }

            function openDisDroid() {
                window.location.href = 'disdroid://settings';
            }

            function createEntry(reference) {

                if (!reference || !reference.parentElement) {
                    return false;
                }

                const entry = reference.cloneNode(true);

                entry.id = ENTRY_ID;

                entry.removeAttribute('aria-current');
                entry.removeAttribute('aria-selected');

                entry.setAttribute(
                    'aria-label',
                    'DisDroid Settings'
                );

                entry.setAttribute(
                    'data-disdroid-entry',
                    'true'
                );

                entry.textContent = 'DisDroid';

                entry.addEventListener('click', function(event) {

                    event.preventDefault();
                    event.stopPropagation();

                    openDisDroid();

                }, true);

                reference.parentElement.insertBefore(
                    entry,
                    reference.nextSibling
                );

                return true;
            }

            function addEntry() {

                if (document.getElementById(ENTRY_ID)) {
                    return;
                }

                const candidates = Array.from(
                    document.querySelectorAll(
                        'button, [role="button"], [role="menuitem"], a'
                    )
                );

                const reference = candidates.find(function(element) {

                    const text = clean(
                        element.innerText ||
                        element.textContent
                    );

                    return labels.has(text) &&
                        element.getClientRects().length > 0;

                });

                if (reference && createEntry(reference)) {
                    return;
                }

                // Fallback: locate the user-settings sidebar.
                const containers = Array.from(
                    document.querySelectorAll(
                        'nav, [role="navigation"], [class*="sidebar"], [class*="Sidebar"]'
                    )
                );

                const settingsNav = containers.find(function(container) {

                    if (!container.getClientRects().length) {
                        return false;
                    }

                    const text = clean(
                        container.innerText ||
                        container.textContent
                    );

                    return text.includes('my account') ||
                        text.includes('appearance') ||
                        text.includes('voice & video') ||
                        text.includes('hesabım') ||
                        text.includes('görünüm');

                });

                if (settingsNav) {

                    const button = document.createElement('button');

                    button.id = ENTRY_ID;
                    button.type = 'button';
                    button.textContent = 'DisDroid';

                    button.setAttribute(
                        'aria-label',
                        'DisDroid Settings'
                    );

                    button.style.cssText =
                        'display:block;' +
                        'width:100%;' +
                        'padding:10px 12px;' +
                        'margin:6px 0;' +
                        'border:0;' +
                        'border-radius:6px;' +
                        'background:rgba(168,85,247,.18);' +
                        'color:#d8b4fe;' +
                        'font:inherit;' +
                        'text-align:left;' +
                        'cursor:pointer;';

                    button.addEventListener(
                        'click',
                        openDisDroid
                    );

                    settingsNav.appendChild(button);
                }
            }

            addEntry();

            // Observe Discord's dynamic interface.
            if (!window.__disdroidSettingsObserver) {

                window.__disdroidSettingsObserver =
                    new MutationObserver(function() {
                        addEntry();
                    });

                window.__disdroidSettingsObserver.observe(
                    document.documentElement,
                    {
                        childList: true,
                        subtree: true
                    }
                );
            }

            // Detect Discord SPA navigation.
            if (!window.__disdroidRouteHooked) {

                window.__disdroidRouteHooked = true;

                const notify = function() {
                    setTimeout(addEntry, 350);
                    setTimeout(addEntry, 1200);
                };

                const originalPushState = history.pushState;

                history.pushState = function() {

                    const result = originalPushState.apply(
                        this,
                        arguments
                    );

                    notify();

                    return result;
                };

                window.addEventListener('popstate', notify);
            }

            // Retry while Discord renders its Settings panel.
            let attempts = 0;

            const retry = setInterval(function() {

                addEntry();

                attempts++;

                if (
                    document.getElementById(ENTRY_ID) ||
                    attempts >= 20
                ) {
                    clearInterval(retry);
                }

            }, 500);

        })();
        """.trimIndent()

        webView.evaluateJavascript(javascript, null)
    }
}
