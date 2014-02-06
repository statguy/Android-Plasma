package com.codeandpop.android.plasma;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class PlasmaView extends SurfaceView implements SurfaceHolder.Callback {
	DrawThread thread;
	Bitmap bitmap;
	int[] colors;
	int width, height;
	long startTime = System.currentTimeMillis();
	long timeNow, timeDelta, timePrevFrame = 0;
	
	public PlasmaView(Context context) {
		super(context);
		//setFocusable(true);
		getHolder().addCallback(this);
	}
	
	Rect srcWindow, dstWindow;
	
    @Override
    public void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        this.width = width;
        this.height = height;
        
        initializePlasma(64, 256, 2);
        
        //paint.setAntiAlias(true);
        //paint.setFilterBitmap(true);
        //paint.setDither(true);
    }

    public void initializePlasma(int sideLength, int numColors, int numFunctions) {
    	this.sideLength = sideLength;
    	this.numFunctions = numFunctions;
    	this.halfNumColors = numColors / 2;
    	
    	bitmap = Bitmap.createBitmap(sideLength, sideLength, Bitmap.Config.RGB_565);//.ARGB_8888);
    	srcWindow = new Rect(0, 0, sideLength, sideLength);
    	dstWindow = new Rect(0, 0, width, height);
    	buffer = new int[sideLength * sideLength];
    	
        // period a: sin(a * theta) = 2*pi/a
        // a = 32 => 2*pi/32 = 0.1963495
		sidePeriod = 2 * Math.PI / sideLength;
        halfSideLength = sideLength / 2;
        colorPeriod = 2 * Math.PI / numColors;
    }
    
    //Paint paint = new Paint();
    int buffer[];
    int sideLength, halfSideLength, numFunctions, halfNumColors;
    double sidePeriod, colorPeriod;
    double t = 0;
    float[] HSV = new float[] { 0, 1, 1 };
    double dist(double x1, double y1, double x2, double y2) {
    	return Math.sqrt((x1-x2) * (x1-x2) + (y1-y2) * (y1-y2));
    }
    
    @Override
    public void onDraw(Canvas canvas) {
    	if (canvas == null) return;
    	super.onDraw(canvas);

    	//long start = System.currentTimeMillis();
    	        
		for (int x = 0; x < sideLength;  x++) {
		    for (int y = 0; y < sideLength; y++) {
		    	/*
		    	double hue = halfNumColors * (
	    			  1 + Math.sin(x * sidePeriod + t)
	    			+ 1 + Math.sin(dist(x, y, (halfSideLength + halfSideLength * Math.sin(-t)), (halfSideLength + halfSideLength * Math.cos(-t))) * sidePeriod)
    			) / numFunctions;
		    	HSV[0] = (float)hue;
		    	buffer[y * sideLength + x] = Color.HSVToColor(HSV);
		    	*/
		    	double color = halfNumColors * (
	    			    1 + Math.sin(2*sidePeriod * (x * Math.sin(t/2) + y * Math.cos(t/3)) + t) 
	    			  + 1 + Math.sin(dist(x, y, (halfSideLength + halfSideLength * Math.sin(-t)), (halfSideLength + halfSideLength * Math.cos(-t))) * sidePeriod)
	    			  + 1 + Math.sin(t + dist(x, y, (halfSideLength + halfSideLength * Math.sin(t/5)), (halfSideLength + halfSideLength * Math.cos(t/3))) * sidePeriod)
    			) / numFunctions;
    			buffer[y * sideLength + x] = Color.rgb(
					(int)(halfNumColors + halfNumColors * Math.cos(color * colorPeriod + t/3)),
					(int)(halfNumColors + halfNumColors * Math.sin(color * colorPeriod + t)),
					(int)(halfNumColors + halfNumColors * Math.sin(-t / 5))
				);
		    }
		}

		//long end1 = System.currentTimeMillis();
		
		bitmap.setPixels(buffer, 0, sideLength, 0, 0, sideLength, sideLength);
		canvas.drawBitmap(bitmap, srcWindow, dstWindow, null);
		t += 0.1;
		
		//long end2 = System.currentTimeMillis();
		//Log.d("", "d1 = "+(end1-start)+" d2 = "+(end2-start));
    }
    
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		setWillNotDraw(false);
		thread = new DrawThread(holder);
        thread.setRunning(true);
        thread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            }
            catch (InterruptedException e) {
            }
        }
	}
	
	class DrawThread extends Thread {
        private SurfaceHolder surfaceHolder;
        private boolean run = false;
        private int FPS = 60, SLEEP = 1000 / FPS;

        public DrawThread(SurfaceHolder surfaceHolder) {
            this.surfaceHolder = surfaceHolder;
        }

        public void setRunning(boolean run) {
            this.run = run;
        }

        public SurfaceHolder getSurfaceHolder() {
            return surfaceHolder;
        }

		@Override
        public void run() {
            while (run) {
                timeNow = System.currentTimeMillis();

                Canvas c = surfaceHolder.lockCanvas();
                if (c != null) {
	                try {
	                    synchronized (surfaceHolder) {
	                    	postInvalidate();
	                    }
	                }
	                finally {
	                    surfaceHolder.unlockCanvasAndPost(c);
	                }
	            }
                
                timeDelta = timeNow - timePrevFrame;
                if (timeDelta < SLEEP) {
                	try {
                        Thread.sleep(SLEEP - timeDelta);
                	}
                    catch(InterruptedException e) {
                    }
                }
                timePrevFrame = System.currentTimeMillis();
            }
        }
    }
}
