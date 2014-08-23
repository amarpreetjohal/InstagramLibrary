package com.example.listeners;

public interface OAuthDialogListener {
	public void onComplete(String accessToken);
	public void onError(String error);
}
