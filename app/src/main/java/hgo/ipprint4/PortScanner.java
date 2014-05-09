package hgo.ipprint4;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
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
import java.util.concurrent.TimeUnit;

/**
 * Created by hgode on 06.05.2014.
 */
public class PortScanner implements Runnable{

    private Handler mHandler=null;

    String TAG = "portscanner";
    String m_sStartIP="192.168.128.0";
    int timeout=200;

    Thread backgroundThread;

    eState state=eState.idle;

    final ExecutorService es=Executors.newFixedThreadPool(20);

    Message msg;
    Bundle bundle;
    String baseIP="192.168.128";
    int port=9100;
    boolean bValidIP=false;

    //contructor
    PortScanner(Handler handler, String startIP){
        mHandler=handler;
        m_sStartIP=startIP;
        //test(startIP);
        String[] ss = m_sStartIP.split("\\.");
        if(ss.length!=4){   //no regular IP
            state=eState.finished;
            msg = mHandler.obtainMessage(hgo.ipprint4.msgTypes.MESSAGE_TOAST);
            bundle = new Bundle();
            bundle.putString(hgo.ipprint4.msgTypes.TOAST, "inavlid IP");
            msg.setData(bundle);
            mHandler.sendMessage(msg);
            return;
        }
        bValidIP=true;
        baseIP=ss[0]+"."+ss[1]+"."+ss[2];
        state=eState.idle;
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
        @Override
        public String toString(){
            return sIP;
        }
    }

    @Override
    public void run() {
        doLog("thread: run()...");
        if(state!=eState.idle) {
            doLog("thread: run() ended as state!=idle");
            return;
        }
        state=eState.running;
        ScanResult scanResult;
        try {
            doLog("thread: starting...");
            msg=mHandler.obtainMessage(msgTypes.started);
            mHandler.sendMessage(msg);
            if( !backgroundThread.interrupted() ) {
                doLog("thread: starting discovery...");
                startDiscovery1();
                //wait for finish
                Thread.sleep(1000);
                doLog("thread: waiting for finish...");
                while(state!=state.idle) {
                    Thread.sleep(1000);
                }
                doLog("thread: wait finished");
                /*
                //scanning 250 addresses with 200ms each will take 50 seconds if done one by one!
                for (int ip1=1; ip1<=254; ip1++){
                    String sip=String.format(baseIP + ".%03d", ip1);
                    scanResult = portIsOpen1(sip, port, timeout);
                    if(scanResult.isOpen){
                        Log.i(TAG, scanResult.sIP + " 9100 open");
                        // Send the name of the connected device back to the UI Activity
                        msg = mHandler.obtainMessage(msgTypes.addHost);
                        bundle = new Bundle();
                        bundle.putString(msgTypes.HOST_NAME, scanResult.sIP);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                        doLog("added host msg for " + scanResult.sIP);
                    }
                    else{
                        Log.i(TAG, scanResult.sIP + " 9100 unavailable");
                    }
                    if(backgroundThread.interrupted())
                        break;
                }
                */
            }
            doLog("thread: interrupted");
        } catch( Exception ex ) {
            // important you respond to the InterruptedException and stop processing
            // when its thrown!  Notice this is outside the while loop.
            doLog("Thread shutting down as it was requested to stop.");
        } finally {
            backgroundThread = null;
        }
        /*
        state=eState.idle;
        msg=mHandler.obtainMessage(msgTypes.finished);
        mHandler.sendMessage(msg);
        */
        doLog("thread: run() ended");
    }

    public void startDiscovery(){
        doLog("startDiscovery");
        if(bValidIP) {
            doLog("startDiscovery: checked bValidIP");
            this.start();
        }
        doLog("startDiscovery END");
    }
    public void start() {
        doLog("start()...");
        if( backgroundThread == null ) {
            doLog("start: backgroundThread == null");
            backgroundThread = new Thread( this );
            doLog("start: starting new backgroundThread");
            backgroundThread.start();
        }
        doLog("start() END");
    }

    public void cancelDiscovery(){
        doLog("cancelDiscovery()...");
        this.stop();
        doLog("cancelDiscovery() END");
    }
    public void stop() {
        doLog("stop()...");
        cancelDiscovery1();
        if( backgroundThread != null ) {
            doLog("backgroundThread != null. trying interrupt()");
            backgroundThread.interrupt();
        }
        doLog("stop() END");
    }

    enum eState{
        idle,
        running,
        finished,
        error
    }


    ScanResult portIsOpen1(String sIp, int p, int timeout){
        ScanResult scanResult=new ScanResult(sIp, p, false);
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(sIp, p), timeout);
            socket.close();
            scanResult = new ScanResult(sIp, p, true);// true;
        } catch (Exception ex) {
            doLog("Exception in portIsOpen1 for " + sIp + "/" + p);
            //return new ScanResult(ip, port, false);// false;
        }
        return  scanResult;
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

    public void cancelDiscovery1(){
        doLog("cancelDiscovery1()...");
        if(state==eState.idle) {
            doLog("cancelDiscovery1: abort as state!=idle");
            return;
        }
        doLog("cancelDiscovery1: stutdown ExecutorService");
        es.shutdown();
        try {
            doLog("cancelDiscovery1: waiting for es.shutdown...");
            // Wait a while for existing tasks to terminate
            if (!es.awaitTermination(60, TimeUnit.SECONDS)) {
                es.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!es.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
            doLog("cancelDiscovery1: es terminated");
        }catch(InterruptedException ie){
            // (Re-)Cancel if current thread also interrupted
            doLog("cancelDiscovery1: trying es.shutdownNow...");
            es.shutdownNow();
            // Preserve interrupt status
            doLog("cancelDiscovery1: Preserve interrupt status");
            Thread.currentThread().interrupt();
        }
        doLog("cancelDiscovery1: set state=finished");
//        state=eState.finished;
        Message msg = mHandler.obtainMessage(msgTypes.finished);
        Bundle bundle = new Bundle();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        state=eState.idle;
        doLog("cancelDiscovery1: END");
    }

    public void startDiscovery1(){
        doLog("startDiscovery1()...");
//        if(state!=eState.idle)
//            return;
        doLog("startDiscovery1: setting state=running");
        state=eState.running;
        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(msgTypes.started);
        mHandler.sendMessage(msg);

        final int timeout = 200;
        final List<Future<ScanResult>> futures = new ArrayList<Future<ScanResult>>();

        //for (int port = 1; port <= 65535; port++) {
        //for (int port = 1; port <= 1024; port++) {
        //    futures.add(portIsOpen(es, ip, port, timeout));
        //}
        //
/*
        String[] ss = m_sStartIP.split("\\.");
        if(ss.length!=4){   //no regular IP
            state=eState.finished;
            msg = mHandler.obtainMessage(hgo.ipprint4.msgTypes.MESSAGE_TOAST);
            bundle = new Bundle();
            bundle.putString(hgo.ipprint4.msgTypes.TOAST, "inavlid IP");
            msg.setData(bundle);
            mHandler.sendMessage(msg);
            return;
        }
        baseIP=ss[0]+"."+ss[1]+"."+ss[2];
        int port=9100;
*/
        //start many threads for scanning
        doLog("startDiscovery1: starting futures...");
        for (int ip1=1; ip1<=254; ip1++){
            String sip=String.format(baseIP + ".%03d", ip1);
            futures.add(portIsOpen(es, sip, port, timeout));
        }
        doLog("startDiscovery1: es.shutdown()");
        es.shutdown();
        int openPorts = 0;
        doLog("startDiscovery1 getting results...");
        for (final Future<ScanResult> f : futures) {
            try {
                if (f.get().isOpen) {
                    openPorts++;
                    Log.i("portScan:", f.get().sIP + " 9100 open");
                    // Send the name of the connected device back to the UI Activity
                    msg = mHandler.obtainMessage(msgTypes.addHost);
                    bundle = new Bundle();
                    bundle.putString(msgTypes.HOST_NAME, f.get().sIP);
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);
                    doLog("added host msg for " + f.get().sIP);
                }
                else{
                    Log.i("portScan:", f.get().sIP + " 9100 closed");
                }
            }
            catch(ExecutionException e){
                doLog("ExecutionException: "+e.getMessage());
            }
            catch(InterruptedException e){
                doLog("InterruptedException: "+e.getMessage());
            }
        }
        doLog("startDiscovery1: found " + openPorts + " open ports on host " + m_sStartIP + "/24 (probed with a timeout of " + timeout + "ms)");
        msg = mHandler.obtainMessage(msgTypes.finished);
        mHandler.sendMessage(msg);
        state=eState.idle;
        doLog("startDiscovery1: END");
    }

/*
    static void test(String sIP){
        final ExecutorService es = Executors.newFixedThreadPool(20);
        final String ip = sIP;//"127.0.0.1";
        final int timeout = 200;
        final List<Future<ScanResult>> futures = new ArrayList<Future<ScanResult>>();

        //for (int port = 1; port <= 65535; port++) {
        //for (int port = 1; port <= 1024; port++) {
        //    futures.add(portIsOpen(es, ip, port, timeout));
        //}

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
*/

    void doLog(String s){
        Log.i(TAG, s);
    }
}
