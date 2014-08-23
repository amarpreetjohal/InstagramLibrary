package com.example.listeners;

import java.util.ArrayList;

import com.library.entity.ImageDetails;

public interface AuthStatusListener {
	
	public void onSuccess(ArrayList<ImageDetails> mImageDetailsList);
	public void onFail(String error);
}
