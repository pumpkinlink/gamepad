package fatecsjc.ihc.gamepad;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import fatecsjc.ihc.gamepad.util.SystemUiHider;


public class GamepadActivity extends Activity {


    class ClientThread implements Runnable {

        @Override
        public void run() {

            try {
                String ip = getIntent().getExtras().getString("ip");
                InetAddress serverAddr = InetAddress.getByName(ip);

                socket = new Socket( serverAddr, 9080);
                socket.setTcpNoDelay(true);
                System.out.println("DEBUG denisdenis "+String.valueOf(socket.isConnected()));

            }
            catch (ConnectException e1) {
                finish();
            }
            catch (UnknownHostException e1) {
                e1.printStackTrace();
                finish();
            } catch (IOException e1) {
                e1.printStackTrace();
                finish();
            }

        }

    }

    Socket socket;


    void sendKey(String key,int txtid) {
        TextView txt = (TextView) findViewById(txtid);
        txt.setText(key);

        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())),
                    true);

            out.print(key);
            out.flush();
            if(key.charAt(1)=='+') {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

                v.vibrate(40);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final int txtdebug = R.id.debug;
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gamepad);
        SystemUiHider mSystemUiHider;
        View frame = findViewById(R.id.framelayout1);

       mSystemUiHider = SystemUiHider.getInstance(this, findViewById(R.id.framelayout1)
                , SystemUiHider.FLAG_HIDE_NAVIGATION & SystemUiHider.FLAG_FULLSCREEN );
        mSystemUiHider.setup();
        mSystemUiHider.hide();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        new Thread(new ClientThread()).start();

        final ViewGroup dpadGroup=(ViewGroup)findViewById(R.id.tableLayoutDpad);
        final ViewGroup actionGroup=(ViewGroup)findViewById(R.id.tableLayoutActionButtons);
        final ViewGroup optionGroup=(ViewGroup)findViewById(R.id.linearLayoutOptionButtons);



        View.OnTouchListener dpadListener = new View.OnTouchListener() {

            Rect pressedRect;
            View pressedView;
            Integer pressedFinger=-1;
            boolean inside;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >>
                        MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                int fingerId = event.getPointerId(pointerIndex);
                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        if(fingerId==pressedFinger) {
                            View vpar = (View) v.getParent();
                            int x = v.getLeft() + (int) event.getX();
                            int y = v.getTop() + vpar.getTop() + (int) event.getY();
                            if (inside) {
                                if (!pressedRect.contains(x, y)) {
                                    sendKey(pressedView.getTag().toString() + "-", txtdebug);

                                    inside = false;
                                }
                            } else {
                                for (int i = 0; i < 3; i++) {
                                    TableRow row = (TableRow) dpadGroup.getChildAt(i);
                                    int dCount = row.getChildCount();
                                    for (int j = 0; j < dCount; j++) {
                                        View d = row.getChildAt(j);
                                        if (d instanceof ImageButton) {
                                            Rect dirRect =
                                                    new Rect(
                                                            d.getLeft(),
                                                            d.getTop() + row.getTop(),
                                                            d.getRight(),
                                                            d.getBottom() + row.getTop());
                                            if (dirRect.contains(x, y)) {
                                                inside = true;
                                                pressedView = d;
                                                pressedRect = dirRect;
                                                sendKey(pressedView.getTag().toString() + "+", txtdebug);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            return true;
                        }
                        else return false;
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        pressedFinger=fingerId;
                        inside = true;
                        pressedView=v;
                        View vpar=(View)v.getParent();
                        pressedRect = new Rect(
                                v.getLeft() ,
                                v.getTop() +    vpar.getTop(),
                                v.getRight() ,
                                v.getBottom() + vpar.getTop());
                        sendKey(pressedView.getTag().toString() + "+", txtdebug);

                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        if(fingerId==pressedFinger) {
                            if (inside) {
                                sendKey(pressedView.getTag().toString() + "-", txtdebug);
                                inside = false;
                            }
                        return true;
                        }
                        else return false;
                    default:
                        return true;
                }


            }
        };

        View.OnTouchListener actionListener=new View.OnTouchListener() {
            Integer pressedFinger = -1;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >>
                        MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                int fingerId = event.getPointerId(pointerIndex);

                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        pressedFinger=fingerId;
                        sendKey(v.getTag()+"+", txtdebug);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        if(fingerId==pressedFinger)
                        {
                            sendKey(v.getTag()+"-", txtdebug);
                            return true;
                        }
                        else return false;
                    default:
                        return true;
                }
            }
        };

        for (int i=0;i<3;i++){
            TableRow row=(TableRow)dpadGroup.getChildAt(i);
            int dCount=row.getChildCount();
            for(int j=0;j<dCount;j++) {
                View d=row.getChildAt(j);
                if (d instanceof ImageButton) {
                    d.setOnTouchListener(dpadListener);
                }
            }
        }
        for (int i=0;i<3;i++){
            TableRow row=(TableRow)actionGroup.getChildAt(i);
            int aCount=row.getChildCount();
            for(int j=0;j<aCount;j++) {
                View a=row.getChildAt(j);
                if (a instanceof ImageButton) {
                    a.setOnTouchListener(actionListener);
                }
            }
        }
        int oCount=optionGroup.getChildCount();
        for(int i=0;i<oCount;i++) {
            View o=optionGroup.getChildAt(i);
            if (o instanceof ImageButton) {
                o.setOnTouchListener(actionListener);
            }
        }

    }
}
