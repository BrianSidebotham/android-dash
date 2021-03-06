package com.valvers.dash;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public final class Dash extends View {

	/* private static final String TAG = Dash.class.getSimpleName(); */
    
	private Handler handler;
	private Handler mHandler = new Handler();
	private TextView mTitle;
	
	private RectF rimRect;
	private Paint rimPaint;
	private Paint rimCirclePaint;
	
	private RectF faceRect;
	private Bitmap faceTexture;
	private Paint facePaint;
	private Paint rimShadowPaint;
	
	private Paint scalePaint;
	private RectF scaleRect;
	private Paint hardLimitPaint;
	private Paint softLimitPaint;
	
	private Paint titlePaint;	
	private Path titlePath;

	private Paint logoPaint;
	private Bitmap logo;
	private Matrix logoMatrix;
	private float logoScale;
	
	private Paint handPaint;
	private Paint handOutlinePaint;
	
	private Path handPath;
	private Paint handScrewPaint;
	
	private Paint backgroundPaint; 
	
	 // holds the cached static part
	private Bitmap background;
	
	// scale configuration
	private static final int totalNicks = 32;
	private static final float degreesPerNick = 180.0f / totalNicks;	
	private static final int centerDegree = 5000; // the one in the top centre (12 o'clock)
	private static final int minDegrees = 0;
	private static final int maxDegrees = 8000;
	
	// hand dynamics -- all are angular expressed in F degrees
	private boolean handInitialized = false;
	private float handPosition = 500;
	private float handTarget = 1000;
	private float handVelocity = 0.0f;
	private float handAcceleration = 0.0f;
	private long lastHandMoveTime = -1L;
	private Paint mphPaint;
	private String mphString;
	private int mph;
	
	private Context myApp;
	
	public Dash(Context context) {
		super(context);	    
		init();
	}

	public Dash(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public Dash(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
	}
	
	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		Bundle bundle = (Bundle) state;
		Parcelable superState = bundle.getParcelable("superState");
		super.onRestoreInstanceState(superState);
		
		handInitialized = bundle.getBoolean("handInitialized");
		handPosition = bundle.getFloat("handPosition");
		handTarget = bundle.getFloat("handTarget");
		handVelocity = bundle.getFloat("handVelocity");
		handAcceleration = bundle.getFloat("handAcceleration");
		lastHandMoveTime = bundle.getLong("lastHandMoveTime");
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		
		Bundle state = new Bundle();
		state.putParcelable("superState", superState);
		state.putBoolean("handInitialized", handInitialized);
		state.putFloat("handPosition", handPosition);
		state.putFloat("handTarget", handTarget);
		state.putFloat("handVelocity", handVelocity);
		state.putFloat("handAcceleration", handAcceleration);
		state.putLong("lastHandMoveTime", lastHandMoveTime);
		return state;
	}

	private void init() {
		setPadding(3, 3, 3, 3);
		handler = new Handler();
		myApp = (DashApplication)getContext().getApplicationContext();
		
		initDrawingTools();

		if (mStartTime == 0L) {
            mStartTime = System.currentTimeMillis();
            mHandler.removeCallbacks(mUpdateTimeTask);
            mHandler.postDelayed(mUpdateTimeTask, 100);
       }
	}

	private String getTitle() {
		return "RPM x1000";
	}

	private void initDrawingTools() {
		rimRect = new RectF(0.01f, 0.01f, 0.99f, 0.99f);

		// the linear gradient is a bit skewed for realism
		rimPaint = new Paint();
		rimPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		rimPaint.setShader(new LinearGradient(0.40f, 0.0f, 0.60f, 1.0f, 
										   Color.rgb(0xa0, 0xa0, 0xff),
										   Color.rgb(0x30, 0x30, 0x20),
										   Shader.TileMode.CLAMP));		

		rimCirclePaint = new Paint();
		rimCirclePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		rimCirclePaint.setStyle(Paint.Style.STROKE);
		rimCirclePaint.setColor(Color.argb(0x40, 0xa0, 0xa0, 0xa0));
		rimCirclePaint.setStrokeWidth(0.020f);
		rimCirclePaint.setShader(new LinearGradient(0.10f, 0.20f, 0.60f, 1.0f, 
				   Color.rgb(0xc0, 0xc0, 0xc0),
				   Color.rgb(0x10, 0x10, 0x10),
				   Shader.TileMode.CLAMP));
		
		float rimSize = 0.02f;
		faceRect = new RectF();
		faceRect.set(rimRect.left + rimSize, rimRect.top + rimSize, 
			     rimRect.right - rimSize, rimRect.bottom - rimSize);		

		faceTexture = BitmapFactory.decodeResource(getContext().getResources(), 
				   R.drawable.plastic);
		BitmapShader paperShader = new BitmapShader(faceTexture, 
												    Shader.TileMode.MIRROR, 
												    Shader.TileMode.MIRROR);
		
		hardLimitPaint = new Paint();
		hardLimitPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		hardLimitPaint.setStyle(Paint.Style.STROKE);
		hardLimitPaint.setColor(Color.argb(0xE0, 0xFF, 0x00, 0x00));
		hardLimitPaint.setStrokeWidth(0.1f);

		softLimitPaint = new Paint();
		softLimitPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		softLimitPaint.setStyle(Paint.Style.STROKE);
		softLimitPaint.setColor(Color.argb(0xE0, 0xFF, 0xA0, 0x00));
		softLimitPaint.setStrokeWidth(0.05f);
		
		Matrix paperMatrix = new Matrix();
		facePaint = new Paint();
		facePaint.setFilterBitmap(true);
		paperMatrix.setScale(1.0f / faceTexture.getWidth(), 
							 1.0f / faceTexture.getHeight());
		paperShader.setLocalMatrix(paperMatrix);
		facePaint.setStyle(Paint.Style.FILL);
		facePaint.setAntiAlias(true);

		rimShadowPaint = new Paint();
		rimShadowPaint.setShader(new RadialGradient(0.5f, 0.5f, faceRect.width() / 2.0f, 
				   new int[] { 0x00000000, 0x00000500, 0x50000500 },
				   new float[] { 0.96f, 0.96f, 0.99f },
				   Shader.TileMode.MIRROR));
		rimShadowPaint.setStyle(Paint.Style.FILL);

		scalePaint = new Paint();
		scalePaint.setStyle(Paint.Style.STROKE);
		scalePaint.setColor(0xF0F0F0FF);
		scalePaint.setStrokeWidth(0.005f);
		scalePaint.setAntiAlias(true);
		
		scalePaint.setTextSize(0.06f);
		scalePaint.setTypeface(Typeface.SANS_SERIF);
		scalePaint.setTextScaleX(0.8f);
		scalePaint.setTextAlign(Paint.Align.CENTER);
		
		
		mphPaint = new Paint();
		mphPaint.setTextSize(0.1f);
		mphPaint.setColor(0xF0F0F0FF);
		mphPaint.setStrokeWidth(0.005f);
		mphPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		mphPaint.setTypeface(Typeface.SANS_SERIF);
		mphPaint.setTextScaleX(0.8f);
		mphPaint.setTextAlign(Paint.Align.RIGHT);
		mphPaint.setAntiAlias(true);
		
		
		float scalePosition = 0.10f;
		scaleRect = new RectF();
		scaleRect.set(faceRect.left + scalePosition, faceRect.top + scalePosition,
					  faceRect.right - scalePosition, faceRect.bottom - scalePosition);

		titlePaint = new Paint();
		titlePaint.setColor(0xF0E0E0EF);
		titlePaint.setAntiAlias(true);
		titlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
		titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
		titlePaint.setTextAlign(Paint.Align.CENTER);
		titlePaint.setTextSize(0.08f);
		titlePaint.setTextScaleX(0.8f);

		titlePath = new Path();
		titlePath.addArc(new RectF(0.22f, 0.22f, 0.78f, 0.78f), 180.0f, 90.0f);

		logoPaint = new Paint();
		logoPaint.setFilterBitmap(true);
		logo = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.logo);
		logoMatrix = new Matrix();
		logoScale = (1.0f / logo.getWidth()) * 0.3f;;
		logoMatrix.setScale(logoScale, logoScale);

		handPaint = new Paint();
		handPaint.setAntiAlias(true);
		handPaint.setColor(0xfff0f0f0);		
		handPaint.setShadowLayer(0.01f, -0.005f, -0.005f, 0x7f000000);
		handPaint.setStyle(Paint.Style.FILL);	
		
		handOutlinePaint = new Paint();
		handOutlinePaint.setAntiAlias(true);
		handOutlinePaint.setColor(0xfff0f0f0);		
		//handOutlinePaint.setShadowLayer(0.01f, -0.005f, -0.005f, 0x7f000000);
		handOutlinePaint.setStyle(Paint.Style.STROKE);
		
		handPath = new Path();
		handPath.moveTo(0.5f, 0.5f + 0.2f);
		handPath.lineTo(0.5f - 0.010f, 0.5f + 0.2f - 0.007f);
		handPath.lineTo(0.5f - 0.002f, 0.5f - 0.32f);
		handPath.lineTo(0.5f + 0.002f, 0.5f - 0.32f);
		handPath.lineTo(0.5f + 0.010f, 0.5f + 0.2f - 0.007f);
		handPath.lineTo(0.5f, 0.5f + 0.2f);
		handPath.addCircle(0.5f, 0.5f, 0.025f, Path.Direction.CW);
		
		handScrewPaint = new Paint();
		handScrewPaint.setAntiAlias(true);
		handScrewPaint.setColor(0xffaaaaaa);
		handScrewPaint.setStyle(Paint.Style.FILL);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		
		int chosenWidth = chooseDimension(widthMode, widthSize);
		int chosenHeight = chooseDimension(heightMode, heightSize);
		
		int chosenDimension = Math.min(chosenWidth, chosenHeight);
		
		setMeasuredDimension(chosenDimension, chosenDimension);
	}
	
	private int chooseDimension(int mode, int size) {
		if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
			return size;
		} else { // (mode == MeasureSpec.UNSPECIFIED)
			return getPreferredSize();
		} 
	}
	
	// in case there is no size specified
	private int getPreferredSize() {
		return 300;
	}

	private void drawRim(Canvas canvas) {
		// first, draw the metallic body
		canvas.drawOval(rimRect, rimPaint);
		// now the outer rim circle
		canvas.drawOval(rimRect, rimCirclePaint);
	}
	
	private void drawFace(Canvas canvas) {		
		canvas.drawOval(faceRect, facePaint);
		// draw the inner rim circle
		canvas.drawOval(faceRect, rimCirclePaint);
		// draw the rim shadow inside the face
		canvas.drawOval(faceRect, rimShadowPaint);
	}

	private void drawScale(Canvas canvas) {
		canvas.drawArc(scaleRect, 125, 290, false, scalePaint);
		//canvas.drawOval(scaleRect, scalePaint);
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		
		// Rotate the canvas to start low at 45deg and end at 135deg
		canvas.rotate(225.0f, 0.5f, 0.5f);
		
		for (int i = 0; i < (totalNicks + 1); ++i) {
			float y1 = scaleRect.top;
			float y2 = y1 - 0.020f;
			
			if (i == 0)
			{
				canvas.drawLine(0.5f, y1 + 0.025f, 0.5f, y2 - 0.01f, scalePaint);
			}
			else
			{
				if (i % 4 == 0)
				{
					canvas.drawLine(0.5f, y1 + 0.025f, 0.5f, y2 - 0.01f, scalePaint);
				}
				else
				{
					canvas.drawLine(0.5f, y1, 0.5f, y2, scalePaint);
				}
			}
			
			if (i % 4 == 0) {
				int value = nickToDegree(i);
				
				if (value >= minDegrees && value <= maxDegrees) {
					String valueString = Integer.toString(value/1000);
					canvas.drawText(valueString, 0.5f, y2 - 0.02f, scalePaint);
				}
			}
			
			canvas.rotate(degreesPerNick, 0.5f, 0.5f);
		}
		
		canvas.restore();
		
		canvas.drawArc(scaleRect, 280.0f, 12.0f, false, softLimitPaint);
		canvas.drawArc(scaleRect, 292.0f, 15.0f, false, hardLimitPaint);
	}
	
	private int nickToDegree(int nick) {
		int value = ((maxDegrees - minDegrees) / totalNicks) * nick;
		return value;
	}
	
	private float degreeToAngle(float degree) {
		float angle = degree * (180.0f / (maxDegrees - minDegrees));
		return angle + 225;
		//return (degree - centerDegree) / 2.0f * degreesPerNick;
	}
	
	private void drawTitle(Canvas canvas) {
		String title = getTitle();
		canvas.drawTextOnPath(title, titlePath, 0.0f,0.0f, titlePaint);				
	}
	
	private void drawLogo(Canvas canvas) {
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		canvas.translate(0.5f - logo.getWidth() * logoScale / 2.0f, 
						 0.5f - logo.getHeight() * logoScale / 2.0f);

		int color = 0x00000000;
		float position = getRelativeTemperaturePosition();
		if (position < 0) {
			color |= (int) ((0xf0) * -position); // blue
		} else {
			color |= ((int) ((0xf0) * position)) << 16; // red			
		}
		
		LightingColorFilter logoFilter = new LightingColorFilter(0xff338822, color);
		logoPaint.setColorFilter(logoFilter);
		
		canvas.drawBitmap(logo, logoMatrix, logoPaint);
		canvas.restore();		
	}

	private void drawHand(Canvas canvas) {
		if (handInitialized) {
			float handAngle = degreeToAngle(handPosition);
			canvas.save(Canvas.MATRIX_SAVE_FLAG);
			canvas.rotate(handAngle, 0.5f, 0.5f);
			canvas.drawPath(handPath, handPaint);
			canvas.drawPath(handPath, handOutlinePaint);
			canvas.restore();
			
			canvas.drawCircle(0.5f, 0.5f, 0.01f, handScrewPaint);
		}
	}

	private void drawBackground(Canvas canvas) {
		
		if (background != null) {
			canvas.drawBitmap(background, 0, 0, backgroundPaint);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		drawBackground(canvas);

		float scale = (float) getWidth();		
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		canvas.scale(scale, scale);

		drawLogo(canvas);
		drawHand(canvas);
		
		mphString = Integer.toString(mph) + " MPH";
		canvas.drawText(mphString, 0.65f, 0.90f, mphPaint);
		
		canvas.restore();
		moveHand();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		regenerateBackground();
	}
	
	private void regenerateBackground() {
		// free the old bitmap
		if (background != null) {
			background.recycle();
		}
		
		background = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
		Canvas backgroundCanvas = new Canvas(background);
		float scale = (float) getWidth();		
		backgroundCanvas.scale(scale, scale);
		
		drawRim(backgroundCanvas);
		drawFace(backgroundCanvas);
		drawScale(backgroundCanvas);
		drawTitle(backgroundCanvas);		
	}

	private boolean handNeedsToMove() {
		return Math.abs(handPosition - handTarget) > 0.01f;
	}
	
	private void moveHand() {
		if (! handNeedsToMove()) {
			return;
		}
		
		handPosition = handTarget;
		invalidate();
		
		/*
		if (lastHandMoveTime != -1L) {
			long currentTime = System.currentTimeMillis();
			float delta = (currentTime - lastHandMoveTime) / 100.0f;

			float direction = Math.signum(handVelocity);
			if (Math.abs(handVelocity) < 90.0f) {
				handAcceleration = 5.0f * (handTarget - handPosition);
			} else {
				handAcceleration = 0.0f;
			}
			handPosition += handVelocity * delta;
			handVelocity += handAcceleration * delta;
			if ((handTarget - handPosition) * direction < 0.01f * direction) {
				handPosition = handTarget;
				handVelocity = 0.0f;
				handAcceleration = 0.0f;
				lastHandMoveTime = -1L;
			} else {
				lastHandMoveTime = System.currentTimeMillis();				
			}
			invalidate();
		} else {
			lastHandMoveTime = System.currentTimeMillis();
			moveHand();
		}
		*/
	}
	
	private float getRelativeTemperaturePosition() {
		if (handPosition < centerDegree) {
			return - (centerDegree - handPosition) / (float) (centerDegree - minDegrees);
		} else {
			return (handPosition - centerDegree) / (float) (maxDegrees - centerDegree);
		}
	}
	
	private void setHandTarget(float temperature) {
		if (temperature < minDegrees) {
			temperature = minDegrees;
		} else if (temperature > maxDegrees) {
			temperature = maxDegrees;
		}
		handTarget = temperature;
		handInitialized = true;
		invalidate();
	}
	
	private long mStartTime = 0;
	
	private Runnable mUpdateTimeTask = new Runnable() {
		public synchronized void run() {

		       int rpms = ((DashApplication) myApp).getRpm();
		       int mph = (((DashApplication) myApp).getKph() / 80) * 5;
	    	   
		       setHandTarget(rpms);
		       
		       mHandler.postDelayed(mUpdateTimeTask, 50);
		   }
	};
		
}
