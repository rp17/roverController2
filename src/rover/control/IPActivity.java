package rover.control;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;



public class IPActivity extends Activity implements OnClickListener {

	EditText etIPAddress;
	Button btnIPAddress;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ipaddress);
		
		etIPAddress	= (EditText) findViewById(R.id.edittxtIPAddress);
		btnIPAddress = (Button)	findViewById(R.id.btnIPAddress);
		btnIPAddress.setOnClickListener(this);
	}


	@Override
	public void onClick(View v) { 
		
		String SERVER_IP = etIPAddress.getText().toString().trim();
		
		Intent i = new Intent(getApplicationContext(), RoverControlActivity.class);
		i.putExtra("serverIP", SERVER_IP);
		startActivity(i);
		
	}

	
	
	
	
}
