package com.example.glimagegallery4;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class GlImageView extends GLSurfaceView {
	
	public static final int SCROLL_TO_NEXT = 1;
	public static final int SCROLL_TO_NORMAL = 0 ; 
	public static final int SCROLL_TO_PRE = -1;

	private GLImageViewRender mRender;

	private GestureDetector mGestureDetector;
	private GestureListener mGestureListener ;
	private ScaleGestureDetector mScaleDetector;
	private ScaleListener mScaleListener;
	
	private Cubic mEasing = new Cubic();
	
	private PointF mLastValidCenter ;

	public GlImageView(Context context) {
		super(context);
		setup();
	}

	public GlImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setup();
	}
	
	private void setup() {
		setEGLContextClientVersion( 2 );
		
		mGestureListener = new GestureListener();
		mGestureDetector = new GestureDetector(getContext(), mGestureListener);
		mScaleListener = new ScaleListener();
		mScaleDetector = new ScaleGestureDetector(getContext(), mScaleListener);
		
		mLastValidCenter = new PointF();
		
		mRender = new GLImageViewRender(this);
		setRenderer(mRender);
		setRenderMode(RENDERMODE_WHEN_DIRTY);
		requestRender();
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mScaleDetector.onTouchEvent(event);
		if (!mScaleDetector.isInProgress())
			mGestureDetector.onTouchEvent(event);
		
		int action = event.getAction();
		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_UP:
			if (mRender.getAbsoluteScale() == 1.0f) {
				// 滑到指定位置
				scrollTo();
			} else {
				if (mRender.getAbsoluteScale() < mRender.getMinZoomScale()) {
					zoomTo(mRender.getMinZoomScale(), 0, 0, 50);
				} else if (mRender.getAbsoluteScale() > mRender.getMaxZoomScale()) {
					zoomTo(mRender.getMaxZoomScale(), mLastValidCenter.x, mLastValidCenter.y, 50);
				}
			}
			break;
		default:
			break;
		}
		return true;
	}
	
	public boolean onScale(ScaleGestureDetector detector) {
		float scale = detector.getScaleFactor();
		
		float centerX = 2 * detector.getFocusX() / (float) mRender.getViewWidth() -1;
		float centerY = 1 - 2 *detector.getFocusY()/(float)mRender.getViewHeight();
		
		if(mRender.getAbsoluteScale() < mRender.getMaxZoomScale() * GLImageViewRender.ALLOWABLE_ZOOM_OVER_SHOT) {
			mRender.postScaleByAmount(scale, centerX, centerY);
			if(mRender.getAbsoluteScale() <= mRender.getMaxZoomScale()) {
				mLastValidCenter = mRender.getAbsoluteTransfrom();
			}
			requestRender();
		}
		return true;
	}
	
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		if (e1 == null || e2 == null) return false;
		if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1) return false;
		if (mScaleDetector.isInProgress()) return false;

		float dx = -2 * distanceX / (float) mRender.getViewWidth();
		float dy = 2 * distanceY / (float) mRender.getViewHeight();
		
		mRender.postTranslationByAmount(dx, dy);
		
		requestRender();
		return true;
	}
	
	public boolean onDoubleTap(MotionEvent e) {
		float centerX = e.getX();
		float centerY = e.getY();
		
		float currentScale = mRender.getAbsoluteScale();
		PointF currentTranslation = mRender.getAbsoluteTransfrom();
		
		float minScale = mRender.getMinZoomScale();
		float maxScale = mRender.getMaxZoomScale();
		float halfScale = maxScale / 2.0f ;
		
		float nextScale = 1.0f; 
		if (currentScale < halfScale) {
			nextScale = halfScale;
		} else if(currentScale < maxScale) {
			nextScale = maxScale;
		} else {
			nextScale = minScale;
		}
		
		float scaleAdjust = nextScale / currentScale;
		
		float dx = (1 - 2 * centerX/(float)mRender.getViewWidth());
		float dy =  (2 * centerY/(float)mRender.getViewHeight() - 1);
		
		float newProposedCenterX = (dx + currentTranslation.x) * scaleAdjust;
		float newProposedCenterY = (dy + currentTranslation.y) * scaleAdjust;
		
		PointF newCenter = mRender.getConstrainedTranslation(newProposedCenterX, newProposedCenterY, nextScale);
		
		zoomTo(nextScale, newCenter.x, newCenter.y, 300);
		
		mLastValidCenter = newCenter;
		return false;
	}
	
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if (e1 == null || e2 == null) return false;
		if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1) return false;
		if (mScaleDetector.isInProgress()) return false;
		
		float originalScale = mRender.getAbsoluteScale();
		
		float distanceX = e2.getX() - e1.getX();
		float distanceY = e2.getY() - e1.getY();
		
		if ((Math.abs(velocityX) > 800 || Math.abs(velocityY) > 800) && originalScale != 1.0f) {
			PointF originalTranslation = mRender.getAbsoluteTransfrom();
			final float originalX = originalTranslation.x; 
			final float origianlY = originalTranslation.y;
			
			float rawNewX = originalX + 2  * distanceX/(float)mRender.getViewWidth();
			float rawNewY = origianlY - 2 * distanceY /(float)mRender.getViewHeight();
			
			PointF newTranslation = mRender.getConstrainedTranslation(rawNewX, rawNewY);
			
			mLastValidCenter = newTranslation;
			
			final float newX = newTranslation.x;
			final float newY = newTranslation.y;
			
			final long startTime = System.currentTimeMillis();
			final long duratiomMs = 300 ;
			
			this.post(new Runnable() {
				
				@Override
				public void run() {
					long now = System.currentTimeMillis();
					double currentMs = Math.min(now-startTime, duratiomMs);
					float dx = (float)mEasing.easeOut(currentMs, 0, newX-originalX, duratiomMs);
					float dy = (float)mEasing.easeOut(currentMs, 0, newY-origianlY, duratiomMs);
					
					mRender.setAbsoluteTransform(originalX+dx, origianlY+dy);
					requestRender();
					
					if (currentMs < duratiomMs) {
						postDelayed(this, 10);
					} 
				}
			});
			
			return true;
		}
		
		return false;
	}
	
	public void scrollTo() {
		PointF center = mRender.getAbsoluteTransfrom();
		final float originalCenterX = center.x;
		
		int temp_mode = SCROLL_TO_NORMAL;
		
		if (originalCenterX > -0.4f && originalCenterX < 0.4f) {
			temp_mode = SCROLL_TO_NORMAL;
		} else if (originalCenterX >= 0.5f) {
			temp_mode = SCROLL_TO_PRE;
		} else {
			temp_mode = SCROLL_TO_NEXT;
		}
		scrollTo(temp_mode);
	}
	
	public void scrollTo(final int mode) {
		PointF center = mRender.getAbsoluteTransfrom();
		final float originalCenterX = center.x;
		final float originalCenterY = center.y;
		
		
		float tempX  = 0 ;
		if (mode == SCROLL_TO_NORMAL) {
			tempX = 0.0f;
		} else if (mode == SCROLL_TO_PRE) {
			tempX = 2.0f;
		} else {
			tempX = -2.f;
		}
		
		final float newCenterX = tempX;
		
		final long startTime = System.currentTimeMillis();
		final long duratiomMs = 200;
		this.post(new Runnable() {
			
			@Override
			public void run() {
				long now = System.currentTimeMillis();
				double currentMs = Math.min(now-startTime, duratiomMs);
				
				float dx = (float)mEasing.easeOut(currentMs, 0, newCenterX - originalCenterX, duratiomMs);
				
				mRender.setAbsoluteTransform(originalCenterX+dx, originalCenterY);
				requestRender();
				
				if (currentMs < duratiomMs) {
					postDelayed(this, 10);
				} else {
					mRender.onScrollPageCompleted(mode);
				}
				
			}
		});
	}
	
	public void zoomTo(float scale, float centerX, float centerY, long duration) {
		final float originalScale = mRender.getAbsoluteScale();
		PointF center = mRender.getAbsoluteTransfrom();
		
		final float originalCenterX = center.x;
		final float originalCenterY = center.y;
		
		final float newScale = scale; 
		final float newCenterX = centerX;
		final float newCenterY = centerY;
		
		final long startTime = System.currentTimeMillis();
		final long durationMs = duration;
		
		this.post(new Runnable() {
			
			@Override
			public void run() {
				long now = System.currentTimeMillis();
				double currentMs = Math.min(now - startTime, durationMs);
				float dScale = (float)mEasing.easeOut(currentMs, 0, newScale-originalScale, durationMs);
				float dx = (float)mEasing.easeOut(currentMs, 0, newCenterX-originalCenterX, durationMs);
				float dy = (float)mEasing.easeOut(currentMs, 0, newCenterY - originalCenterY, durationMs);
				
				mRender.setAbsoluteScale(originalScale + dScale);
				mRender.setAbsoluteTransform(originalCenterY+dx, originalCenterY+dy);
				requestRender();
				if(currentMs < durationMs) {
					postDelayed(this, 10);
				} else { 
					if (mRender.getAbsoluteScale() == 1.f) {
						// 如果是缩放到了初始位置，重置一下矩阵
						mRender.onScrollPageCompleted(SCROLL_TO_NORMAL);
					}
				}
			}
		});
	}
	
	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener{
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			return GlImageView.this.onScale(detector);
		}
	}
	
	
	private class GestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			return GlImageView.this.onDoubleTap(e);
		}
		
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			return GlImageView.this.onScroll(e1, e2, distanceX, distanceY);
		}
		
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			return GlImageView.this.onFling(e1, e2, velocityX, velocityY);
		}
	}

}
