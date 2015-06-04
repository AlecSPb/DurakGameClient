package com.yan.durak.communication.game_server.connector;

import com.yan.durak.gamelogic.communication.protocol.BaseProtocolMessage;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yan-Home on 1/25/2015.
 * <p/>
 * This class is responsible to handle communication between server and client in an easy manner.
 */
public class RemoteGameServerConnector extends BaseGameServerConnector {

//        private static final String SERVER_ADDRESS = "192.168.1.101";
        private static final String SERVER_ADDRESS = "yan-durak-server-lobby-staging.herokuapp.com";
//    private static final int SERVER_PORT = 1314;
//    private static final int SERVER_PORT = 5000;
    private static final int SERVER_PORT = 80;

    public RemoteGameServerConnector() {
        super();
    }

    @Override
    public void connect() {
        //opening new thread to do network operation off the GL thread
        (new Thread(new Runnable() {
            @Override
            public void run() {
                String domain = SERVER_ADDRESS;

                //obtain socket adress
                String socketRelativeAdress = requestSocketAdress("http://" + domain);

                //connect to socket adress
                SocketConnectionManager.getInstance().connectToRemoteServerViaWebSocket(SERVER_ADDRESS + socketRelativeAdress, -1);
//                SocketConnectionManager.getInstance().connectToRemoteServerViaSocket( socketDomain, socketPort);


//                SocketConnectionManager.getInstance().connectToRemoteServerViaSocket("http://" + "yan-durak-server-lobby-staging.herokuapp.com:" +splitString[1]  /*splitString[0]*/,-1 /*Integer.parseInt(splitString[1])*/);
//                SocketConnectionManager.getInstance().connectToRemoteServerViaSocket("http://" + socketAdress  /*splitString[0]*/,-1 /*Integer.parseInt(splitString[1])*/);
//                SocketConnectionManager.getInstance().connectToRemoteServerViaSocket("http://yan-durak-server-lobby-staging.herokuapp.com:8080", 1234);
            }
        })).start();
    }

    private String requestSocketAdress(String adress) {
        /*
         * Create the POST request
         */
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(adress);
        // Request parameters and other properties.
        List<NameValuePair> params = new ArrayList<>();

        //TODO : make it dynamic
//        params.add(new BasicNameValuePair("payload", "{gameType:\"ONE_PLAYER_TWO_BOTS\",userId:\"s\"}"));
        params.add(new BasicNameValuePair("payload", "{gameType:\"TWO_PLAYERS_ONE_BOT\",userId:\"s\"}"));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // writing error to Log
            e.printStackTrace();
        }
        /*
         * Execute the HTTP Request
         */
        try {
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity respEntity = response.getEntity();

            if (respEntity != null) {
                // EntityUtils to get the response content
                String content = EntityUtils.toString(respEntity);
                return content;
            }
        } catch (ClientProtocolException e) {
            // writing exception to log
            e.printStackTrace();
        } catch (IOException e) {
            // writing exception to log
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void disconnect() {
        SocketConnectionManager.getInstance().disconnectFromRemoteServer();
    }

    @Override
    public void sentMessageToServer(BaseProtocolMessage message) {
        SocketConnectionManager.getInstance().sendMessageToRemoteServer(message.toJsonString());
    }
}
