package com.example.c1tappydefender;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;


public class TDView extends SurfaceView implements Runnable {

    private SoundPool soundPool;
    int start = -1;
    int bump = -1;
    int destroyed = -1;
    int win = -1;

    private boolean gameEnded;

    private Context context;

    private int screenX;
    private int screenY;

    private float distanceRemaining;
    private long timeTaken;
    private long timeStarted;
    private long fastestTime;

    volatile boolean playing;
    Thread gameThread = null;

    private PlayerShip player;
    public EnemyShip enemy1;
    public EnemyShip enemy2;
    public EnemyShip enemy3;
    public EnemyShip enemy4;
    public EnemyShip enemy5;

    ArrayList<SpaceDust> dustList = new ArrayList<SpaceDust>();

    private Paint paint;
    private Canvas canvas;
    private SurfaceHolder ourHolder;

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    private MediaPlayer mediaPlayer;

    TDView(Context context, int x, int y) {
        super(context);
        this.context  = context;

        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC,0);
        try{
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;

            descriptor = assetManager.openFd("start.ogg");
            start = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("win.ogg");
            win = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("bump.ogg");
            bump = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("crash.ogg");
            destroyed = soundPool.load(descriptor, 0);

        }catch(IOException e){
            Log.e("error", "failed to load sound files");
        }
        //Background music
        mediaPlayer = MediaPlayer.create(context, R.raw.song);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        screenX = x;
        screenY = y;

        // Initialize our drawing objects
        ourHolder = getHolder();
        paint = new Paint();

        // Load fastest time
        prefs = context.getSharedPreferences("HiScores", context.MODE_PRIVATE);
        // Initialize the editor ready
        editor = prefs.edit();
        // Load fastest time
        // if not available our highscore = 1000000
        fastestTime = prefs.getLong("fastestTime", 1000000);
        startGame();
    }

    private void startGame(){
        soundPool.play(start,1, 1, 0, 0, 1);

        player = new PlayerShip(context, screenX, screenY);

        enemy1 = new EnemyShip(context, screenX, screenY);
        enemy2 = new EnemyShip(context, screenX, screenY);
        enemy3 = new EnemyShip(context, screenX, screenY);
        if(screenX > 1000){
            enemy4 = new EnemyShip(context, screenX, screenY);
        }
        if(screenX > 1200){
            enemy5 = new EnemyShip(context, screenX, screenY);
        }

        int numSpecs = 400;

        for (int i = 0; i < numSpecs; i++) {
            SpaceDust spec = new SpaceDust(screenX, screenY);
            dustList.add(spec);
        }

        distanceRemaining = 10000;// 10 km
        timeTaken = 0;

        timeStarted = System.currentTimeMillis();

        gameEnded = false;
        soundPool.play(start, 1, 1, 0, 0, 1);
    }

    @Override
    public void run() {
        while (playing) {
            update();
            draw();
            control();
        }
    }

    private void update() {
        boolean hitDetected = false;
        if(Rect.intersects(player.getHitbox(), enemy1.getHitbox())){
            hitDetected = true;
            enemy1.setX(-1000);
        }
        if(Rect.intersects(player.getHitbox(), enemy2.getHitbox())){
            hitDetected = true;
            enemy2.setX(-1000);
        }
        if(Rect.intersects(player.getHitbox(), enemy3.getHitbox())){
            hitDetected = true;
            enemy3.setX(-1000);
        }
        if(screenX > 1000) {
            if (Rect.intersects(player.getHitbox(), enemy4.getHitbox())) {
                hitDetected = true;
                enemy4.setX(-1000);
            }
            if (screenX > 1200) {
                if (Rect.intersects(player.getHitbox(), enemy3.getHitbox())) {
                    hitDetected = true;
                    enemy5.setX(-1000);
                }
            }

            if (hitDetected) {
                soundPool.play(bump, 1, 1, 0, 0, 1);
                player.reduceShieldStrength();
                if (player.getShieldStrength() < 0) {
                    soundPool.play(destroyed, 1, 1, 0, 0, 1);
                    gameEnded = true;
                }
            }


            player.update();

            enemy1.update(player.getSpeed());
            enemy2.update(player.getSpeed());
            enemy3.update(player.getSpeed());
            if (screenX > 1000) {
                enemy4.update(player.getSpeed());
            }
            if (screenX > 1200) {
                enemy5.update(player.getSpeed());
            }
            //evades ConcurrentModificationException when game over
            try {
                for (SpaceDust sd : dustList) {
                    sd.update(player.getSpeed());
                }
            } catch (ConcurrentModificationException e) {
            }

            if (!gameEnded) {
                //subtract distance to home planet based on current speed
                distanceRemaining -= player.getSpeed();
                //How long has the player been flying
                timeTaken = System.currentTimeMillis() - timeStarted;
            }
            //Completed the game
            if (distanceRemaining < 0) {
                soundPool.play(win, 1, 1, 0, 0, 1);

                if (timeTaken < fastestTime) {
                    // Save high score
                    editor.putLong("fastestTime", timeTaken);
                    editor.commit();
                    fastestTime = timeTaken;
                }

                distanceRemaining = 0;
                gameEnded = true;
            }
        }
    }

    private void draw() {
        if (ourHolder.getSurface().isValid()) {
            //First we lock the area of memory we will be drawing to
            canvas = ourHolder.lockCanvas();
            // Rub out the last frame
            canvas.drawColor(Color.argb(255, 0, 0, 0));

            paint.setColor(Color.argb(255, 255, 255, 255));

            try{
                for (SpaceDust sd : dustList) {
                    canvas.drawPoint(sd.getX(), sd.getY(), paint);
                }
            } catch (ConcurrentModificationException e) {}

            // Draw the player and enemies
            canvas.drawBitmap(player.getBitmap(), player.getX(), player.getY(), paint);
            canvas.drawBitmap(enemy1.getBitmap(), enemy1.getX(), enemy1.getY(), paint);
            canvas.drawBitmap(enemy2.getBitmap(), enemy2.getX(), enemy2.getY(), paint);
            canvas.drawBitmap(enemy3.getBitmap(), enemy3.getX(), enemy3.getY(), paint);
            if(screenX > 1000) {
                canvas.drawBitmap(enemy4.getBitmap(), enemy4.getX(), enemy4.getY(), paint);
            }
            if(screenX > 1200) {
                canvas.drawBitmap(enemy5.getBitmap(), enemy5.getX(), enemy5.getY(), paint);
            }

            if(!gameEnded) {
                paint.setTextAlign(Paint.Align.LEFT);
                paint.setColor(Color.argb(255, 255, 255, 255));
                paint.setTextSize(25);
                canvas.drawText("Fastest:" + formatTime(fastestTime) + "s", 10, 20, paint);
                canvas.drawText("Time:" + formatTime(timeTaken) + "s", screenX / 2, 20, paint);
                canvas.drawText("Distance:" + distanceRemaining / 1000 + " KM", screenX / 3, screenY - 20, paint);
                canvas.drawText("Shield:" + player.getShieldStrength(), 10, screenY - 20, paint);
                canvas.drawText("Speed:" + player.getSpeed() * 60 + " MPS", (screenX / 3) * 2, screenY - 20, paint);
            }else{
                paint.setTextSize(80);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("Game Over", screenX/2, 100, paint);
                paint.setTextSize(25);
                canvas.drawText("Fastest:"+ formatTime(fastestTime) + "s", screenX/2, 160, paint);
                canvas.drawText("Time:" + formatTime(timeTaken) + "s", screenX / 2, 200, paint);
                canvas.drawText("Distance remaining:" + distanceRemaining/1000 + " KM",screenX/2, 240, paint);
                paint.setTextSize(80);
                canvas.drawText("Tap to replay!", screenX/2, 350, paint);
            }
            ourHolder.unlockCanvasAndPost(canvas);
        }
    }

    private void control() {
        //slows the frame rate to 60FPS
        try {
            gameThread.sleep(17);
        } catch (InterruptedException e) {

        }
    }
    // SurfaceView allows us to handle the onTouchEvent
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            // Has the player lifted there finger up?
            case MotionEvent.ACTION_UP:
                player.stopBoosting();
                break;
            // Has the player touched the screen?
            case MotionEvent.ACTION_DOWN:
                player.setBoosting();
                if(gameEnded){
                    startGame();
                }
                break;
        }
        return true;
    }

    // Clean up our thread if the game is interrupted or the player quits
    public void pause() {
        playing = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {

        }
    }

    public void resume() {
        playing = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    //making the time look nicer
    public static String formatTime(long time){
        long seconds = (time) / 1000;
        long thousandths = (time) - (seconds * 1000);
        String strThousandths = "" + thousandths;
        if (thousandths < 100){strThousandths = "0" + thousandths;}
        if (thousandths < 10){strThousandths = "0" + strThousandths;}
        String stringTime = "" + seconds + "." + strThousandths;
        return stringTime;
    }
}
