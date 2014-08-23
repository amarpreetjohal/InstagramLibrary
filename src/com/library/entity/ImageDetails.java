package com.library.entity;

import java.io.Serializable;


public class ImageDetails implements Serializable{
	private static final long serialVersionUID = 1L;
	private String mImageURI;
	private String mImageURILarge;
	public String getmImageURI() {
		return mImageURI;
	}
	public void setmImageURI(String mImageURI) {
		this.mImageURI = mImageURI;
	}
	public String getmImageURILarge() {
		return mImageURILarge;
	}
	public void setmImageURILarge(String mImageURILarge) {
		this.mImageURILarge = mImageURILarge;
	}
}
