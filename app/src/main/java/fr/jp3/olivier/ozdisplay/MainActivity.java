package fr.jp3.olivier.ozdisplay;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    private AverageQueue tws10mn = new AverageQueue(600);
    private AverageQueue twd10mn = new AverageQueue(600);

    private static final int MAX_UDP_DATAGRAM_LEN = 1500;
    private static int UDP_SERVER_PORT;

    private TextView hdg, drift, stddev, sats;
    private TextView[] tv = new TextView[11];
    private TextView[] tvLabel = new TextView[11];
    private TextView[] tvRight = new TextView[11];
    private String[] tvVar = new String[11];
    private static String mhdg, mdrift, msats, mstddev, mtwsavg10, mtwdavg10;
    private static float mset, mtwa, mawa, mcog, mhdgf, mcapwp, mdopt, mtwd;
    Map<String,String> map = new HashMap<>();

    Timer timer;
    TimerTask timerTask;
    //we are going to use a handler to be able to run in our TimerTask
    final Handler handler = new Handler();
    SharedPreferences SP;

    // Variables laylines
    private float leftLLangle, rightLLangle, minLeftLLangle, maxLeftLLangle, minRightLLangle, maxRightLLangle;

    // define the display assembly compass picture
    private ImageView imageCompas, imageCOG, imageSET, imageWP, imageTWA, imageAWA, imageT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        UDP_SERVER_PORT = Integer.parseInt(SP.getString("udpPort", "10110"));
        boolean bAwake = SP.getBoolean("disableSleep", false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UDPReceiver udpReceiver = new UDPReceiver();
        udpReceiver.start();

        if (bAwake) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        imageCompas = (ImageView) findViewById(R.id.ivCompas);
        imageCOG    = (ImageView) findViewById(R.id.ivCOG);
        imageSET    = (ImageView) findViewById(R.id.ivSET);
        imageWP     = (ImageView) findViewById(R.id.ivWP);
        imageTWA    = (ImageView) findViewById(R.id.ivTWA);
        imageAWA    = (ImageView) findViewById(R.id.ivAWA);
        imageT      = (ImageView) findViewById(R.id.ivT);

        hdg         = (TextView) findViewById(R.id.tvHDG);
        drift       = (TextView) findViewById(R.id.tvDrift);
        stddev      = (TextView) findViewById(R.id.tvStdDev);
        sats        = (TextView) findViewById(R.id.tvSats);

        startTimer();
        initialiseCases();
    }

/*
    @Override
    protected void onResume() {
        super.onResume();

        //onResume we start our timer so it can start when the app comes from the background
        startTimer(); // Soucis %cpu retour veille
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!bKeepNmea) stoptimertask();
    }
*/

    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 1000ms the TimerTask will run every 1000ms
        timer.schedule(timerTask, 1000, 1000);
    }

/*
    public void stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
*/

    public void initialiseCases() {

        String[] parts;

        for(int i = 1; i < 11; i++) {
            tv[i] = new TextView(this);
            tvLabel[i] = new TextView(this);
            tvRight[i] = new TextView(this);
        }

        tv[1]     = (TextView) findViewById(R.id.tv1);
        tv[2]     = (TextView) findViewById(R.id.tv2);
        tv[3]     = (TextView) findViewById(R.id.tv3);
        tv[4]     = (TextView) findViewById(R.id.tv4);
        tv[5]     = (TextView) findViewById(R.id.tv5);
        tv[6]     = (TextView) findViewById(R.id.tv6);
        tv[7]     = (TextView) findViewById(R.id.tv7);
        tv[8]     = (TextView) findViewById(R.id.tv8);
        tv[9]     = (TextView) findViewById(R.id.tv9);
        tv[10]    = (TextView) findViewById(R.id.tv10);

        tvLabel[1]     = (TextView) findViewById(R.id.tvlabel1);
        tvLabel[2]     = (TextView) findViewById(R.id.tvlabel2);
        tvLabel[3]     = (TextView) findViewById(R.id.tvlabel3);
        tvLabel[4]     = (TextView) findViewById(R.id.tvlabel4);
        tvLabel[5]     = (TextView) findViewById(R.id.tvlabel5);
        tvLabel[6]     = (TextView) findViewById(R.id.tvlabel6);
        tvLabel[7]     = (TextView) findViewById(R.id.tvlabel7);
        tvLabel[8]     = (TextView) findViewById(R.id.tvlabel8);
        tvLabel[9]     = (TextView) findViewById(R.id.tvlabel9);
        tvLabel[10]    = (TextView) findViewById(R.id.tvlabel10);

        tvRight[1]     = (TextView) findViewById(R.id.tvright1);
        tvRight[2]     = (TextView) findViewById(R.id.tvright2);
        tvRight[3]     = (TextView) findViewById(R.id.tvright3);
        tvRight[4]     = (TextView) findViewById(R.id.tvright4);
        tvRight[5]     = (TextView) findViewById(R.id.tvright5);
        tvRight[6]     = (TextView) findViewById(R.id.tvright6);
        tvRight[7]     = (TextView) findViewById(R.id.tvright7);
        tvRight[8]     = (TextView) findViewById(R.id.tvright8);
        tvRight[9]     = (TextView) findViewById(R.id.tvright9);
        tvRight[10]    = (TextView) findViewById(R.id.tvright10);

        parts = SP.getString("1", "BOAT SPEED;kn;mbsp").split(";");
        tvLabel[1].setText(parts[0]); tvRight[1].setText(parts[1]); tvVar[1] = parts[2];
        parts = SP.getString("2", "TARGET SPD;kn;mcible").split(";");
        tvLabel[2].setText(parts[0]); tvRight[2].setText(parts[1]); tvVar[2] = parts[2];
        parts = SP.getString("3", "OPTIMUM VMG;%;mpopt").split(";");
        tvLabel[3].setText(parts[0]); tvRight[3].setText(parts[1]); tvVar[3] = parts[2];
        parts = SP.getString("4", "OPTIMUM VMG;°;mdopt").split(";");
        tvLabel[4].setText(parts[0]); tvRight[4].setText(parts[1]); tvVar[4] = parts[2];
        parts = SP.getString("5", "TODAY LOCH;nm;mdayloch").split(";");
        tvLabel[5].setText(parts[0]); tvRight[5].setText(parts[1]); tvVar[5] = parts[2];
        parts = SP.getString("6", "SOG;kn;msog").split(";");
        tvLabel[6].setText(parts[0]); tvRight[6].setText(parts[1]); tvVar[6] = parts[2];
        parts = SP.getString("7", "TWS;kn;mtws").split(";");
        tvLabel[7].setText(parts[0]); tvRight[7].setText(parts[1]); tvVar[7] = parts[2];
        parts = SP.getString("8", "AWS;kn;maws").split(";");
        tvLabel[8].setText(parts[0]); tvRight[8].setText(parts[1]); tvVar[8] = parts[2];
        parts = SP.getString("9", "HEEL;°;mheel").split(";");
        tvLabel[9].setText(parts[0]); tvRight[9].setText(parts[1]); tvVar[9] = parts[2];
        parts = SP.getString("10", "DEPTH;m;mdepth").split(";");
        tvLabel[10].setText(parts[0]); tvRight[10].setText(parts[1]); tvVar[10] = parts[2];

        for(int i = 1; i < 11; i++) {
            map.put(tvVar[i], "---");
        }
    }

    public void initializeTimerTask() {

        timerTask = new TimerTask() {
            public void run() {

                test();

                // Mise à jour des informations à l'écran
                handler.post(new Runnable() {
                    public void run() {
                        screenUpdate();
                        if(hdg != null) hdg.setText(mhdg);
                        if(drift != null) drift.setText(mdrift);
                        if(sats != null) sats.setText(msats);
                        if(stddev != null) stddev.setText(mstddev);
                        for(int i = 1; i < 11; i++) {
                            if (tv[i] != null) tv[i].setText(map.get(tvVar[i]));
                        }
                    }
                });
            }
        };
    }

    // A virer avec test() quand ça sera fini
    private String roundTwoDecimals(float f) {
        DecimalFormatSymbols point = new DecimalFormatSymbols();
        point.setDecimalSeparator('.');
        DecimalFormat twoDForm = new DecimalFormat("0.00", point);
        return twoDForm.format(f);
    }

    private void test() {
        map.put("mbsp", "4.23");
        map.put("mpopt", "103");
        map.put("mcible", "4.14");
        map.put("msog", "4.35");
        map.put("mtws", roundTwoDecimals(Float.parseFloat("08.0")));
        map.put("maws", roundTwoDecimals(Float.parseFloat("04.1")));
        map.put("mheel", "3");
        map.put("mdepth", roundTwoDecimals(Float.parseFloat("004.8")));
        map.put("mdayloch", roundTwoDecimals(Float.parseFloat("006.1")));
        msats = "12 sats";
        mstddev = "1.20 m";
        UpdateCellColor(stddev, Color.GREEN);
        UpdateCellColor(sats, Color.GREEN);
        mcog = 50;
        mhdgf = 53;
        mhdg = String.valueOf((int)mhdgf);
        mdopt = 163;
        map.put("mdopt", String.valueOf((int)mdopt));
        mtwa = 163;
        map.put("mtwa", String.valueOf((int)mtwa));
        mtwd = mhdgf + mtwa;
        if (mtwd >= 360) mtwd -= 360;
        map.put("mtwd", String.valueOf((int)mtwd));
        mcapwp = 43;
        mset = 0;
        mdrift = "0.2";
        mawa = 146;
        map.put("mawa", String.valueOf((int)mawa));
        twd10mn.update(String.valueOf(mtwd - 7));
        twd10mn.update(String.valueOf(mtwd + 8));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode){
            case KeyEvent.KEYCODE_MENU:
                Intent i = new Intent(this, UserSettingActivity.class);
                startActivity(i);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void screenUpdate() {
        rotate(imageCompas, -mhdgf);
        rotate(imageCOG, mcog - mhdgf);
        rotate(imageSET, mset - mhdgf);
        rotate(imageWP, mcapwp - mhdgf);
        rotate(imageTWA, mtwa);
        rotate(imageT, mtwa);
        rotate(imageAWA, mawa);

        // Colorisation TWA
        setTWAcolor();

        // Dessin des laylines
        twd10mn.calculMinMax();
        leftLLangle = mtwd + mdopt - mhdgf;
        rightLLangle = mtwd - mdopt - mhdgf;
        minLeftLLangle = twd10mn.getMin() + mdopt - mhdgf;
        maxLeftLLangle = twd10mn.getMax() + mdopt - mhdgf;
        minRightLLangle = twd10mn.getMin() - mdopt - mhdgf;
        maxRightLLangle = twd10mn.getMax() - mdopt - mhdgf;
        View llv = new LaylineView(getApplicationContext());
        Bitmap bitmap = Bitmap.createBitmap(720, 720, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        llv.draw(canvas);
        ImageView iv = (ImageView) findViewById(R.id.ivLL);
        iv.setImageBitmap(bitmap);
    }

    public void rotate(ImageView iv,  Float bearing){
        if(bearing != null) {
            RotateAnimation ra = new RotateAnimation(bearing, bearing,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            ra.setFillAfter(true); //make the arrow stay at its destination (after rotation)
            ra.setDuration(800);
            iv.startAnimation(ra);
        }
    }

    public void setTWAcolor() {
        int twa, offset;
        String teinte = "#00ff00";
        if (mtwa > 180) twa = (int) (360 - mtwa);
        else twa = (int) mtwa;
        if (mdopt < 90.0 ) offset = (int) (twa - mdopt);
        else offset = (int) (mdopt - twa);

        if      (offset <= -5) teinte = "#ff0000"; // rouge
        else if (offset == -4) teinte = "#cc3300";
        else if (offset == -3) teinte = "#996600";
        else if (offset == -2) teinte = "#669900";
        else if (offset == -1) teinte = "#33cc00";
        else if (offset ==  0) teinte = "#00ff00"; // vert
        else if (offset ==  1) teinte = "#00e51a";
        else if (offset ==  2) teinte = "#00cc33";
        else if (offset ==  3) teinte = "#00b24d";
        else if (offset ==  4) teinte = "#009966";
        else if (offset ==  5) teinte = "#007f80";
        else if (offset ==  6) teinte = "#006699";
        else if (offset ==  7) teinte = "#004db2";
        else if (offset ==  8) teinte = "#0033cc";
        else if (offset ==  9) teinte = "#001ae5";
        else if (offset >= 10) teinte = "#0000ff"; // bleu

        imageTWA.setImageTintList(ColorStateList.valueOf(Color.parseColor(teinte)));
    }

    public class LaylineView extends View {
        Path path;
        Paint pBackground;
        Paint paint;
        RectF oval;
        float sweep;
        float width;
        float height;
        float radius;

        public LaylineView(Context context) {
            super(context);
            init();
        }

        private void init() {
            path = new Path();
            pBackground = new Paint();
            paint = new Paint();
            oval = new RectF();
            width = (float)getWidth();
            height = (float)getHeight();
            radius = 360;

            pBackground.setColor(Color.TRANSPARENT);
            paint.setStrokeWidth(5);
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            path.addCircle(width/2, height/2, radius, Path.Direction.CW);
            canvas.drawRect(0, 0, width, height, pBackground);
            oval.set(0, 0, radius * 2, radius * 2);

            // Moyenne babord
            paint.setColor(Color.RED);
            paint.setAlpha(0xAA);
            sweep = maxLeftLLangle - minLeftLLangle;
            if (sweep < 0 ) sweep += 360;
            canvas.drawArc(oval, minLeftLLangle - 90, sweep, true, paint);

            // Layline babord
            paint.setAlpha(0xFF);
            canvas.drawArc(oval, leftLLangle - 90.5f, 1, true, paint);

            // Moyenne tribord
            paint.setColor(Color.GREEN);
            paint.setAlpha(0xAA);
            sweep = maxRightLLangle - minRightLLangle;
            if (sweep < 0 ) sweep += 360;
            canvas.drawArc(oval, minRightLLangle - 90, sweep, true, paint);

            // Layline tribord
            paint.setAlpha(0xFF);
            canvas.drawArc(oval, rightLLangle -90.5f, 1, true, paint);
        }
    }

    // Implémente une moyenne mobile sur un échantillon donné
    // Calcule les valeurs min et max de l'échantillon
    private class AverageQueue {
        float total = 0;
        float avg = 0;
        float min = 0;
        float max = 0;
        int capa = 0;
        Queue q;


        private AverageQueue(int Capacity) {
            capa = Capacity;
            q = new ArrayDeque(Capacity);
        }

        // Attend une string contenant le dernier échantillon
        // Retourne un float contenant la moyenne mise à jour
        private float update(String v) {
            Float f = Float.parseFloat(v);

            // Si on a atteint la capacité maximale on commence à faire glisser la moyenne
            if (q.size() > capa) {
                total -= (float) q.poll();
            }
            q.add(f);
            total += f;
            avg = total / q.size();
            return avg;
        }

        private void calculMinMax() {
            min = 359; // Sinon on ne trouve pas le plus petit qui peut être 359
            max = 0;
            float current;
            for (Iterator<Float> i = q.iterator(); i.hasNext();) {
                current = i.next();
                if (current < min) min = current;
                else if (current > max) max = current;
            }
        }

        private float getMin() {
            return min;
        }

        private float getMax() {
            return max;
        }
    }

    public void UpdateCellColor(final TextView t, final int i) {
        runOnUiThread(new Runnable() {
            @Override
            public void run(){
                if(t != null) t.setTextColor(i);
            }
        });
    }

    private class UDPReceiver extends Thread {
        private boolean bKeepRunning = true;
        private String lastMessage = "";
        //private InetAddress server_addr;
        private DatagramSocket socket;

        public void run() {
            String message;
            byte[] lmessage = new byte[MAX_UDP_DATAGRAM_LEN];
            DatagramPacket packet = new DatagramPacket(lmessage, lmessage.length);

/*
            try {
                server_addr = InetAddress.getByName(UDP_SERVER_IP);
            } catch (UnknownHostException e) {
            }
*/

            try {
                socket = new DatagramSocket(UDP_SERVER_PORT);

                while(bKeepRunning) {
                    socket.receive(packet);
                    message = new String(lmessage, 0, packet.getLength());
                    lastMessage = message;
                    parseNMEA(lastMessage);
                }

            } catch (Throwable e) {
                e.printStackTrace();
            }

            if (socket != null) {
                socket.close();
            }
        }

/*
        public void kill() {
            bKeepRunning = false;
        }
*/

        // Traitement d'un paquet UDP reçu
        private void parseNMEA(String str) {

            int GPSColor = Color.WHITE;

            // Découpe un paquet UDP en phrases uniques
            List<String> sentences = Arrays.asList(str.split("\\r?\\n"));

            for (int i = 0; i < sentences.size(); i++) {

                // Supprime * et checksum
                String line = sentences.get(i).substring(0, sentences.get(i).indexOf('*'));

                // Découpe en valeurs dans un array
                // items.get(0) contient l'identifiant de la phrase ($-----)
                final List<String> items = Arrays.asList(line.split("\\s*,\\s*"));
                String ID = items.get(0).substring(3, 6);

                switch (ID) {
                    case "HDM":
                        mhdg = ent(items.get(1));
                        mhdgf = str2float(items.get(1));
                        break;

                    case "VHW":
                        map.put("mbsp", items.get(5));
                        break;

                    case "VLW":
                        map.put("mtotalloch", ent(items.get(1)));
                        map.put("mdayloch", roundTwoDecimals(Float.parseFloat(items.get(3))));
                        break;

                    case "MWV":
                        final String wss = roundTwoDecimals(Float.parseFloat(items.get(3)));
                        if (Objects.equals(items.get(2), "T")) {
                            final float wss10mn = tws10mn.update(wss);
                            mtwa = str2float(items.get(1));
                            map.put("mtwa", getwa(items.get(1)));
                            map.put("mtws", wss);
                            mtwsavg10 = roundTwoDecimals(wss10mn);
                        } else if (Objects.equals(items.get(2), "R")) {
                            mawa = str2float(items.get(1));
                            map.put("mawa", getwa(items.get(1)));
                            map.put("maws", wss);
                        }
                        break;

                    case "MWD":
                        final String twds = items.get(3);
                        final float twds10mn = twd10mn.update(twds);
                        map.put("mtwd", ent(twds));
                        mtwd = str2float(items.get(3));
                        mtwdavg10 = ent(String.valueOf(twds10mn));
                        break;

                    case "XDR":
                        if (Objects.equals(items.get(4), "Heel")) {
                            map.put("mheel", abs(ent(items.get(2)))) ;
                        } else if (Objects.equals(items.get(4), "Pitch")) {
                            map.put("mpitch", abs(ent(items.get(2))));
                        } else if (Objects.equals(items.get(4), "AirTemp")) {
                            map.put("mairtemp", items.get(2));
                        } else if (Objects.equals(items.get(4), "Barometer")) {
                            map.put("mbaro", bar2mbar(items.get(2)));
                        }
                        break;

                    case "RMC":// Fix valide = A
                        if (Objects.equals(items.get(2), "A")) {
                            mcog = str2float(items.get(8));
                            map.put("msog", items.get(7));
                            map.put("mcog", ent(items.get(8)));
                        }
                        break;

                    case "GGA":// Qualité ) 1 = GPS (blanc), 2 = DGPS (vert)
                        msats = items.get(7) + " sats";
                        if (Objects.equals(items.get(6), "1")) {
                            if (GPSColor == Color.GREEN) {
                                UpdateCellColor(stddev, Color.WHITE);
                                UpdateCellColor(sats, Color.WHITE);
                                GPSColor = Color.WHITE;
                            }
                        } else if (Objects.equals(items.get(6), "2")) {
                            if (GPSColor == Color.WHITE) {
                                UpdateCellColor(stddev, Color.GREEN);
                                UpdateCellColor(sats, Color.GREEN);
                                GPSColor = Color.GREEN;
                            }
                        }
                        break;

                    case "VDR":
                        mdrift = items.get(5);
                        mset = str2float(items.get(3));
                        break;

                    case "MTW":
                        map.put("mwatertemp", items.get(1));
                        break;

                    case "DPT":
                        map.put("mdepth", roundTwoDecimals(Float.parseFloat(items.get(1))));
                        break;

                    case "ZPE":
                        map.put("mcible", items.get(1));
                        map.put("mcapab", ent(items.get(2)));
                        map.put("mdopt", items.get(3));
                        mdopt = str2float(items.get(3));
                        final int opt = Integer.parseInt(items.get(3));
                        if (opt < 90)
                            map.put("mpopt", items.get(4));
                        else if (opt >= 90)
                            map.put("mpopt", items.get(5));
                        break;

                    case "RMB":
                        map.put("mdistwp", items.get(10));
                        map.put("mcapwp", ent(items.get(11)));
                        mcapwp = str2float(items.get(11));
                        map.put("mvmc", items.get(12));
                        map.put("mnomwp", "CAP " + items.get(4).substring(0, 4));
                        break;

                    case "GST":
                        Double lat = Double.parseDouble(items.get(6));
                        Double lon = Double.parseDouble(items.get(7));
                        mstddev = roundTwoDecimals((float) Math.sqrt(lat*lat + lon*lon)) + " m";
                        break;

                    default:
                        break;
                }
            }
        }

        // Retourne bars décimaux en mbar entiers
        private String bar2mbar(String v) {
            Double d = str2double(v)*1000;
            return String.valueOf(Math.round(d));
        }

        // Calcule le TWA ou AWA. Retourne une String entière
        private String getwa(String v) {
            if (str2double(v) < 180) return ent(v) + "<";
            else return ">" + String.valueOf(Math.round(360 - str2double(v)));
        }

        // Retourne une String entière en double
        private double str2double(String v) {
            return Double.parseDouble(v);
        }

        // Retourne une String entière en float
        private float str2float(String v) {
            return Float.parseFloat(v);
        }

        // Retourne une String entière
        private String ent(String v) {
            return String.valueOf(Math.round(str2double(v)));
        }

        // Retourne une String en valeur absolue
        private String abs(String v) {
            return v.substring(v.indexOf('-') + 1, v.length());
        }


        // Retourne une String arrondie à 2 décimales
        private String roundTwoDecimals(float f) {
            DecimalFormatSymbols point = new DecimalFormatSymbols();
            point.setDecimalSeparator('.');
            DecimalFormat twoDForm = new DecimalFormat("0.00", point);
            return twoDForm.format(f);
        }
    }
}
