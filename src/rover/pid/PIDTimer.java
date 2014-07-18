package rover.pid;

public class PIDTimer {
	public PIDTimer(){}
	
	private boolean started = false;
	private long startTime;
		
	public synchronized void startTimer() {
		if(!started){
			started = true;
			startTime = System.currentTimeMillis();
		} 
	}
	public synchronized void clear(){
		started = false;
	}
	
	public synchronized long getTime(){
		return System.currentTimeMillis() - startTime;
	}
}
