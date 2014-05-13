package rover.websocket;

import java.net.URI;
import java.net.URISyntaxException;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import android.util.Log;
import android.widget.Toast;
import rover.control.RoverControlActivity;

public class AndroidUpdater implements ClientUpdater {
	
	volatile boolean active = true;
	private WebSocketClient mWebSocketClient;
	    
	public void run() {
		
		while(active) {
			int azimut = RoverControlActivity.avgAzimut;
		
			sendMessage(Integer.toString(azimut));

			try {
				Thread.sleep(10);
			}
			catch(InterruptedException ex){
				active = false;
			}
		}
	}
	
	@Override
    public boolean connectWebSocket() {
        URI uri;
        try {
            uri = new URI("ws://wildfly8websocket-calvincarter.rhcloud.com:8000/updater");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return false;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                mWebSocketClient.send("Started Android Updater");
            }

            @Override
            public void onMessage(String s) {}

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
            }
        };
        
        try{
        	mWebSocketClient.connect();
        } catch (Exception e)  {
        	return false;
        }
        
        return true;
    }
    
    public void sendMessage(String message) {
    	mWebSocketClient.send(message);
    }
}

