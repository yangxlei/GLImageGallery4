package com.example.glimagegallery4;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLUtils;

public class GLImageProgram {
	public static final int PAGE_CURRENT = 0;
	public static final int PAGE_PREVIOUS = -2 ; 
	public static final int PAGE_NEXT = 2 ;
	
	private String mVertexShader = "" +
			"attribute vec4 a_position;" + 
		    "attribute vec2 a_texCoord;" + 
		    "varying highp vec2 v_texCoord;" + 
		    "void main(void) {" +
		    "    v_texCoord = a_texCoord;" +
		    "    gl_Position = a_position;" +
		    "}";
			
	private String mFragmentShader = "" +
			"		precision mediump float;" + 
			"       uniform lowp sampler2D u_sampler;" +
			"       varying highp vec2 v_texCoord;" +   
			"       void main(void) { " +
			"           gl_FragColor = texture2D(u_sampler, v_texCoord);" +
			"       }";
	

	int mTextureId;
	int mProgram;
	int mPageMode = PAGE_CURRENT;
	
	private RectF mImageBoundsRect;
	private int mCurrentBitmapWidth;
	private int mCurrentBitmapHeight;
	
	private int mViewWidth;
	private int mViewHeight;

	private float[] mVertices = {
			1.0f, -1.0f, 0.0f, 1.0f, 1.0f,
			-1.0f, -1.0f, 0.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 
			-1.0f, 1.0f, 0.0f, 0.0f, 0.0f
	};
	
	private FloatBuffer vertexBuffer;
	
	private int mPositionHandle;
	private int mTexCoordHandle;
	private int mSamplerHandle;
	
	public void setPageMode(int pagemode) {
		mPageMode = pagemode;
	}
	
	public int getPageMode() {
		return mPageMode;
	}
	
	public void setup(int pagemode) {
		vertexBuffer = ByteBuffer.allocateDirect(mVertices.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		vertexBuffer.put(mVertices);
		vertexBuffer.position(0);
		
		mPageMode = pagemode;
		mImageBoundsRect = new RectF();
		
		int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, mVertexShader);
		int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, mFragmentShader);
		
		mProgram = GLES20.glCreateProgram();
		GLES20.glAttachShader(mProgram, vertexShader);
		GLES20.glAttachShader(mProgram, fragmentShader);
		GLES20.glLinkProgram(mProgram);
		
		mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_position");
		mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "a_texCoord");
		mSamplerHandle = GLES20.glGetUniformLocation(mProgram, "u_sampler");
	}
	
	public void onViewChanged(int width, int height) {
		mViewWidth = width;
		mViewHeight = height;
	}
	
	public void scrollToNext() {
		if (mPageMode == PAGE_CURRENT) {
			mPageMode = PAGE_PREVIOUS;
		} else if (mPageMode == PAGE_PREVIOUS) {
			mPageMode = PAGE_NEXT;
		} else {
			mPageMode = PAGE_CURRENT;
		}
	}
	
	public void scrollToPrevious() {
		if (mPageMode == PAGE_CURRENT) {
			mPageMode = PAGE_NEXT;
		} else if (mPageMode == PAGE_PREVIOUS) {
			mPageMode = PAGE_CURRENT;
		} else {
			mPageMode = PAGE_PREVIOUS;
		}
	}
	
	public void render(Matrix transformMatrx) {
		if(mTextureId == 0 || mImageBoundsRect == null) { 
			System.out.println("err: This's not initialized. (mTextureId == 0 || mImageBoundsRect == null)");
			return;
		}
		
		GLES20.glUseProgram(mProgram);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
		
		GLES20.glUniform1f(mSamplerHandle, 0f);
		
		float points[] = {
				mImageBoundsRect.right, mImageBoundsRect.bottom,
				mImageBoundsRect.left, mImageBoundsRect.bottom,
				mImageBoundsRect.right, mImageBoundsRect.top,
				mImageBoundsRect.left, mImageBoundsRect.top
		};
		
		transformMatrx.mapPoints(points);
		
		mVertices[0] = points[0] + mPageMode;
		mVertices[1] = points[1];

		mVertices[5] = points[2] + mPageMode;
		mVertices[6] = points[3];

		mVertices[10] = points[4] + mPageMode;
		mVertices[11] = points[5];

		mVertices[15] = points[6] + mPageMode;
		mVertices[16] = points[7];
		
		vertexBuffer.position(0);
		vertexBuffer.put(mVertices);
		
		vertexBuffer.position(0);
		GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 20, vertexBuffer);
		vertexBuffer.position(3);
		GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 20, vertexBuffer);
		
		GLES20.glEnableVertexAttribArray(mPositionHandle);
		GLES20.glEnableVertexAttribArray(mTexCoordHandle);
		
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		
		GLES20.glDisableVertexAttribArray(mPositionHandle);
		GLES20.glDisableVertexAttribArray(mTexCoordHandle);
		
	}
	
	public void setImage(Bitmap bitmap) {
		mCurrentBitmapWidth = bitmap.getWidth();
		mCurrentBitmapHeight = bitmap.getHeight();
		
		loadTexture(bitmap);
		
		setupImageBounds();
	}
	
	public RectF getImageBounds() {
		return mImageBoundsRect;
	}
	
	private void setupImageBounds() {
		if (mCurrentBitmapWidth == 0 || mCurrentBitmapHeight == 0 || mViewWidth == 0 || mViewHeight == 0) 
			return;
		
		float glHalfWidth = 1.0f;
		float glHalfHeight = 1.0f;
		
		float imageAspect = mCurrentBitmapWidth / (float)mCurrentBitmapHeight;
		float renderBufferAspect = mViewWidth / (float) mViewHeight;
		
		if (imageAspect > renderBufferAspect) {
			glHalfWidth = Math.min(1.f, mCurrentBitmapWidth / (float) mViewWidth);
			glHalfHeight = glHalfWidth * renderBufferAspect / imageAspect ;
		} else {
			glHalfHeight = Math.min(1.f, mCurrentBitmapHeight / (float) mViewHeight) ;
			glHalfWidth = imageAspect / renderBufferAspect;
		}
		
		mImageBoundsRect.set(-glHalfWidth, glHalfHeight, glHalfWidth, -glHalfHeight);
	}
	
	private void loadTexture(Bitmap bitmap) {
		int textures[] = new int[1];
		GLES20.glGenTextures(1, textures, 0);
		
		mTextureId = textures[0];
		
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
	}
	
	private int loadShader(int type, String shaderCode) {
		int shader = GLES20.glCreateShader(type);
		
		GLES20.glShaderSource(shader, shaderCode);
		GLES20.glCompileShader(shader);
		return shader;
	}
	
}
