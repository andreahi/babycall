package babycall.appfabric;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import android.R.bool;
import android.app.backup.RestoreObserver;
import android.util.Log;

public class Communicator extends Thread{
	private ServerSocket servsock2;
	private Socket sock2;
	private boolean disconnectPressed = false;
	private boolean restartConnection;
	private boolean inAcceptState;
	private int batteryLevel = 0;

	@Override
	public void run(){
		Log.e("debug message", "in Communicator run");
		while(!disconnectPressed){
			restartConnection = false;
			Log.e("debug message", "in while run loop");
			if(initialize())
				communicateReceive();
			Log.e("debug message", "after communicatreRceive");
		}
	}
	int pingErrors = 0;
	private void ping(){
		Log.e("debug message", "ping..");
		byte data [] = {Environment.BATTERY_LEVEL,(byte) batteryLevel,0,0};
		if(communnicateSend(data) == false){
			if(pingErrors++ >10){
				restartConnection = true;			
				pingErrors = 0;
			}
		}
	}
	void setBatteyLevel(int batteryLevel){
		this.batteryLevel = batteryLevel;
	}

	private boolean initialize(){
		try {
			Log.d("debug message", "trying to setup socket2");
			int fails = 0;
			while(fails++ < 10 && !disconnectPressed){
				try{
					servsock2 = new ServerSocket();
					servsock2.setReuseAddress(true);

					servsock2.bind(new InetSocketAddress(Environment.PORT2));
					inAcceptState = true;
					sock2 = servsock2.accept();
					inAcceptState = false;
					Log.e("debug message", "accepting connection");
					sock2.setSoTimeout(1000);
					break;
				}
				catch(BindException e){
					Log.e("debug message", "bind exception");
					e.printStackTrace();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}

			}
			if(fails >= 10)
				return false;
			Log.d("debug messsage", "socket2 setup");
		} catch (IOException e) {
			e.printStackTrace();
			Log.d("debug message", "socket2 failed");
			return false;
		}
		byte [] messageToSend = {ChildActivity.MESSAGE_TYPE_VERSION, ChildActivity.VERSION[0], ChildActivity.VERSION[1], ChildActivity.VERSION[2]};
		Log.e("debg message", "Sending version data" + ChildActivity.VERSION[0] + ChildActivity.VERSION[1]+ ChildActivity.VERSION[2]);

		communnicateSend(messageToSend);
		return true;
	}
	private void communicateReceive(){


		while(true){
			int bytesRead = 0;
			byte[] buffer = new byte[Environment.COM_SIZE];
			int totalBytes = 0;
			//messages.add("template message");
			while(totalBytes < Environment.COM_SIZE){

				try {
					if(disconnectPressed || restartConnection){
						Log.d("debug message", "trying to connections closed in communicateReceive");

						byte data [] = {Environment.STATUS_UPDATE,0,0,0};
						communnicateSend(data);
						servsock2.close();
						sock2.close();
						//sock2 = null;
						//servsock2 = null;
						Log.d("debug message", "connections closed in communicateReceive");
						return;
					}
					ping();
					Log.e("debug message", "going to reaad");
					bytesRead = sock2.getInputStream().read(buffer, totalBytes, Environment.COM_SIZE-totalBytes);

					if(bytesRead == -1)
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						else
							totalBytes += bytesRead;
				} 
				catch(SocketTimeoutException  e){
					Log.e("debug message", "timeout exception");
					try {
						Thread.sleep(10);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
				catch (IOException e) {
					e.printStackTrace();
					Log.d("debug message", "IOException related to sock2");
					try {
						Log.d("debug messsage", "trying to close sock2 in catch");

						servsock2.close();
						sock2.close();
						//sock2 = null;
						//servsock2 = null;
						Log.d("debug message", "I caucht on exception so I closed sock2");
					} catch (IOException e1) {
						e1.printStackTrace();
						Log.d("debug message", "I caucht on exception in catch, this is not good");
					}
					return;
				}
			}
			Log.d("debug message", "I read a message");
			switch (buffer[0]) {
			case Environment.SEEKBAR_PROGRESS:
				byte volumeThreshold = buffer[1]; 
				Log.d("debug message", "I received a new threshold: " + buffer[1]);
				break;
			case Environment.USE_VIDEO:
				if(buffer[1] == 1)
					Preview.usevideo = true;
				else
					Preview.usevideo = false;
				break;
			case Environment.USE_HD_VIDEO:
				if(buffer[1] == 1)
					Preview.usehd = true;
				else
					Preview.usehd = false;
				break;

			default:
				break;
			}
			//messages.add(new String(buffer));
			bytesRead = 0;
			totalBytes = 0;
		}
	}
	synchronized  boolean communnicateSend(byte [] message){
		if(message.length > Environment.COM_SIZE){
			Log.d("debug message", "message was larger than COM_SIZE");
			return false;
		}
		try {
			while(sock2 == null){
				Thread.sleep(100);
				if (disconnectPressed) {
					return false;
				}
			}

			Log.d("debug message", "i wrote a  message");
			try {
				sock2.getOutputStream().write(message);
				sock2.getOutputStream().flush();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				try {
					sock2.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				e.printStackTrace();
				return false;
			}

		} catch (InterruptedException e) {
			//e.printStackTrace();
		}
		return true;
	}
	void setDisconnectPressed(){
		disconnectPressed = true;
		try {
			servsock2.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		if(inAcceptState){

		}
		Log.e("debug message", "setDisconnectPressed");
	}
}
