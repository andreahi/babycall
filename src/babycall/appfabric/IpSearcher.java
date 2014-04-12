package babycall.appfabric;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Log;

class IpSearcher{
	IpSearcher(InetAddress ip, Semaphore sema, LinkedList<Socket> sockets) {
		Socket sock = null;
		try {
			sock = new Socket(ip, 13270);
			if(sock != null && sock.isBound())
				sockets.add(sock);


		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		finally{
			Log.e("debug message", "ip done: " + ip.getHostAddress());
			sema.release();
		}

	}

	IpSearcher(InetAddress ip, Semaphore sema, LinkedList<Socket> sockets, AtomicInteger count) {
		Socket sock = null;
		try {
			sock = new Socket(ip, 13270);
			if(sock != null && sock.isBound()){
				sockets.add(sock);
				sema.release();
			}


		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		finally{
			count.addAndGet(1);
			Log.e("debug message", "ip done: " + ip.getHostAddress());
			if(count.get() == 256)
				sema.release();
		}

	}
}
