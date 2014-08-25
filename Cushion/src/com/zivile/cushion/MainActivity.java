package com.zivile.cushion;


import android.os.Bundle;
import android.view.MenuItem;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {
	private final static String TAG = MainActivity.class.getSimpleName();
	private BluetoothAdapter mBluetoothAdapter;
	public static final String EXTRAS_DEVICE = "EXTRAS_DEVICE";
	private TextView tv = null;
	private String mDeviceName= "BlendMicro";
	private String mDeviceAddress;
	private final String deviceAddress = "C5:37:7B:31:4D:7D";
	
	private RBLService mBluetoothLeService;
	private Dialog mDialog;
	private Map<UUID, BluetoothGattCharacteristic> map = new HashMap<UUID, BluetoothGattCharacteristic>();
	private Charset isoDecoder = Charset.forName("ISO-8859-1");
	private View view;
	private ImageView image;
	private int integer=0;
	private final int alfa=50;
	private int isCreated=0;
	public static final String PREFS_NAME = "MyPrefsFile";
	
	private static final int REQUEST_ENABLE_BT = 1;
	private static final long SCAN_PERIOD = 1000;

	
	public static List<BluetoothDevice> mDevices = new ArrayList<BluetoothDevice>();
	private final ServiceConnection mServiceConnection = new ServiceConnection() {
	
		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			
			showDialog(MainActivity.this, R.layout.calculate);
			mBluetoothLeService = ((RBLService.LocalBinder) service)
					.getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
			// Automatically connects to the device upon successful start-up
			// initialization.
			
			
			if(mDeviceAddress!=null){
				mBluetoothLeService.connect(mDeviceAddress);
			}else{
				
				mBluetoothLeService.connect(deviceAddress);
			}
			
		
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			Log.d("onServiceDisconnected", "ComponentName "+ componentName.toString());
			mBluetoothLeService = null;
		}
	};

	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

			if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
			} else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
				getGattService(mBluetoothLeService.getSupportedGattService());
				
			} else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
				displayData(intent.getByteArrayExtra(RBLService.EXTRA_DATA));
				
			}else{
				Log.d("BroadcastReceiver onReceive", "action "+action);
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
					.show();
			Log.e(TAG, "bluetooth le not supported");
			finish();
		}

		final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Ble not supported", Toast.LENGTH_LONG)
					.show();
			finish();
			return;
		}

		if (!mBluetoothAdapter.isEnabled()) {

			
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			while(!mBluetoothAdapter.isEnabled()){
				
			}
			
		}	
		
		isCreated++;
		view = this.getWindow().getDecorView();
		image = (ImageView)this.findViewById(R.id.imageView1);

		view.setBackgroundResource(getBackground());
		image.setImageResource(getSmiley());
		image.setAlpha(alfa);

		
		tv = (TextView) findViewById(R.id.textView);
		
		tv.setMovementMethod(ScrollingMovementMethod.getInstance());

		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		scanLeDevice();
		
		Timer mTimer = new Timer();
		mTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				for(BluetoothDevice device:mDevices){
					if(device.getName().equals("BlendMicro")){
						Log.d("BlendMicroAddress",device.getAddress());
						
						mDeviceAddress=device.getAddress();
					
						mDeviceName = device.getName();

						cancel();
						
					}else{
						Log.e(TAG, "BlendMicro was not found");
					}
				}
				
			}
		}, SCAN_PERIOD);
		
			
			Intent gattServiceIntent = new Intent(MainActivity.this, RBLService.class);
			try{
			bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);	
			}catch(Exception e){
				Log.d(TAG, e.getMessage());
			}
		
		
	}

	@Override
	protected void onResume() {
		super.onResume();

		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			mBluetoothLeService.disconnect();
			mBluetoothLeService.close();

			System.exit(0);
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStop() {
		super.onStop();
		
		
		unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		
		mBluetoothLeService.disconnect();
		mBluetoothLeService.close();
		
		System.exit(0);
	}

	private void displayData(byte[] byteArray) {
		if (byteArray != null) {
			Log.d("byteArray", new String(byteArray));
			if(byteArray[0]==' '){
				Log.d(TAG, "no data sent ");
				return;
			}
			
			String data = new String(byteArray,isoDecoder);
			

			Log.d("length of string",(new Integer(data.length())).toString());
			integer = properInteger(data);
			
			if(isCreated>4 ){
				mDialog.dismiss();
				checkLimits(integer);
				int milliseconds = integer*1000;
				int seconds = (int) (milliseconds / 1000) % 60 ;
				int minutes = (int) ((milliseconds / (1000*60)) % 60);
				int hours   = (int) ((milliseconds / (1000*60*60)) % 24);
				String s = String.format("%d h, %d min, %d sec",hours,minutes,seconds);
				Log.d("proper integer", (new Integer(integer)).toString());
				tv.setText(s);				
			}
			isCreated ++;
			
			
		}
	}

	private void getGattService(BluetoothGattService gattService) {
		if (gattService == null)
			return;

		BluetoothGattCharacteristic characteristic = gattService
				.getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);
		map.put(characteristic.getUuid(), characteristic);

		BluetoothGattCharacteristic characteristicRx = gattService
				.getCharacteristic(RBLService.UUID_BLE_SHIELD_RX);
		mBluetoothLeService.setCharacteristicNotification(characteristicRx,
				true);
		mBluetoothLeService.readCharacteristic(characteristicRx);
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();

		intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);

		return intentFilter;
	}


	private int properInteger(String string){
		int length = string.length();
		int start = 0;
		String s = "";
		for(int i =1;i<=length;i=i+2){
			s=s+string.substring(start,i);
			start= start+2;
		}
		
		try{
		return Integer.parseInt(s);
		}catch(NumberFormatException e){
			return 0;
		}
	}
	private void checkLimits(int number){
		int background;
		int smiley;
		if(number<1200){
			
			background = R.drawable.backgroundgreen;
			smiley = R.drawable.tango_face_smile;
			setBackground(background, smiley);
			view.setBackgroundResource(getBackground());
			image.setImageResource(getSmiley());
			image.setAlpha(alfa);
			
		}else if(number>=1200 && number<1800){
			background = R.drawable.backgroundyellow;
			smiley = R.drawable.tango_face_plain;
			setBackground(background, smiley);
			view.setBackgroundResource(getBackground());
			image.setImageResource(getSmiley());
			image.setAlpha(alfa);
			
		}else {
			background = R.drawable.backgroundred;
			smiley = R.drawable.tango_face_sad;
			setBackground(background, smiley);
			view.setBackgroundResource(getBackground());
			image.setImageResource(getSmiley());
			image.setAlpha(alfa);
			
		}
		
	}

	private void showDialog(Context mContext,int layout){
		mDialog = new AlertDialog.Builder(mContext).create();
		mDialog.show();		
		mDialog.setContentView(layout);
		
	}
	private void scanLeDevice() {
		new Thread() {

			@Override
			public void run() {
				try{
				mBluetoothAdapter.startLeScan(mLeScanCallback);
				}catch(Exception e){
					Log.e(TAG, e.getMessage());
				}
				
				Log.d(TAG, "starting scan");
				try {
					Thread.sleep(SCAN_PERIOD);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				
			}
		}.start();
	}

	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi,
				byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (device != null) {
						if (mDevices.indexOf(device) == -1)
							mDevices.add(device);
					}
					if(mDevices.isEmpty()){
						Log.d("onLeScan", "no devices found");
					}
				}
			});
		}
	};


	private void setBackground(int i, int smiley){
		Log.d("Setting background", String.valueOf(i));
		
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("background", i);
		editor.putInt("smiley", smiley);
		editor.apply();
		
	} 
	private int getBackground(){
		
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		
		return settings.getInt("background",0x7f020000);
	}
	private int getSmiley(){
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		return settings.getInt("smiley", 0x7f020007);
	}
	
	}
