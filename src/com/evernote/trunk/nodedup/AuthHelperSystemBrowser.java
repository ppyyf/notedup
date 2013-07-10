package com.evernote.trunk.nodedup;


import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import javax.swing.JFrame;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Api;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import com.sun.net.httpserver.*;

import com.evernote.client.oauth.EvernoteAuthToken;

public class AuthHelperSystemBrowser extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String host;
	private String urlBase;
	private String authorizationUrl;
	
	public static String getKey() {
		return Constants.API_KEY;
	}

	public AuthHelperSystemBrowser(String serviceHost) {
		this.host = serviceHost;
		this.urlBase = "https://"+host; //$NON-NLS-1$
		this.authorizationUrl = urlBase + "/OAuth.action"; //$NON-NLS-1$
	}
	
	private String getAuthorizationUrl(String requestToken)
	{
		return String.format(this.authorizationUrl + "?oauth_token=%s", requestToken); //$NON-NLS-1$
	}

	public String getOAuthToken(){		
		
		int port = 9000;
		// find a unused TCP port > 9000 and start HTTP server listening on it.
		class MyHandler implements HttpHandler {
			public void handle(HttpExchange t) throws IOException {
				String ReqURI=t.getRequestURI().getRawQuery();
				String[] strings= ReqURI.split("[=&]"); //$NON-NLS-1$
				String verifier = null;
				for (int i = 0;i<strings.length;i+=2){
					if(strings[i].equals("oauth_verifier")){ //$NON-NLS-1$
						verifier = strings[i+1];
					}
				}
				String response = "<html><head><meta content=\"text/html\" charset=\"gbk\"/></head><body>"+Messages.getString("AuthHelperSystemBrowser.close_msg")+"</body></html>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				t.sendResponseHeaders(200, response.getBytes().length);
				OutputStream os = t.getResponseBody();
				os.write(response.getBytes());
				os.close();
				SyncVariables.setVerifier(verifier);
			}
		}
		HttpServer server = null;
		while(port < 65535){
			try {
				server = HttpServer.create(new InetSocketAddress(port),4);
				server.createContext("/__ND_callback", new MyHandler()); //$NON-NLS-1$
				server.setExecutor(null);
				server.start();
				break;
			} catch (IOException e) {
				port++;
			}
		}
		
		if (server == null){
			return null;
		}
		
		String callbackUrl = "http://localhost:"+ port + "/__ND_callback"; //$NON-NLS-1$ //$NON-NLS-2$
		
		Class<? extends Api> providerClass = org.scribe.builder.api.EvernoteApi.Sandbox.class;                                                                                          
		if (urlBase.equals("https://www.evernote.com")) { //$NON-NLS-1$
			providerClass = org.scribe.builder.api.EvernoteApi.class;                                                                                                                    
		} else if(urlBase.equals("https://app.yinxiang.com")){ //$NON-NLS-1$
			providerClass = YinxiangApi.class;
		}		

		OAuthService service = new ServiceBuilder()
		.provider(providerClass)
		.apiKey(Constants.API_KEY)
		.apiSecret(Constants.API_SECRET)
		.callback(callbackUrl)
		.build();

		Token token = service.getRequestToken();
		
		com.evernote.trunk.nodedup.SyncVariables.resetVerifier();
		// open system web browser with this URL.
		try {
			Desktop.getDesktop().browse(java.net.URI.create(this.getAuthorizationUrl(token.getToken())));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// wait up to one minute and get verifier (may or may not be set)
		Long beginTime=new java.util.Date().getTime();
		while (SyncVariables.getVerifier()!=null && SyncVariables.getVerifier().equals(SyncVariables.VERIFIER_NOT_SET)){
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if ((new java.util.Date().getTime() - beginTime)>60000l){
				break;
			}
		}
		server.stop(0);
		String verifier = com.evernote.trunk.nodedup.SyncVariables.getVerifier();
		
		if (verifier==null || verifier.equals("")){ //$NON-NLS-1$
			return null;
		} else if (verifier.equals(SyncVariables.VERIFIER_NOT_SET)){
			return null;
		} else {
			Verifier scribeVerifier = new Verifier(verifier);
			return new EvernoteAuthToken(service.getAccessToken(token, scribeVerifier)).getToken();
		}
	}
}
