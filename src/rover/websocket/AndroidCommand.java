package rover.websocket;

import java.net.URI;
import java.net.URISyntaxException;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import rover.control.RoverControlActivity;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class AndroidCommand implements ClientUpdater {
	
	volatile boolean active = true;
	private WebSocketClient mWebSocketClient;
	private RoverControlActivity sca; 
	
	public AndroidCommand(RoverControlActivity context) {
		sca = context;
	}
	public void run() {}
	
	@Override
    public boolean connectWebSocket() {
        URI uri;
        try {
            uri = new URI("ws://wildfly8websocket-calvincarter.rhcloud.com:8000/command");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return false;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                mWebSocketClient.send("Started Android Command");
            }

            @Override
            public void onMessage(String message) {
            	
            	System.out.println("Turn down");
            	
    			if(message == null) {
    				System.out.println("Received null from server");
    			}
    			else {
    		
    				String param[] = message.split(" ");
    				
    				// remote command and Speed Request
    				if(param.length == 2) {
    					
    					// set command
    					sca.command = Integer.parseInt(param[0]);
    					
    					// set speed
    					int s = Integer.parseInt(param[1]);
    					sca.setSpeed(s);					
    					
    					Message mesg = sca.handler.obtainMessage();
    					Bundle bundle = new Bundle();
    					bundle.putInt("command", sca.command);
    					bundle.putInt("speed", s);
    					mesg.setData(bundle);
    					sca.handler.sendMessage(mesg);
    					
    				}
    				// remote command, Speed, and Turn request
    				else if(param.length == 3) {
    					
    					// set command
    					sca.command = Integer.parseInt(param[0]);
    					
    					// set speed
    					int s = Integer.parseInt(param[1]);
    					sca.setSpeed(s);
    					
    					// set turn
    					int turnPercentage = Integer.parseInt(param[2]);
    					sca.setTurn(turnPercentage);
    					
    					
    					Message mesg = sca.handler.obtainMessage();
    					Bundle bundle = new Bundle();
    					bundle.putInt("command", sca.command);
    					bundle.putInt("speed", s);
    					bundle.putInt("turn", turnPercentage);
    					mesg.setData(bundle);
    					sca.handler.sendMessage(mesg);
    					
    				}
    				// remote command, Speed, and Turn request, course, duration
    				else if(param.length == 5) {
    					
    					// set command
    					sca.command = Integer.parseInt(param[0]);
    					
    					// set speed
    					int s = Integer.parseInt(param[1]);
    					sca.setSpeed(s);
    					
    					// set turn
    					int turnPercentage = Integer.parseInt(param[2]);
    					sca.setTurn(turnPercentage);
    					
    					// set course
    					int desiredCourse = Integer.parseInt(param[3]);
    					sca.setDesiredCourse(desiredCourse);
    					
    					int duration = Integer.parseInt(param[4]);
    					sca.setDuration(duration);
    					
    					Message mesg = sca.handler.obtainMessage();
    					Bundle bundle = new Bundle();
    					bundle.putInt("command", sca.command);
    					bundle.putInt("speed", s);
    					bundle.putInt("turn", turnPercentage);
    					bundle.putInt("desiredcourse", desiredCourse);
    					bundle.putInt("duration", duration);
    					mesg.setData(bundle);
    					sca.handler.sendMessage(mesg);
    					
    				}
    				// only setting to remote command
    				else if(param.length == 1) {
    					try {
    						
    						// set command
    						sca.command = Integer.parseInt(param[0]);						
    						
    						Message mesg = sca.handler.obtainMessage();
    						Bundle bundle = new Bundle();
    						bundle.putInt("command", sca.command);
    						mesg.setData(bundle);
    						sca.handler.sendMessage(mesg);
    						
    						
    					}
    					catch(NumberFormatException ex) {
    						System.out.println(ex.getMessage());
    					}
    				}				
    			}
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
            }
        };
        mWebSocketClient.connect();
        return true;
    }
    
    public void sendMessage(String message) {
    	mWebSocketClient.send(message);
    }
}

