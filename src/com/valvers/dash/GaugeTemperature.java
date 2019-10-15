package com.valvers.dash;

import android.app.*;
import java.util.List;

import com.valvers.dash.BluetoothService;

import android.bluetooth.BluetoothAdapter;
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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

public final class GaugeTemperature extends View {

	private static final String TAG = GaugeTemperature.class.getSimpleName();
    private static final Boolean Debug = true;

	private Handler mHandler = new Handler();
	
	// drawing tools
	private RectF rimRect;
	private Paint rimPaint;
	private Paint rimCirclePaint;
	
	private RectF faceRect;
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
	// end drawing tools
	
	private Bitmap background; // holds the cached static part
	private static final String mTitle = "Coolant";
	
	// The update time for this gauge (in ms)
	private static final int mUpdateTime = 250;
	
	// Minimum and maximum values for the scale on this gauge
	private static final int scaleMinValue = 0;
	private static final int scaleMaxValue = 110;
	
	// Start angle for the gauge (zero is 3 o'clock) and the angle of 
	// needle travel
	private static final float scaleStartAngle = 180;
	private static final float scaleAngleSpan = 180; 
		
	// The delta change to make an extended graduation mark and display a 
	// number
	private static final float majorMarkDelta = 10;
	private static final float minorMarkDelta = 2;
	private static final float totalMarks =  ((scaleMaxValue - scaleMinValue) / minorMarkDelta);
	private static final float degreesPerMark = scaleAngleSpan / totalMarks;
		
	// The actual value and it's associated bits!
	private float value;
	private Paint valuePaint;
	private String valueString;
	private static final String valueUnitString = "'C";

	// hand dynamics -- all are angular expressed in F degrees
	private boolean handInitialized = false;
	private float handPosition = 500;
	private float handTarget = 1000;
	private float handVelocity = 0.0f;
	private float handAcceleration = 0.0f;
	private long lastHandMoveTime = -1L;
	
	private Context myApp;
	
	/*
	 * 
	 * Log a debug string (if debug is enabled)
	 * 
	 */
	public final void D(String s) {
		if (Debug)
		{
			Log.d(TAG, s);
		}
	}
	
	public GaugeTemperature(Context context) {
		super(context);	    
		init();
	}

	public GaugeTemperature(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public GaugeTemperature(Context context, AttributeSet attrs, int defStyle) {
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
		myApp = (DashApplication)getContext().getApplicationContext();
		
		initDrawingTools();

		if (mStartTime == 0L) {
            mStartTime = System.currentTimeMillis();
            mHandler.removeCallbacks(mUpdateTimeTask);
            mHandler.postDelayed(mUpdateTimeTask, 100);
       }
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
		
		facePaint = new Paint();		
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
		
		
		valuePaint = new Paint();
		valuePaint.setTextSize(0.11f);
		valuePaint.setColor(0xF0F0F0FF);
		valuePaint.setStrokeWidth(0.005f);
		valuePaint.setStyle(Paint.Style.FILL_AND_STROKE);
		valuePaint.setTypeface(Typeface.SANS_SERIF);
		valuePaint.setTextScaleX(0.8f);
		valuePaint.setTextAlign(Paint.Align.CENTER);
		valuePaint.setAntiAlias(true);
		
		
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
		
		//backgroundPaint = new Paint();
		//backgroundPaint.setAntiAlias(true);
		//backgroundPaint.setFilterBitmap(true);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		Log.d(TAG, "Width spec: " + MeasureSpec.toString(widthMeasureSpec));
		Log.d(TAG, "Height spec: " + MeasureSpec.toString(heightMeasureSpec));
		
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

		// Draw the scale graduation line, (0 is at 3 o'clock) and angles are
		// positive = clockwise
		canvas.drawArc(scaleRect, scaleStartAngle, scaleAngleSpan, false, scalePaint);
		
		// Save the current rotation and scaling matrix so we can restore
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		
		// Rotate the canvas so that scaleStartAngle is at 12 o'clock
		// (0 is at 3 o'clock) Canvas rotation is positive = counter-clockwise
		canvas.rotate(90 + scaleStartAngle, 0.5f, 0.5f);
		
		// Draw the graduation marks and the numbers
		float v = scaleMinValue;
		float yStart = scaleRect.top;
		float yEndMinor = yStart - 0.02f;
		float yEndMajor = yStart - 0.02f;
		
		do
		{
			
			if ((v % (int)majorMarkDelta == 0) || (v == scaleMinValue))
			{
				canvas.drawLine(0.5f, yStart + 0.025f, 0.5f, yEndMajor - 0.01f, scalePaint);
					
				String valueString = Integer.toString((int)v);
				canvas.drawText(valueString, 0.5f, yEndMajor - 0.02f, scalePaint);
			}
			else
			{
				canvas.drawLine(0.5f, yStart, 0.5f, yEndMinor, scalePaint);
			}
			
			v += minorMarkDelta;
			canvas.rotate(degreesPerMark, 0.5f, 0.5f);
		}
		while(v <= scaleMaxValue);
		
		canvas.restore();
		
		//
		//canvas.drawArc(scaleRect, 285.0f, 10.0f, false, softLimitPaint);
		//canvas.drawArc(scaleRect, 295.0f, 10.0f, false, hardLimitPaint);
		//
	}
	
	private float valueToAngle(float value) {
		float angle = value * (scaleAngleSpan / (scaleMaxValue - scaleMinValue));
		return angle + (90 + scaleStartAngle);
	}
	
	private void drawTitle(Canvas canvas) {
		canvas.drawTextOnPath(mTitle, titlePath, 0.0f,0.0f, titlePaint);				
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
			float handAngle = valueToAngle(handPosition);
			canvas.save(Canvas.MATRIX_SAVE_FLAG);
			canvas.rotate(handAngle, 0.5f, 0.5f);
			canvas.drawPath(handPath, handPaint);
			canvas.drawPath(handPath, handOutlinePaint);
			canvas.restore();
			
			canvas.drawCircle(0.5f, 0.5f, 0.01f, handScrewPaint);
		}
	}

	private void drawBackground(Canvas canvas) {
		if (background == null) {
			Log.w(TAG, "Background not created");
		} else {
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
		
		// Print the actual value, as it is always useful to know the actual value
		valueString = Integer.toString((int)value) + " " + valueUnitString;
		canvas.drawText(valueString, 0.5f, 0.30f, valuePaint);
		
		canvas.restore();
		moveHand();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		Log.d(TAG, "Size changed to " + w + "x" + h);
		
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
	}
	
	private float getRelativeTemperaturePosition() {
		return 0;
	}
	
	private void setHandTarget(float val) {

		// Limit the needle position to the scale
		if (val < scaleMinValue)
			val = scaleMinValue;
		else if (val > scaleMaxValue)
			val = scaleMaxValue;

		handTarget = val;
		handInitialized = true;
		value = val;
		invalidate();
	}
	
	private long mStartTime = 0;
	
	private Runnable mUpdateTimeTask = new Runnable() {
		public synchronized void run() {
			int water = ((DashApplication) myApp).getWaterTemp();
		    
			if ((water >= scaleMinValue) && (water <= scaleMaxValue))
				setHandTarget(water);
			
			// This gauge does not need to be updated *that* often!
			mHandler.postDelayed(mUpdateTimeTask, mUpdateTime);
		}
	};
		
}
