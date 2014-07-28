package rover.pid;

import rover.autopilot.Start;


public class PIDControl {

	private static int heading_PID_timer; //Timer to calculate the dt of the PID
	private static float heading_D; //Stores the result of the derivator
	private static float heading_output; //Stores the result of the PID loop
	private static float scaler = 0.5f;
	private static int timer;

	public PIDControl() {} // Constructor

	/*
	public float constrain(float val, float max){
		if(val < -max) return -max;
		else if(val > max) return max;
		else return val;
	}
	public float kp = 0.7f;
	public float kd = 0;
	public float ki = 0;
	public float scaler = 5;
	public static final float PI = (float)Math.PI;
	protected float rc = 1/(PI*20);
	
	protected float prevError = 0;
	protected float prevDeriv = 0;
	protected float integrator = 0;
	protected float imax = 20;
	public void setFreqLPF(float freq){rc = 1/(PI*freq);}
	public void setPrevState(float prevError, float prevDeriv) {this.prevError = prevError; this.prevDeriv = prevDeriv;}
	public float pidCycle(float error) {
		float dt=((float)System.currentTimeMillis() - timer)*1.0e-3f;
		float out = 0;
		//float dtSec = dt*1.0e-3f;
		
		float dtSec = dt;
		
		out += kp*error;
		if( dt > 0) {
			if(Math.abs(kd) > 0) {
				float deriv = (error - prevError)/dtSec;
				//deriv = prevDeriv + (dtSec/(rc + dtSec))*(deriv - prevDeriv);
				prevError = error;
				prevDeriv = deriv;
				out += kd*deriv;
			}
			out *= scaler;
			if(Math.abs(ki) > 0) {
				integrator += ki*error*scaler*dtSec;
				integrator = constrain(integrator, imax);
				out += integrator;
			}
		}
		else out *= scaler;
		System.out.println("Output control = " + out);
		out = constrain(out, 90);
		timer = (int)System.currentTimeMillis();
		return out;
	}
	*/

	public int PID_heading(int PID_error)
	{ 
	  float dt=((float)System.currentTimeMillis() - heading_PID_timer)*1.0e-3f;//calculating dt, you must divide it by 1000, because this system only understands seconds.. and is normally given in millis


	  //Integrator part
	  Start.heading_I += PID_error*dt; //1000 microseconds / 1000 = 1 millisecond
	  Start.heading_I = constrain(Start.heading_I, Start.heading_min, Start.heading_max); //Limit the PID integrator... 

	  //Derivation part
	  heading_D = (PID_error - Start.heading_previous_error)/dt;

	  heading_output = 0;//Clearing the variable.	

	  heading_output = (int)(Start.kp[0]*PID_error);//Proportional part, is just the KP constant * error.. and adding to the output 
	  heading_output += (Start.ki[0]*Start.heading_I);//Adding integrator result...
	  heading_output += (Start.kd[0]*heading_D);//Adding derivator result.... 

	  //Adds all the PID results and limit the output... 
	  heading_output = constrain(heading_output,Start.heading_min,Start.heading_max);//limiting the output.... 

	  Start.heading_previous_error = PID_error;//Saving the actual error to use it later (in derivating part)...

	  heading_PID_timer = (int)System.currentTimeMillis();//Saving the last execution time, important to calculate the dt...

	  //Now checking if the user have selected normal or reverse mode (servo)... 
	  /*
	  if (Start.reverse_yaw == 1)  {
	    return (int)(-1*heading_output); 
	  }
	  else
	  {
	    return (int)(heading_output);
	  }
	  */
	  heading_output *= scaler;
	  return (int)heading_output;
	}


	public int constrain(int x, int a, int b) {

		/* 
		 	What is constrain function in arduino?
		  	reference: http://arduino.cc/en/Reference/constrain
		 
		  	x: if x is between a and b
			a: if x is less than a

			b: if x is greater than b

			Example: limits range of sensor values to between 10 and 150 
			sensVal = constrain(sensVal, 10, 150);
			
		 */

		if (x >= a && x <= b)
			return x;
		else if(x < a)
			return a;
		else if( x > b)
			return b;

		return 0;

	}

	public float constrain(float x, int a, int b) {

		/* 
		 	What is constrain function in arduino?
		  	reference: http://arduino.cc/en/Reference/constrain
		 
		  	x: if x is between a and b
			a: if x is less than a

			b: if x is greater than b

			Example: limits range of sensor values to between 10 and 150 
			sensVal = constrain(sensVal, 10, 150);
			
		 */

		if (x >= a && x <= b)
			return x;
		else if(x < a)
			return a;
		else if( x > b)
			return b;

		return 0;

	}


	/***************************************************************************/
	//Computes the heading error, and choose the shortest way to reach the desired heading
	/***************************************************************************/
	public int compass_error(int PID_set_Point, int PID_current_Point)
	{
	   float PID_error=0; //Temporary variable
	    if(Math.abs(PID_set_Point - PID_current_Point) > 180) 
		{
			if(PID_set_Point - PID_current_Point < -180)
			{
			  PID_error = (PID_set_Point + 360) - PID_current_Point;
			}
			else
			{
			  PID_error = (PID_set_Point - 360) - PID_current_Point;
			}
		}
		else
		{
	          PID_error = PID_set_Point - PID_current_Point;
	        }
		return (int) PID_error;
	}
}