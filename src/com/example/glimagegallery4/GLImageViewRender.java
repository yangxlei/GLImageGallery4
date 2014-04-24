package com.example.glimagegallery4;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;

public class GLImageViewRender implements Renderer {
	
	public static final float ALLOWABLE_ZOOM_OVER_SHOT = 1.0f / 0.9f;

	private int mCurrentPage = 0;
	private int mPageCount = 3 ;
	
	private GLImageProgram[] mPrograms;
	
	private int mCurrentWidth; 
	private int mCurrentHeight;

	private Matrix mTransformMatrix;
	private float[] mMatrixValues = new float[9];
	private GlImageView mImageView;
	
	private float mMinZoomScale = 1.0f; 
	private float mMaxZoomScale = 5.0f ; 
	
	public GLImageViewRender(GlImageView imageview) {
		mPrograms = new GLImageProgram[3];
		mPrograms[0] = new GLImageProgram();
		mPrograms[1] = new GLImageProgram();
		mPrograms[2] = new GLImageProgram();

		mTransformMatrix = new Matrix();
		
		mImageView = imageview;
	}
	
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		GLES20.glClearColor(0, 0, 0, 1);
		
		mPrograms[0].setup(GLImageProgram.PAGE_CURRENT);
		mPrograms[1].setup(GLImageProgram.PAGE_NEXT);
		mPrograms[2].setup(GLImageProgram.PAGE_PREVIOUS);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
		
		mCurrentWidth = width ; 
		mCurrentHeight = height;
		
		mPrograms[0].onViewChanged(width, height);
		mPrograms[1].onViewChanged(width, height);
		mPrograms[2].onViewChanged(width, height);
		
		
		Bitmap bitmap = BitmapFactory.decodeResource(mImageView.getResources(), R.drawable.test);
		mPrograms[0].setImage(bitmap);
		bitmap.recycle();
		bitmap = null;
		
		bitmap = BitmapFactory.decodeResource(mImageView.getResources(), R.drawable.login_btn_qq);
		mPrograms[1].setImage(bitmap);
		bitmap.recycle();
		bitmap = null;
		
		bitmap = BitmapFactory.decodeResource(mImageView.getResources(), R.drawable.abouticon);
		mPrograms[2].setImage(bitmap);
		bitmap.recycle();
		bitmap = null;
		
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		GLES20.glClearColor(0, 0, 0, 1);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		
		float scale = getAbsoluteScale();
		if (scale > 1.0f) {
			getCurrentPageProgram().render(mTransformMatrix);
		} else {
			mPrograms[0].render(mTransformMatrix);
			mPrograms[1].render(mTransformMatrix);
			mPrograms[2].render(mTransformMatrix);
		}
	}

	public void postTranslationByAmount(float amountX, float amountY)  {
		mTransformMatrix.getValues(mMatrixValues);
		
		mMatrixValues[Matrix.MTRANS_X] += amountX ; 
		mMatrixValues[Matrix.MTRANS_Y] += amountY;
		
		mTransformMatrix.setValues(mMatrixValues);
		
		constrainedTranslation();
	}
	
	public void onScrollPageCompleted(int mode) {
		if ((mode > 0 && mCurrentPage == mPageCount-1) || (mode < 0 && mCurrentPage == 0))
			return ;
		mTransformMatrix.getValues(mMatrixValues);
		if (mode > 0) {
			mCurrentPage = Math.min(++mCurrentPage, mPageCount-1); 
		} else if (mode < 0) {
			mCurrentPage = Math.max(--mCurrentPage, 0);
		}
		
		for (int i = 0; i < mPrograms.length; ++ i) {
			if (mode > 0) {
				mPrograms[i].scrollToNext();
			} else if(mode < 0) {
				mPrograms[i].scrollToPrevious();
			}
		}
		
		mTransformMatrix.reset();
		mTransformMatrix.getValues(mMatrixValues);
		mImageView.requestRender();
	}
	
	public void setAbsoluteScale(float scale) {
		mTransformMatrix.getValues(mMatrixValues);
		
		mMatrixValues[Matrix.MSCALE_X] = scale;
		mMatrixValues[Matrix.MSCALE_Y] = scale; 
		
		mTransformMatrix.setValues(mMatrixValues);
		
		constrainedTranslation();
	}
	
	public float getAbsoluteScale() {
		mTransformMatrix.getValues(mMatrixValues);
		return mMatrixValues[Matrix.MSCALE_X];
	}
	
	
	public void postScaleByAmount(float amount, float centerX, float centerY) {
		mTransformMatrix.postScale(amount, amount, centerX, centerY);
		mTransformMatrix.getValues(mMatrixValues);
		
		float scale = mMatrixValues[Matrix.MSCALE_X];
		scale = Math.max(mMinZoomScale * 1/ALLOWABLE_ZOOM_OVER_SHOT, Math.min(scale, mMaxZoomScale * ALLOWABLE_ZOOM_OVER_SHOT));
		
		mMatrixValues[Matrix.MSCALE_X] = scale ;
		mMatrixValues[Matrix.MSCALE_Y] = scale;
		
		mTransformMatrix.setValues(mMatrixValues);
		constrainedTranslation();
		
	}

	
	public void setAbsoluteTransform(float centerX, float centerY) {
		mTransformMatrix.getValues(mMatrixValues);
		
		mMatrixValues[Matrix.MTRANS_X] = centerX;
		mMatrixValues[Matrix.MTRANS_Y] = centerY;
		
		mTransformMatrix.setValues(mMatrixValues);
		constrainedTranslation();
	}
	
	public PointF getAbsoluteTransfrom() {
		mTransformMatrix.getValues(mMatrixValues);
		
		PointF point = new PointF();
		point.x = mMatrixValues[Matrix.MTRANS_X];
		point.y = mMatrixValues[Matrix.MTRANS_Y];
		
		return point;
	}
	
	public void constrainedTranslation() {
		
		mTransformMatrix.getValues(mMatrixValues);
		
		float transX = mMatrixValues[Matrix.MTRANS_X];
		float transY = mMatrixValues[Matrix.MTRANS_Y];
		
		PointF newPoint = getConstrainedTranslation(transX, transY, getAbsoluteScale());
		
		mMatrixValues[Matrix.MTRANS_X] = newPoint.x;
		mMatrixValues[Matrix.MTRANS_Y] = newPoint.y;
		
		mTransformMatrix.setValues(mMatrixValues);
	}
	
	public boolean hasNextPage() {
		return mCurrentPage < (mPageCount - 1);
	}
	
	public boolean hasPreviousPage() {
		return mCurrentPage > 0;
	}
	
	public GLImageProgram getCurrentPageProgram() {
		for (int i = 0; i < mPrograms.length; ++ i) {
			if (mPrograms[i].getPageMode() == GLImageProgram.PAGE_CURRENT) {
				return mPrograms[i];
			}
		}
		return null;
	}
	
	public PointF getConstrainedTranslation(float transX, float transY) {
		return getConstrainedTranslation(transX, transY, getAbsoluteScale());
	}
	
	public PointF getConstrainedTranslation(float transX, float transY, float scale) {
		PointF point = new PointF();
		mTransformMatrix.getValues(mMatrixValues);
		
		RectF imagebounds = getCurrentPageProgram().getImageBounds();
		
		float scaleX = Math.max(imagebounds.right * scale, 1.f);
		float scaleY = Math.max(scale * imagebounds.top, 1.f);
		
		// 已经有缩放下的平移
		if (transX < 0) {
			point.x = Math.max(transX, 1 - scaleX);
		} else {
			point.x = Math.min(transX, scaleX - 1);
		}

		if (transY < 0) {
			point.y = Math.max(transY, 1 - scaleY);
		} else {
			point.y = Math.min(transY, scaleY - 1);
		}
		
		
		//处理翻页的情况
		if (getAbsoluteScale() == 1.f) {
			if (transX < 0) { // 向右
				if (hasNextPage())
					point.x = Math.max(transX, -2.f);
				else
					point.x = Math.max(transX, 1 - scaleX);
			} else {
				if (hasPreviousPage())
					point.x = Math.min(transX, 2.f);
				else 
					point.x = Math.min(transX, scaleX - 1);
			}
		}
		
		return point;
	}
	
	public int getViewWidth() {
		return mCurrentWidth;
	}
	
	public int getViewHeight() {
		return mCurrentHeight;
	}
	
	public float getMinZoomScale() {
		return mMinZoomScale;
	}
	
	public float getMaxZoomScale() {
		return mMaxZoomScale;
	}
}
