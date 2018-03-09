package info.androidhive.speechtotext;

import android.app.Dialog;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class myWebClient extends WebViewClient
{
	Dialog mydia;
	myWebClient(Dialog dia){
		mydia = dia;
	}
	@Override
	public void onPageFinished(WebView view, String url) {
		super.onPageFinished(view, url);
		mydia.show();
	}
}
