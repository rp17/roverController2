package rover.control;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
//import ioio.lib.util.AbstractIOIOActivity;
import ioio.lib.util.android.IOIOActivity;
import ioio.lib.android.bluetooth.*;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.os.Environment;
import android.provider.MediaStore.Files;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.net.UnknownHostException;

import org.apache.commons.io.FileUtils;

import rover.netclient.IPIDClient;
import rover.netclient.TCPNetClient;
import rover.netclient.UDPNetClient;
import rover.netclient.UDPUpdater;
import rover.pid.PIDControl;
import rover.pid.PIDTimer;
import smartSweeper.jGenAlg;
import smartSweeper.jNeuralNet;
import smartSweeper.jParams;
import smartSweeper.utils;


//public class ServoControlActivity extends AbstractIOIOActivity {
public class RoverControlActivity extends IOIOActivity implements SensorEventListener, LocationListener, OnClickListener, SeekBar.OnSeekBarChangeListener, OnInitListener {
	public final static int NOCMD = 0;
	public final static int FORWARD = 1;
	public final static int BACKWARD = 2;
	public final static int LEFT = 3;
	public final static int RIGHT = 4;
	public final static int STOP = 5;
	public final static int SLIGHTRIGHT = 6;
	public final static int SLIGHTLEFT = 7;
	public final static int TIMEDPID = 8;
	public final static int LeftObst = 9;
	public final static int RightObst = 10;
	public final static int MANUAL = -1;
	public static int VOICECMD = NOCMD;
	public static int VOICESTATUS = 0;
	public volatile int command = MANUAL;
	private final int obstDist = 7;
	private final int leftTurnDeg = 15;
	private final int rightTurnDeg = 15;
	private boolean firstLeft = true;
	private boolean firstRight = true;
	private float newAzimut = 0.0f;
	private final static float sensorDiffTolerance = 0.5f;
	
	
	
	private final static float CONTROL_FREQ = 100.0f; // Hz
	private final static float ITER_DURATION = 1.0e6f/CONTROL_FREQ; // single control loop iteration duration in millisecs
	
	private final int MOTOR1 = 11;		// Left-Front, forward true
	private final int MOTOR2 = 12;		// Left-Rear, forward false
	private final int MOTOR3 = 13;		// Right-Front, forward true 
	private final int MOTOR4 = 14;		// Right-Rear, forward false
	private final int DIRECTION1 = 3;
	private final int DIRECTION2 = 6;
	private final int DIRECTION3 = 7;
	private final int DIRECTION4 = 10;
	private final int PWM_FREQ = 100;
	private final int SENSOR1 = 35; // right sonar
	private final int SENSOR2 = 36; // left sonar
	
	private int SPEED = 1500;
	
	private int SERVER_PORT = 49005;
	private int SERVER_PORT2 = 49006;
	
	private String SERVER_IP = null;
	
	private Button bForward;
	private Button bBackward;
	private Button bLeft;
	private Button bRight;
	
	private ToggleButton tMotor1;
	private ToggleButton tMotor2;
	private ToggleButton tMotor3;
	private ToggleButton tMotor4;
	
	private SeekBar sBar;
	//private SeekBar sBar_steer;
	private SeekBar sBar_R;
	private SeekBar sBar_L;
	
	private TextView txtViewSensor1;
	private TextView txtViewSensor2;
	private static final ExecutorService singleClientPool = Executors.newSingleThreadExecutor();
	private static final ExecutorService singleUpdatePool = Executors.newSingleThreadExecutor();

	
	
	private UDPNetClient clientLoop = new UDPNetClient(this);
	private UDPUpdater updateLoop = new UDPUpdater(this);
	private static float sensors[];
	
	private SensorManager mSensorManager;
	Sensor accelerometer;
	Sensor magnetometer;
	
	public static volatile float azimut = 0.0f; // in radians
	static volatile float azimutPre = 0.0f;
	static final float azimutGain = 0.7f;
	public static volatile int avgAzimut = 0; // in degrees
	
	
	private LocationManager locationManager;
	
	private static double android_GPS_Lat = 0;
	private static double android_GPS_Lon = 0;
	private static double skyhook_GPS_Lat = 0;
	private static double skyhook_GPS_Lon = 0;
	
	private float origSteerProgress;
	private int steerPercent = 0;
	private int desiredCourse = 0;
	private int duration = 0;
	private volatile long durationMillis = 0;
	
	
	// Speech Variables
	private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
	// Speech Recognizer instance (analyzes user voice)
	private SpeechRecognizer speech;
	// Text To Speech instance (android Voice)
	private TextToSpeech repeatTTS;
	
	
	private static ToggleButton togSkyHookAccuracy;
	private static TextView txtAndroidGPS;
	private TextView txtSkyHookGPS;
	
	private String filename = "MySampleFile.txt";
	private String filepath = "MyFileStorage";
	File myExternalFile;
	String text;
	String email = "tsullivan320@yahoo.com";
	
	jNeuralNet brain;
	jGenAlg ga = new jGenAlg(10, jParams.dMutationRate, jParams.dCrossoverRate, 15);
	Vector<jGenAlg.SGenome> population;
	jGenAlg.SGenome currentGenome;

	
	public Handler handler = new Handler() {
		  @Override
		  public void handleMessage(Message msg) { 

			Bundle bundle = msg.getData();
			int cmd = bundle.getInt("command");
			int spd = bundle.getInt("speed");
			float tn = bundle.getInt("turn");
			int dc = bundle.getInt("desiredcourse"); 
			int dur = bundle.getInt("duration");
	
			txtAndroidGPS.setText("command: " + cmd + ", speed: " + spd + " turn: " + tn + " course: " + dc + " duration: " + dur);
		  }
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		System.out.println("Constructor for steering called");
		//jParams.LoadInParameters("params.ini");
		brain = new jNeuralNet(3,2,1,3);
		population = new Vector<jGenAlg.SGenome>();
		Vector<Double> weight = new Vector<Double>();
		for (int j = 0; j < 10; j++){
			weight.clear();
			for (int i = 0; i < brain.GetWeights().size(); ++i){
				weight.add(utils.RandomClamped());
			}
			population.add(ga.new SGenome(weight, 0.0));
		}
		population = ga.Epoch(population);
		currentGenome = ga.GetChromoRoulette();
		if(currentGenome.vecWeights.size() == 15)
			brain.PutWeights(currentGenome.vecWeights);
		else
			System.out.println("currentGenome.vecWeights has wrong size");
		System.out.println("Size of vecWeights in currentGenome: " + currentGenome.vecWeights.size());
		brain.PutWeights(currentGenome.vecWeights);
		System.out.println("weights: " + brain.GetWeights());
		
		
		ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
		  File directory = contextWrapper.getDir(filepath, Context.MODE_PRIVATE);
		  myExternalFile = new File(directory , filename);
		  
		  Button saveToExternalStorage = 
				  (Button) findViewById(R.id.saveExternalStorage);
				  saveToExternalStorage.setOnClickListener(this);
				 
		Button readFromExternalStorage = 
				(Button) findViewById(R.id.getExternalStorage);
				readFromExternalStorage.setOnClickListener(this);
		
		Button sendFile = 
				(Button) findViewById(R.id.sendFile);
				sendFile.setOnClickListener(this);
				
		Button turnNNon = 
				(Button) findViewById(R.id.turnNNon);
				turnNNon.setOnClickListener(this);

		
		// IPActivity (the info user typed in) is stored in a Bundle and 
		// passed to RoverControlActivity
		Bundle extras = getIntent().getExtras();
		SERVER_IP = extras.getString("serverIP");
		
		bForward = (Button) findViewById(R.id.btnForward);
		bBackward = (Button) findViewById(R.id.btnBackward);
		bLeft = (Button) findViewById(R.id.btnLeft);
		bRight = (Button) findViewById(R.id.btnRight);
		
		tMotor1 = (ToggleButton) findViewById(R.id.tgMotor1);
		tMotor2 = (ToggleButton) findViewById(R.id.tgMotor2);
		tMotor3 = (ToggleButton) findViewById(R.id.tgMotor3);
		tMotor4 = (ToggleButton) findViewById(R.id.tgMotor4);
		
		sBar = (SeekBar) findViewById(R.id.seekBar1);
		
		sBar_R = (SeekBar) findViewById(R.id.seekBarR);
		sBar_R.setProgress(sBar_R.getMax()/2);
		sBar_L = (SeekBar) findViewById(R.id.seekBarL);
		sBar_L.setProgress(sBar_L.getMax()/2);
		
		//origSteerProgress = sBar_steer.getMax()/2;
		//sBar_steer.setOnSeekBarChangeListener(this);

		txtViewSensor1 = (TextView) findViewById(R.id.txtVoltageR);
		txtViewSensor2 = (TextView) findViewById(R.id.txtVoltageL);
		sensors = new float[5];
		//new IMU().start();
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
    	accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    	magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    	mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
	    mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
	    
	    
	    
	    /** Android GPS 
	     *  Gps Location Service LocationManager Object 
	     */
	    txtAndroidGPS = (TextView) findViewById(R.id.androidGPS);
	    
	    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER,
                0,   // Immediate
                0, this);
        
        /** SkyHook GPS
         *
         */
        txtSkyHookGPS = (TextView) findViewById(R.id.skyHookGPS);
        togSkyHookAccuracy = (ToggleButton) findViewById(R.id.skyHookTogButton);
	    
	    
        /** Speech Recognizer
         *
         */
        
        	// Create speech recognizer
        /*
     		speech = SpeechRecognizer.createSpeechRecognizer(this);

     		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
     		// Analyze English language
     		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
     		intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
     				this.getPackageName());
     		// After analyzing user voice return top 5 best results
     		intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10);
     		// Keep listener on for 1 second as soon as a voice is recognized
     		// This helps speed up the voice analyze process
     		intent.putExtra(
     				RecognizerIntent. EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
     				1500);

     		// Set up Text to Speech
     		startRepeat();
     		repeatTTS = new TextToSpeech(this, this);

     		RoverSpeechListener speechListener = new RoverSpeechListener(this, speech,
     				intent, repeatTTS);

     		speech.setRecognitionListener(speechListener);
     		
     		// Start Listener
     		speech.startListening(intent);
     		*/
	    
		try {
			boolean resUpdater = updateLoop.serverConnect(SERVER_IP, SERVER_PORT2);
        	if(resUpdater) {
        		singleUpdatePool.execute(updateLoop);
        	}
        	boolean res = clientLoop.serverConnect(SERVER_IP, SERVER_PORT);
        
        	if(res) {
        		singleClientPool.execute(clientLoop);
        	}
        	
        	
        }
        catch(final UnknownHostException ex) {
        	runOnUiThread(new Runnable(){
   		 		@Override
   		 		public void run() {
   		 			// User is at the waypoint. Display an alert toast for a few seconds
   		 			Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
   		 		}
   		 	});
        }
        catch(final IOException ex) {
        	runOnUiThread(new Runnable(){
   		 		@Override
   		 		public void run() {
   		 			// User is at the waypoint. Display an alert toast for a few seconds
   		 			Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
   		 		}
   		 	});
        }
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		enableUi(false);
		
		if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {  
			   saveToExternalStorage.setEnabled(false);
			  } 
			  else {
			   myExternalFile = new File(getExternalFilesDir(filepath), filename);
			  }

	}
	
	public void onClick(View v) {
		
		  //EditText myInputText = (EditText) findViewById(R.id.myInputText);
		  TextView responseText = (TextView) findViewById(R.id.responseText);
		  TextView showFile = (TextView) findViewById(R.id.showFile);
		  String myData = "";
		  
		  myExternalFile.setReadable(true,false);

		  switch (v.getId()) {
		  case R.id.saveExternalStorage:
		   try {
		    FileOutputStream fos = new FileOutputStream(myExternalFile);
		    fos.write(brain.GetWeights().toString().getBytes());
		    fos.write(brain.getInputs());
		    fos.close();
		   } catch (IOException e) {
		    e.printStackTrace();
		   }
		   showFile.setText("");
		   responseText.setText("MySampleFile.txt saved to External Storage...");
		   break;
		 
		  case R.id.getExternalStorage:
		   try {
		    FileInputStream fis = new FileInputStream(myExternalFile);
		    DataInputStream in = new DataInputStream(fis);
		    BufferedReader br = 
		     new BufferedReader(new InputStreamReader(in));
		    String strLine;
		    while ((strLine = br.readLine()) != null) {
		     myData = myData + strLine;
		    }
		    in.close();
		   } catch (IOException e) {
		    e.printStackTrace();
		   }
		   showFile.setText(myData);
		   responseText.setText("MySampleFile.txt data retrieved from External Storage...");
		   break;
		  
		  case R.id.sendFile:
			  try {
					FileInputStream is = new FileInputStream(myExternalFile);
					int size = is.available();
					byte[] buffer = new byte[size];
					is.read(buffer);
					is.close();
					text = new String(buffer);
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
			  
			  Intent sendIntent = new Intent();
			  sendIntent.setAction(Intent.ACTION_SEND);
			  sendIntent.putExtra(Intent.EXTRA_SUBJECT, "mySampleFile.txt");
			  sendIntent.putExtra(Intent.EXTRA_TEXT, text);
			  sendIntent.setType("text/plain");
			  startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));
			  showFile.setText("");
			  responseText.setText("select where to send 'mySampleFile' ");  
		  break;
		  
		  /*case R.id.turnNNon:
			  System.out.println("Constructor for steering called");
				jParams.LoadInParameters("params.ini");
				brain = new jNeuralNet();
				population = new Vector<jGenAlg.SGenome>();
				Vector<Double> weight = new Vector<Double>();
				for (int j = 0; j < 10; j++){
					weight.clear();
					for (int i = 0; i < brain.GetWeights().size(); ++i){
						weight.add(utils.RandomClamped());
					}
					population.add(ga.new SGenome(weight, 0.0));
				}
				population = ga.Epoch(population);
				currentGenome = ga.GetChromoRoulette();
				if(currentGenome.vecWeights.size() == 15)
					brain.PutWeights(currentGenome.vecWeights);
				else
					System.out.println("currentGenome.vecWeights has wrong size");
				System.out.println("Size of vecWeights in currentGenome: " + currentGenome.vecWeights.size());
				brain.PutWeights(currentGenome.vecWeights);
			break;*/
		  }
		  }
		  
		  private static boolean isExternalStorageReadOnly() {  
			  String extStorageState = Environment.getExternalStorageState();  
			  if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {  
			   return true;  
			  }  
			  return false;  
			 }  
			 
			 private static boolean isExternalStorageAvailable() {  
			  String extStorageState = Environment.getExternalStorageState();  
			  if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {  
			   return true;  
			  }  
			  return false;   
		 }


//	class IOIOThread extends AbstractIOIOActivity.IOIOThread {
	class IOIOThread extends BaseIOIOLooper{
		private PwmOutput pwmMotor1;
		private PwmOutput pwmMotor2;
		private PwmOutput pwmMotor3;
		private PwmOutput pwmMotor4;
		
		private DigitalOutput direction1;
		private DigitalOutput direction2;
		private DigitalOutput direction3;
		private DigitalOutput direction4;
		
		private AnalogInput sensor1; // right sonar
		private AnalogInput sensor2; // left sonar
		
		private PIDControl pidControl = new PIDControl();
		private PIDTimer timer= new PIDTimer();

		public void setup() throws ConnectionLostException {
			try {
				pwmMotor1 = ioio_.openPwmOutput(MOTOR1, PWM_FREQ);
				pwmMotor2 = ioio_.openPwmOutput(MOTOR2, PWM_FREQ);
				pwmMotor3 = ioio_.openPwmOutput(MOTOR3, PWM_FREQ);
				pwmMotor4 = ioio_.openPwmOutput(MOTOR4, PWM_FREQ);
				
				direction1 = ioio_.openDigitalOutput(DIRECTION1, true);
				direction2 = ioio_.openDigitalOutput(DIRECTION2, true);
				direction3 = ioio_.openDigitalOutput(DIRECTION3, true);
				direction4 = ioio_.openDigitalOutput(DIRECTION4, true);
				
				sensor1 = ioio_.openAnalogInput(SENSOR1);
				sensor2 = ioio_.openAnalogInput(SENSOR2);
				for(int i=0; i<distFwdBufR.length; i++) {
					distFwdBufR[i] = 5.0f;
					distFwdBufL[i] = 5.0f;
				}
				enableUi(true);
				
			} catch (ConnectionLostException e) {
				enableUi(false);
				throw e;
			}
		}
		
		public void setTextSensor1(final String msg){
			runOnUiThread(new Runnable(){
				@Override
				public void run(){
					txtViewSensor1.setText(msg);	
				}
			});
		}
		public void setTextSensor2(final String msg){
			runOnUiThread(new Runnable(){
				@Override
				public void run(){
					txtViewSensor2.setText(msg);	
				}
			});
		}
		private float[] distFwdBufR = new float[3];
		private float[] distFwdBufL = new float[3];
		public void loop() throws ConnectionLostException {

			float distFwdR;
			float avgDistFwdR = 1.0f;
			float distFwdL;
			float avgDistFwdL = 1.0f;
			//updateGPS_UI();
			try {
				distFwdR = sensor1.read();
				distFwdL = sensor2.read();
				distFwdR *= 100;
				distFwdL *= 100;
				for(int i=0; i< (distFwdBufR.length - 1); i++) {
					distFwdBufR[i] = distFwdBufR[i+1];
					distFwdBufL[i] = distFwdBufL[i+1];
				}
				distFwdBufR[distFwdBufR.length - 1] = distFwdR;
				distFwdBufL[distFwdBufL.length - 1] = distFwdL;
				avgDistFwdR = 0.0f;
				avgDistFwdL = 0.0f;
				for(float x: distFwdBufR) {
					avgDistFwdR += x;
				}
				for(float x: distFwdBufL) {
					avgDistFwdL += x;
				}
				avgDistFwdR = avgDistFwdR / distFwdBufR.length;
				avgDistFwdL = avgDistFwdL / distFwdBufL.length;

				setTextSensor1("" + Float.toString(avgDistFwdR));
				setTextSensor2("" + Float.toString(avgDistFwdL));

			} catch (InterruptedException e) {
				ioio_.disconnect();
			} catch (ConnectionLostException e) {
				enableUi(false);
				throw e;
			}
			//command = clientLoop.cmd;

			if(command == MANUAL) {		// MANUAL

				//sBar_steer.setVisibility(View.VISIBLE);
				SPEED = sBar.getProgress();
				//Log.d("RoverController", "MANUAL, command: " + command);
				try {

					if(bForward.isPressed() || bBackward.isPressed()) {

						// Manual Forward
						if(bForward.isPressed()){
							forward();
						} 
						else {	// Manual Backward
							backward();
						}
					}
					else if(bLeft.isPressed() || bRight.isPressed()) {

							// Manual Left				
							if(bLeft.isPressed()){
								left();
							}
							else { 	// Manual Right
								right();
							}
					} else {	// Stop
						stop();
					}
				Thread.sleep(10);
			} catch (InterruptedException e) {
				ioio_.disconnect();
			} catch (ConnectionLostException e) {
				enableUi(false);
				throw e;
			}
		}
		else {	// REMOTE

			//sBar_steer.setVisibility(View.INVISIBLE); 

				try {

					Log.d("RoverController", "inside REMOTE");
					switch(command) {
					case TIMEDPID: timedPID(avgDistFwdL, avgDistFwdR); break;
					case FORWARD: forward(); break;
					case BACKWARD: backward(); break;
					case SLIGHTLEFT: slightLeft(steerPercent); break;
					case SLIGHTRIGHT: slightRight(steerPercent); break;
					case LEFT: left(); break;
					case RIGHT: right(); break;
					case STOP: stop(); break;
					case LeftObst:
						if(firstLeft) {
							newAzimut = calcLeftAzimut(avgAzimut);
							firstLeft = false;
						}
						leftUntilAzimut();
						break;
					case RightObst: 
						if(firstRight) {
							newAzimut = calcRightAzimut(avgAzimut);
							firstRight = false;
						}
						rightUntilAzimut();
						break;
					default: break;
					}
					Thread.sleep(10);
				} catch (InterruptedException e) {
					ioio_.disconnect();
				} catch (ConnectionLostException e) {
					enableUi(false);
					throw e;
				}
		}
	}

	private float calcLeftAzimut(float curAzimut) {
		float resAzimut = curAzimut - leftTurnDeg;
		if(resAzimut < -180) {
			resAzimut = 360 + resAzimut;
		}
		return resAzimut;
	}
	private float calcRightAzimut(float curAzimut) {
		float resAzimut = curAzimut + rightTurnDeg;
		if(resAzimut > 180) {
			resAzimut = resAzimut - 360;
		}
		return resAzimut;
	}
	
	private float[] diffBuf = new float[3];
	private void diffBufClear() {
		for(int i = 0; i< diffBuf.length; ++i) {
			diffBuf[i] = 0.0f;
		}
	}
	private int diffSumCounter = 0;
	private void resetTimerDiffCounter() {
		timer.clear();
		diffBufClear();
		diffSumCounter = 0;
	}
	private void timedPID(float distL, float distR) 
			throws ConnectionLostException 
			{
		Log.d("RoverController", "PID running");
		timer.startTimer();
		//float dt=((float)System.currentTimeMillis() - heading_PID_timer)/1000.0f;
		final long timeLeft = timer.getTime();
		if(timeLeft > durationMillis) {
			resetTimerDiffCounter();
			command = STOP;
			runOnUiThread(new Runnable(){
   		 		@Override
   		 		public void run() {
   		 		txtAndroidGPS.setText("command: " + command);
   		 		}
   		 	});
			
			
			return;
		} else {
			if(distL < obstDist || distR < obstDist) {
				if(Math.abs(distR - distL) > sensorDiffTolerance) {
						if(distL < distR) {
							command = RightObst;
						}
						else {
							command = LeftObst;
						}
						return;
				}
				else {
					command = LeftObst;
					return;
				}
			}
			diffSumCounter++;
			final int courseError = (int) (desiredCourse - avgAzimut);
			int diffControl = pidControl.PID_heading(courseError);
			for(int i=0; i< (diffBuf.length - 1); i++) {
				diffBuf[i] = diffBuf[i+1];
			}
			diffBuf[diffBuf.length - 1] = diffControl;
			float diffSum = 0.0f; 
			for(float x: diffBuf) {
				diffSum += x;
			}
			final float avgDiff = diffSum*0.1f;
			runOnUiThread(new Runnable(){
   		 		@Override
   		 		public void run() {
   		 			txtAndroidGPS.setText("command: " + command + " time left " + timeLeft + " error " + courseError + " avgDiff " + avgDiff);
   		 		}
   		 	});
			
			final String msg = "PID: azimut: " + avgAzimut + " error " + courseError + " diff: " + avgDiff;
			Log.d("RoverController", msg);
			/*
			runOnUiThread(new Runnable(){
   		 		@Override
   		 		public void run() {
   		 			// User is at the waypoint. Display an alert toast for a few seconds
   		 			Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
   		 		}
   		 	});
   		 	*/
			// Guessing for now
			if(diffSumCounter > 9) {
				if(avgDiff < 0) // steer left on negative diff and negative error
					slightLeft(-(int)avgDiff);
				else // steer right on negative diff and negative error
					slightRight((int)avgDiff);
			}
				
		}
		
	}
	private void forward() throws ConnectionLostException {
		// Server Command - Forward
		direction1.write(true);
		direction2.write(false);
		direction3.write(true);
		direction4.write(false);
		setSpeed();
	}

	private void backward() throws ConnectionLostException {
		// Server Command - Backward
		direction1.write(false);
		direction2.write(true);
		direction3.write(false);
		direction4.write(true);
		setSpeed();
	}

	
	private void leftUntilAzimut() throws ConnectionLostException {
		if(avgAzimut <= newAzimut) {
			resetTimerDiffCounter();
			firstLeft = true;
			command = TIMEDPID;
			return;
		}
		else {
			left();
		}
	}
	
	private void rightUntilAzimut() throws ConnectionLostException {
		if(avgAzimut >= newAzimut) {
			resetTimerDiffCounter();
			firstRight = true;
			command = TIMEDPID;
			return;
		}
		else {
			right();
		}
		
	}
	
	private void leftUntilNoObst(float dist) throws ConnectionLostException {
		if(dist < obstDist) {
			left();
		}
		else {
			command = TIMEDPID;
		}
	}
	
	private void rightUntilNoObst(float dist) throws ConnectionLostException {
		if(dist < obstDist) {
			right();
		}
		else {
			command = TIMEDPID;
		}
	}
	private void left() throws ConnectionLostException {
		direction1.write(false);
		direction2.write(true);
		direction3.write(true);
		direction4.write(false);
		setSpeed();
	}

	private void right() throws ConnectionLostException {
		direction1.write(true);
		direction2.write(false);
		direction3.write(false);
		direction4.write(true);	
		setSpeed();
	}

	private void setSpeed() throws ConnectionLostException {
		//pulseWidth range 0 - 1500
		if(tMotor1.isChecked()) pwmMotor1.setPulseWidth(SPEED);
		if(tMotor2.isChecked()) pwmMotor2.setPulseWidth(SPEED);
		if(tMotor3.isChecked()) pwmMotor3.setPulseWidth(SPEED);
		if(tMotor4.isChecked()) pwmMotor4.setPulseWidth(SPEED);
	}
	
	private void slightLeft(int diff) throws ConnectionLostException {
		// Server Command - Forward
		direction1.write(true);
		direction2.write(false);
		direction3.write(true);
		direction4.write(false);
	
		float diffRatio = (float) (100 - diff) * 0.01f;
	
		int LSPEED = (int) (((float) SPEED) * diffRatio);
	
		//pulseWidth range 0 - 1500
		if(tMotor1.isChecked()) pwmMotor1.setPulseWidth(LSPEED);
		if(tMotor2.isChecked()) pwmMotor2.setPulseWidth(LSPEED);
		if(tMotor3.isChecked()) pwmMotor3.setPulseWidth(SPEED);
		if(tMotor4.isChecked()) pwmMotor4.setPulseWidth(SPEED);
	}

	private void slightRight(int diff) throws ConnectionLostException {
		// Server Command - Forward
		direction1.write(true);
		direction2.write(false);
		direction3.write(true);
		direction4.write(false);
	
		float diffRatio = (float) (100 - diff) * 0.01f;
	
		int RSPEED = (int) (((float) SPEED) * diffRatio);
	
		//pulseWidth range 0 - 1500
		if(tMotor1.isChecked()) pwmMotor1.setPulseWidth(SPEED);
		if(tMotor2.isChecked()) pwmMotor2.setPulseWidth(SPEED);
		if(tMotor3.isChecked()) pwmMotor3.setPulseWidth(RSPEED);
		if(tMotor4.isChecked()) pwmMotor4.setPulseWidth(RSPEED);
	}

	private void stop() throws ConnectionLostException {
	
		pwmMotor1.setPulseWidth(0);
		pwmMotor2.setPulseWidth(0);
		pwmMotor3.setPulseWidth(0);
		pwmMotor4.setPulseWidth(0);
	
		}
	}
	

	@Override
//	protected AbstractIOIOActivity.IOIOThread createIOIOThread() {
	protected IOIOLooper createIOIOLooper() {
		return new IOIOThread();
	}

	private void enableUi(final boolean enable) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				bForward.setEnabled(enable);
				bBackward.setEnabled(enable);
			}
		});
	}
	
	public void btnFordwardonTouch(View v, MotionEvent e){
		
	}
	
	public void btnBackwardonTouch(View v, MotionEvent e){
		
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	public static float getSensorReadings(int indx){
		return sensors[indx];
	}
	
	private static boolean compFirstRun = true;
	private static float[] azimBuf = new float[20];
	private float[] mGravity;
	private float[] mGeomagnetic;
	private float azimAvg = 0.0f;
	private float RMat[] = new float[9];
	private float I[] = new float[9];
	private float orientation[] = new float[3];
	public void onSensorChanged(final SensorEvent event) {
		sensors[0] = event.values[0];
		sensors[1] = event.values[1];
		sensors[2] = event.values[2];
           if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            		mGravity = event.values;
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            		mGeomagnetic = event.values;
            if (mGravity != null && mGeomagnetic != null) {
            		//float R[] = new float[9];
            		//float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(RMat, I, mGravity, mGeomagnetic);
            		if (success) {
            			//float orientation[] = new float[3];
            			SensorManager.getOrientation(RMat, orientation);
            			azimut = orientation[0]; // orientation contains: azimut, pitch and roll
            			
            			
            			if(compFirstRun) {
            				compFirstRun = false;
            				for(int i = 0; i<azimBuf.length; i++){
            					azimBuf[i] = azimut; 
            				}
            			}
            			else {
            				for(int i=0; i< (azimBuf.length - 1); i++) {
            					azimBuf[i] = azimBuf[i+1];
            				}
            				azimBuf[azimBuf.length - 1] = azimut;
            			}
            			float avg = 0.0f;
            			for(float x: azimBuf){
            				avg += x; 
            			}
            			avg *= 0.05f;
            			
            			avgAzimut = (int)Math.toDegrees(azimut);
            			//avgAzimut = (int)Math.toDegrees(avg);
            			
            			//azimut = azimutPre*azimutGain + azimut*(1-azimutGain);
            			//azimutPre = azimut;
                		//tv_current_comp_course.setText(String.valueOf(avgAzimut) + " ");
                		
            	}
            }
	   
	}
	
    public void updateGPS_UI() {
    	runOnUiThread(new Runnable(){
		 		@Override
		 		public void run() {
		 			//txtAndroidGPS.setText("Android GPS: lat " + android_GPS_Lat + ", " + android_GPS_Lon);

		 			txtSkyHookGPS.setText("SkyHook GPS: lat " + skyhook_GPS_Lat + ", " + skyhook_GPS_Lon);
		 		}
		 	});
    }
	
	@Override
	protected void onResume() {
		super.onResume();
		try {
			boolean res = clientLoop.serverConnect(SERVER_IP, SERVER_PORT);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
    
	/**  Android GPS Required Methods */

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		android_GPS_Lat = location.getLatitude();
		android_GPS_Lon = location.getLongitude();
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
	
	public void setSpeed(int s) {
		
		if(command != MANUAL) {
			SPEED = s;
		}
	}
	
	public void setTurn(int t) {
		if(command != MANUAL) {
			steerPercent = t;
		}
	}
	
	
	public void setDuration(int d) {
		if(command != MANUAL) {
			duration = d;
			durationMillis = (long)d*1000;
		}
	}
	
	public void setDesiredCourse(int dc) {
		if(command != MANUAL) {
			desiredCourse = dc;
		}
	}
	
	
	
	/** Unimplemented methods for Seekbar sBar_steer */
    @Override
    public void onProgressChanged(SeekBar arg0, int progress, boolean fromTouch) {
    	
    	// steerPercent = ((float) origSteerProgress - (float) progress) / (float) sBar_steer.getMax();
    }
    
    
    @Override
    public void onStartTrackingTouch(SeekBar arg0) {}
    
    @Override
    public void onStopTrackingTouch(SeekBar arg0) {
    	//origSteerProgress = sBar_steer.getProgress();
    }
    
    
    
    /**
     * Speech Recognition Methods
     */
	
	void startRepeat() {
		// prepare the TTS to repeat chosen words
		Intent checkTTSIntent = new Intent();
		// check TTS data
		checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		// start the checking Intent - will retrieve result in onActivityResult
		startActivityForResult(checkTTSIntent, VOICE_RECOGNITION_REQUEST_CODE);
	}

	/**
	 * onInit fires when TTS initializes
	 */
	public void onInit(int initStatus) {
		// if successful, set locale
		if (initStatus == TextToSpeech.SUCCESS)
			repeatTTS.setLanguage(Locale.ENGLISH);// ***choose your own locale
													// here***
	}
	/*
	public void setVoiceCMD(int cmd) {
		 = cmd;
	}
    */
}