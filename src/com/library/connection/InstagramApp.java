package com.library.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.listeners.AuthStatusListener;
import com.example.listeners.OAuthDialogListener;
import com.example.ui.InstagramDialog;
import com.library.appconstant.AppConstants;
import com.library.entity.ImageDetails;

public class InstagramApp {

	private InstagramSession mSession;
	private InstagramDialog mDialog;
	private AuthStatusListener mListener;
	private ProgressDialog mProgress;
	private String mAuthUrl;
	private String mAccessToken;
	private String mClientId;
	private String mClientSecret;

	public static String mCallbackUrl;

	private static final String TAG = "InstagramAPI";

	public InstagramApp(Context context, String clientId, String clientSecret,
			String callbackUrl) {

		mClientId = clientId;
		mClientSecret = clientSecret;
		mSession = new InstagramSession(context);
		mAccessToken = mSession.getAccessToken();
		mCallbackUrl = callbackUrl;
		mAuthUrl = AppConstants.AUTH_URL + "?client_id=" + clientId + "&redirect_uri="
				+ mCallbackUrl + "&response_type=code";
		mDialog = new InstagramDialog(context, mAuthUrl, listener);
		mProgress = new ProgressDialog(context);
		mProgress.setCancelable(false);
	}


	private OAuthDialogListener listener = new OAuthDialogListener() {
		@Override
		public void onComplete(String code) {
			getAccessToken(code);
		}

		@Override
		public void onError(String error) {
			mListener.onFail("Authorization failed");
		}
	};
	private ArrayList<ImageDetails> mImageDetailsList;

	public void getAccessToken(final String code) {
		mProgress.setMessage("Getting access token ...");
		mProgress.show();

		new Thread() {
			@Override
			public void run() {
				Log.i(TAG, "Getting access token");
				int what = AppConstants.WHAT_FETCH_IMAGES;
				try {
					URL url = new URL(AppConstants.TOKEN_URL);
					Log.i(TAG, "Opening Token URL " + url.toString());
					HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
					urlConnection.setRequestMethod("POST");
					urlConnection.setDoInput(true);
					urlConnection.setDoOutput(true);
					OutputStreamWriter writer = new OutputStreamWriter(
							urlConnection.getOutputStream());
					writer.write("client_id=" + mClientId + "&client_secret="
							+ mClientSecret + "&grant_type=authorization_code"
							+ "&redirect_uri=" + mCallbackUrl + "&code=" + code);
					writer.flush();

					String response = streamToString(urlConnection.getInputStream());
					Log.i(TAG, "response " + response);
					JSONObject jsonObj = (JSONObject) new JSONTokener(response).nextValue();

					mAccessToken = jsonObj.getString("access_token");
					Log.i(TAG, "Got access token: " + mAccessToken);

					String id = jsonObj.getJSONObject("user").getString("id");
					String user = jsonObj.getJSONObject("user").getString("username");
					String name = jsonObj.getJSONObject("user").getString("full_name");					

					mSession.storeAccessToken(mAccessToken, id, user, name);

				} catch (Exception ex) {
					what = AppConstants.WHAT_ERROR;
					ex.printStackTrace();
				}

				mHandler.sendMessage(mHandler.obtainMessage(what, 1, 0));
			}
		}.start();
	}

	public void fetchImageURL() {
		mProgress.setMessage("Getiing Images ...");

		new Thread() {
			@Override
			public void run() {
				int what = AppConstants.WHAT_FINALIZE;
				try {

					String lImageJson = null;  
					HttpsURLConnection urlConnection = getConnection(AppConstants.IMAGE_URL + mAccessToken);
					if(urlConnection != null) {
						InputStream is = urlConnection.getInputStream();
						lImageJson = streamToString(is);
						is.close();
						parseJson(lImageJson);
					}
				} catch (Exception e1) {
					e1.printStackTrace();			
				}			

				mHandler.sendMessage(mHandler.obtainMessage(what, 2, 0));
			}
		}.start();	

	}


	private void parseJson(String json){
		mImageDetailsList = new ArrayList<ImageDetails>();
		try {
			JSONArray root = null; 
			JSONObject jsonObject = new JSONObject(json);

			if (!jsonObject.isNull("data")) {
				root = jsonObject.getJSONArray("data");
				for(int i=0;i<root.length();i++){ 
					JSONObject lJSONObjectImages = root.getJSONObject(i).getJSONObject("images");
					ImageDetails lImageDetails = new ImageDetails();
					lImageDetails.setmImageURILarge((String) lJSONObjectImages.getJSONObject("standard_resolution").get("url"));
					addURL(i%3,lJSONObjectImages,lImageDetails);
					mImageDetailsList.add(lImageDetails);

				}
			} 
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void addURL(int i, JSONObject lJSONObjectImages, ImageDetails lImageDetails) {
		switch (i) {
		case 0:
			try {
				lImageDetails.setmImageURI((String) lJSONObjectImages.getJSONObject("standard_resolution").get("url"));

			} catch (JSONException e) {
				e.printStackTrace();
			}
			break;

		case 1:
			try {
				lImageDetails.setmImageURI((String) lJSONObjectImages.getJSONObject("low_resolution").get("url"));

			} catch (JSONException e) {
				e.printStackTrace();
			}
			break;
		case 2:
			try {
				lImageDetails.setmImageURI((String) lJSONObjectImages.getJSONObject("thumbnail").get("url"));

			} catch (JSONException e) {
				e.printStackTrace();
			}
			break;

		default:
			break;
		}

	}

	private HttpsURLConnection getConnection(String pUrl){

		HttpsURLConnection urlConnection = null;
		if(pUrl != null ){
			try {
				URL url = new URL(pUrl);
				urlConnection = (HttpsURLConnection)url.openConnection();
				urlConnection.setRequestMethod("GET");
				urlConnection.setConnectTimeout(30000);
				urlConnection.setRequestProperty("Accept", "application/json");
				urlConnection.setUseCaches(false); 
				urlConnection.setRequestProperty("Connection","Keep-Alive"); 
				urlConnection.getHostnameVerifier(); 
				urlConnection.getSSLSocketFactory(); 
				urlConnection.connect();
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
		return urlConnection;
	}



	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == AppConstants.WHAT_ERROR) {
				mProgress.dismiss();
				if(msg.arg1 == 1) {
					mListener.onFail("Failed to get access token");
				}
				else if(msg.arg1 == 2) {
					mListener.onFail("Failed to get user information");
				}
			} 
			else if(msg.what == AppConstants.WHAT_FETCH_IMAGES) {
				fetchImageURL();
			}
			else {

				mProgress.dismiss();
				mListener.onSuccess(mImageDetailsList);
			}
		}
	};

	public boolean hasAccessToken() {
		return (mAccessToken == null) ? false : true;
	}

	public void setListener(AuthStatusListener listener) {
		mListener = listener;
	}

	public String getUserName() {
		return mSession.getUsername();
	}

	public String getId() {
		return mSession.getId();
	}

	public String getName() {
		return mSession.getName();
	}

	public void authorize() {
		mDialog.show();
	}

	private String streamToString(InputStream is) throws IOException {
		String str = "";

		if (is != null) {
			StringBuilder sb = new StringBuilder();
			String line;

			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is));

				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}

				reader.close();
			} finally {
				is.close();
			}

			str = sb.toString();
		}

		return str;
	}

	public void resetAccessToken() {
		if (mAccessToken != null) {
			mSession.resetAccessToken();
			mAccessToken = null;
		}
	}

}