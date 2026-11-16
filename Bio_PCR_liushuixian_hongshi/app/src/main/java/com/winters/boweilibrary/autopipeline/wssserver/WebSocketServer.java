package com.winters.boweilibrary.autopipeline.wssserver;

import android.util.Log;

import com.winters.boweilibrary.autopipeline.enumtype.EnumMessageEvent;
import com.winters.boweilibrary.autopipeline.model.item.WSSConnectionItem;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.PCRLogMessage;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.SLAN_PCRFinishMessage;
import com.winters.boweilibrary.autopipeline.model.message.RNAMessage.RNALogMessage;
import com.winters.boweilibrary.autopipeline.utils.IPToolsUtils;
import com.winters.boweilibrary.autopipeline.model.event.WSSMessageEvent;

import org.greenrobot.eventbus.EventBus;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebSocketServer extends org.java_websocket.server.WebSocketServer{

    private static WebSocketServer websocketServer;
    
    // 单线程线程池，保证消息按顺序处理
    private static final ExecutorService messageExecutor = Executors.newSingleThreadExecutor();

    WebSocketServer(InetSocketAddress host){
        super(host);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Log.i("WebSocketServer","onOpen():连接到: "+getRemoteSocketAddress(conn));
        EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSS_Connect, new WSSConnectionItem(conn),"onOpen:"+getRemoteSocketAddress(conn)));
    }
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Log.i("WebSocketServer","onClose" + reason + getRemoteSocketAddress(conn));
        EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSS_Disconnect, new WSSConnectionItem(conn),"onClose:"+reason));
    }

    @Override
    public void onClosing(WebSocket conn, int code, String reason, boolean remote) {
        super.onClosing(conn, code, reason, remote);
        Log.i("WebSocketServer","onClose" + reason + getRemoteSocketAddress(conn));
        EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSS_Disconnect, new WSSConnectionItem(conn),"onClose:"+reason));
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
//        Log.i("WebSocketServer","onMessage"+message);
        // 使用单线程线程池，保证消息顺序处理
        messageExecutor.execute(() -> {
            EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSS_Message, new WSSConnectionItem(conn), message));
        });
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        Log.d("WebSocketServer","onMessage"+message);
        // 复制 ByteBuffer，因为原始 buffer 可能会被复用
        ByteBuffer msgCopy = ByteBuffer.allocate(message.remaining());
        msgCopy.put(message);
        msgCopy.flip();
        
        // 使用单线程线程池，保证消息顺序处理
        messageExecutor.execute(() -> {
            try {
                com.winters.boweilibrary.autopipeline.model.message.BinBaseMessage binMsg =
                        com.winters.boweilibrary.autopipeline.utils.BinWssMessageUtils.parseBinMessage(msgCopy);
                if (binMsg != null) {
                    Log.i("WebSocketServer", "BinBaseMessage parsed: Type=" + binMsg.getType());
                    if (binMsg instanceof SLAN_PCRFinishMessage) {
                        Log.i("WebSocketServer", "Received SLAN_RNACurveMessage");
                        EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSS_BinMessage, new WSSConnectionItem(conn), binMsg));
                    } else if (binMsg instanceof PCRLogMessage) {
                        Log.i("WebSocketServer", "Received BinBaseMessage PCRLogMessage");
                        EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSS_BinMessage, new WSSConnectionItem(conn), binMsg));
                    } else if (binMsg instanceof RNALogMessage) {
                        Log.i("WebSocketServer", "Received BinBaseMessage RNALogMessage");
                        EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSS_BinMessage, new WSSConnectionItem(conn), binMsg));
                    }
                }
            } catch (Exception e) {
                Log.e("WebSocketServer", "Error processing binary message: " + e.getMessage());
            }
        });
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        Log.e("WebSocketServer","onError123"+ex.getMessage());
        String map = "unknown reason";
        if(conn == null){
            Log.i("WebSocketServer", "onError message:"+ "--" + map);
            EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSS_Error, null, "onError:"+ex.getMessage()));

        }else {
            InetSocketAddress inetSocketAddress = conn.getRemoteSocketAddress();
            map = inetSocketAddress.getAddress() + ":" + inetSocketAddress.getPort();
            Log.i("WebSocketServer", "onError message:" + "--" + map);
            EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSS_Error, null, "onError:" + ex.getMessage()));
        }
    }

    @Override
    public void onStart() {
        Log.i("WebSocketServer","onStart:"+websocketServer.getAddress());
        EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSS_ServerStart, null,"onStart:服务器已就绪"+websocketServer.getAddress()));
    }

    public static boolean ready(){
        try {
            String ip = IPToolsUtils.getHostIp();
            InetSocketAddress myHost = new InetSocketAddress(ip, 8080);
            websocketServer = new WebSocketServer(myHost);
            websocketServer.setReuseAddr(true);//解决端口占用问题
            websocketServer.start();
        } catch (Exception e) {
            Log.e("WebSocketServer","onStart:" + e.getMessage());
            return  false;
        }
        WebSocketServer.websocketServer = websocketServer;
        return  true;
    }

    public static void Stop() {
        try {
            websocketServer.stop();
            // 关闭线程池
            messageExecutor.shutdown();
        } catch (Exception e) {
            Log.e("stopWSSserver","stopWSSserver:" + e.getMessage());
        }
    }

    public static void Send(String string) {
        try {
            websocketServer.broadcast(string);
            //websocketServer.broadcast(string,clients);//Collection<WebSocket> clients,指定发送到哪个客户端
        } catch (Exception e) {
            Log.e("sendWSSmessage","sendWSSmessage:" + e.getMessage());
            e.printStackTrace();
        }
    }


}