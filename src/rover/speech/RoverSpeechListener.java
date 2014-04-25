package rover.speech;

import java.util.ArrayList;

import rover.control.RoverControlActivity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

public class RoverSpeechListener implements RecognitionListener {

	RoverControlActivity context = null;
	private SpeechRecognizer speech;
	private Intent intent;
	private TextToSpeech repeatTTS;
	
	public RoverSpeechListener(RoverControlActivity c, SpeechRecognizer s, Intent i, TextToSpeech tts) {
		this.context = c;
		this.speech = s;
		this.intent = i;
		this.repeatTTS = tts;
	}
	
	@Override
    public void onBeginningOfSpeech() {
            //Log.d("Speech", "onBeginningOfSpeech");
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
            //Log.d("Speech", "onBufferReceived");
    }

    @Override
    public void onEndOfSpeech() {
            //Log.d("Speech", "onEndOfSpeech");
    }

    @Override
    public void onError(int error) {
    	// If an error occurs notify user and reset listener
    	// This is usually triggered if no speech is recognized
    	Toast.makeText(context.getApplicationContext(), "Voice Recognition Error", Toast.LENGTH_SHORT).show();
    	speech.startListening(intent);
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
            //Log.d("Speech", "onEvent");
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
            //Log.d("Speech", "onPartialResults");
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
            //Log.d("Speech", "onReadyForSpeech");
    }
    

    @Override
    public void onResults(Bundle results) {
            
    		// Gather top results and store in a array list
    		ArrayList<String> words = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            
    		// Run array for each top word
    		for (int i = 0; i < words.size();i++ ) {
    			// Normalize possible word and prepare to analyze
    			String possibleWord = words.get(i).toLowerCase().trim();
            
	            if(possibleWord.equals("enable") || possibleWord.equals("on")) {
	            	
	            	// Enable Voice Command but no official command at this time
	            	RoverControlActivity.VOICESTATUS = 1;
	            	
	            	// Android echos the following:
	            	repeatTTS.speak("rover " + possibleWord, TextToSpeech.QUEUE_FLUSH, null);
	                
	                break;	// terminate loop because possible word was correct
	            }
	            else if(possibleWord.equals("go") || possibleWord.equals("forward") || possibleWord.equals("front")){
	            	
	            	//RoverControlActivity.VOICECMD = 1;
	            	this.context.command = this.context.FORWARD;
	            	break;	// terminate loop because possible word was correct
	            }
	            else if(possibleWord.equals("back") || possibleWord.equals("backward")) {
	            	this.context.command = this.context.BACKWARD;
	            	break;	// terminate loop because possible word was correct
	            }
	            else if(possibleWord.equals("left")) {
	            	this.context.command = this.context.LEFT;
	            	break;	// terminate loop because possible word was correct
	            }
	            else if(possibleWord.equals("right")){
	            	this.context.command = this.context.RIGHT;
	            	break;	// terminate loop because possible word was correct
	            }
	            else if(possibleWord.equals("stop")){
	            	this.context.command = this.context.STOP;
	            	break;	// terminate loop because possible word was correct
	            }
	            else if(possibleWord.equals("disable") || possibleWord.equals("off")) {
	            	
	            	RoverControlActivity.VOICESTATUS = 0;
	            	
	            	// Android echos the following:
	            	repeatTTS.speak("rover " + possibleWord, TextToSpeech.QUEUE_FLUSH, null);
	            	
	            	speech.stopListening();
	            	
	            	// disable speech listener for amount of time
	            	try {
						Thread.sleep(30000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	            	
	            	break;	// terminate loop because possible word was correct
	            }
	        }
            
    		
            try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  
    		
    		// Reset listener
            speech.startListening(intent);
                        
    }

    @Override
    public void onRmsChanged(float rmsdB) {
            //Log.d("Speech", "onRmsChanged");
    }

}
