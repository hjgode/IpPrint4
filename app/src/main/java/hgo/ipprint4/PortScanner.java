package hgo.ipprint4;

import android.util.Log;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by hgode on 06.05.2014.
 */
public class PortScanner {

    PortScanner(String startIP){
        test(startIP);
    }

    public static class ScanResult {
        private int port=0;
        private boolean isOpen=false;
        private String sIP="";
        // constructor
        ScanResult(String s, int p, boolean b){
            sIP=s;
            port=p;
            isOpen=b;
        }
        // getters
        public ScanResult get(){
            return this;
        }

    }

    public static Future<ScanResult> portIsOpen(final ExecutorService es, final String ip, final int port, final int timeout) {
        return es.submit(new Callable<ScanResult>() {
            @Override public ScanResult call() {
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(ip, port), timeout);
                    socket.close();
                    return new ScanResult(ip, port, true);// true;
                } catch (Exception ex) {
                    return new ScanResult(ip, port, false);// false;
                }
            }
        });
    }

    static void test(String sIP){
        final ExecutorService es = Executors.newFixedThreadPool(20);
        final String ip = sIP;//"127.0.0.1";
        final int timeout = 200;
        final List<Future<ScanResult>> futures = new ArrayList<Future<ScanResult>>();
        /*
        //for (int port = 1; port <= 65535; port++) {
        for (int port = 1; port <= 1024; port++) {
            futures.add(portIsOpen(es, ip, port, timeout));
        }
        */
        int port=9100;
        for (int ip1=1; ip1<=254; ip1++){
            String sip=String.format("192.168.128.%03d", ip1);
            futures.add(portIsOpen(es, sip, port, timeout));
        }
        es.shutdown();
        int openPorts = 0;
        for (final Future<ScanResult> f : futures) {
            try {
                if (f.get().isOpen) {
                    openPorts++;
                    Log.i("portScan:", f.get().sIP + " 9100 open");
                }
                else{
                    Log.i("portScan:", f.get().sIP + " 9100 closed");
                }
            }
            catch(ExecutionException e){
                ;
            }
            catch(InterruptedException e){
                ;
            }
        }
        System.out.println("There are " + openPorts + " open ports on host " + ip + " (probed with a timeout of " + timeout + "ms)");

    }
}
