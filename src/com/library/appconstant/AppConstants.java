package com.library.appconstant;

public class AppConstants {
	public static final String AUTH_URL = "https://api.instagram.com/oauth/authorize/";
	public static final String TOKEN_URL = "https://api.instagram.com/oauth/access_token";
	public static final String API_URL = "https://api.instagram.com/v1";
	public static final String IMAGE_URL = API_URL+"/tags/selfie/media/recent?access_token=";
	public static final int WHAT_FINALIZE = 0;
	public static final int WHAT_ERROR = 1;
	public static final int WHAT_FETCH_IMAGES = 2;
}
