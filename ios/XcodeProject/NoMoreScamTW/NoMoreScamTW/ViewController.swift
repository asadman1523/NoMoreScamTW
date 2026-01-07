//
//  ViewController.swift
//  NoMoreScamTW
//
//  Created by Jack on 2026/1/4.
//

import UIKit
import WebKit

class ViewController: UIViewController, WKNavigationDelegate, WKScriptMessageHandler {

    @IBOutlet var webView: WKWebView!

    override func viewDidLoad() {
        super.viewDidLoad()

        // Register the handler. Note: "controller" must match window.webkit.messageHandlers.controller
        self.webView.configuration.userContentController.add(self, name: "controller")

        self.webView.navigationDelegate = self
        self.webView.scrollView.isScrollEnabled = false

        if let url = Bundle.main.url(forResource: "Main", withExtension: "html") {
            self.webView.loadFileURL(url, allowingReadAccessTo: Bundle.main.resourceURL!)
        }
    }

    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        if message.name == "controller", let body = message.body as? String, body == "openSettings" {
            if let url = URL(string: UIApplication.openSettingsURLString) {
                UIApplication.shared.open(url, options: [:], completionHandler: nil)
            }
        }
    }

    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        if let url = navigationAction.request.url, (url.scheme == "http" || url.scheme == "https") {
            UIApplication.shared.open(url)
            decisionHandler(.cancel)
            return
        }
        decisionHandler(.allow)
    }

}
