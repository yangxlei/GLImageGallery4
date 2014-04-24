package com.example.glimagegallery4;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.Window;

public class MainActivity extends Activity {

	GlImageView mImageView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_main);
		mImageView = new GlImageView(this);
		setContentView(mImageView);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mImageView.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mImageView.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
