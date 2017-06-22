package com.physicaloid.lib;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

/**
 * Created by ideni_000 on 2016/1/10.
 */
public class SerialReceiver {

    Physicaloid mSerial;
    SerialReceiverListener mSerialReceiverListener;
    public SerialReceiver(Physicaloid mSerial, SerialReceiverListener mSerialReceiverListener) {
        this.mSerial = mSerial;
        this.mSerialReceiverListener = mSerialReceiverListener;
    }

    // occurs USB packet loss if TEXT_MAX_SIZE is over 6000
    private static final int TEXT_MAX_SIZE = 8192;
    // Linefeed
    private final static String BR = System.getProperty("line.separator");
    // Linefeed Code Settings
    private static final int LINEFEED_CODE_CR   = 0;
    private static final int LINEFEED_CODE_CRLF = 1;
    private static final int LINEFEED_CODE_LF   = 2;
    // Defines of Display Settings
    private static final int DISP_CHAR  = 0;
    private static final int DISP_DEC   = 1;
    private static final int DISP_HEX   = 2;

    //配置
    private int mReadLinefeedCode   = LINEFEED_CODE_LF;
    private int mDisplayType        = DISP_CHAR;

    Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
//            String line=mText.toString();
            String line = (String) msg.obj;
            if(!TextUtils.isEmpty(line)&&line.contains("\n")){
                String[] lines=line.split("\n");
                for (int i=0;i<lines.length;i++){
                    if(null!=mSerialReceiverListener)mSerialReceiverListener.onReceive(lines[i]);
                }
            }else{
                if(null!=mSerialReceiverListener)mSerialReceiverListener.onReceive(line);
            }
        }
    };
    StringBuilder mText = new StringBuilder();
    boolean lastDataIs0x0D = false;
    boolean mStop=true;
    boolean mRunningMainLoop = false;

    private Runnable mLoop = new Runnable() {
        @Override
        public void run() {
            int len;
            byte[] rbuf = new byte[4096];
            while (true) {//循环
                len = mSerial.read(rbuf);
                rbuf[len] = 0;
                if (len > 0) {
                    switch (mDisplayType) {
                        case DISP_CHAR:
                            setSerialDataToTextView(mDisplayType, rbuf, len, "", "");
                            break;
                        case DISP_DEC:
                            setSerialDataToTextView(mDisplayType, rbuf, len, "013", "010");
                            break;
                        case DISP_HEX:
                            setSerialDataToTextView(mDisplayType, rbuf, len, "0d", "0a");
                            break;
                    }
                    mHandler.sendMessage(mHandler.obtainMessage(0,mText.toString()));
                    mText.setLength(0);
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (mStop) {
                    mRunningMainLoop = false;
                    return;
                }
            }
        }
    };

    void setSerialDataToTextView(int disp, byte[] rbuf, int len, String sCr, String sLf) {
        int tmpbuf;
        for (int i = 0; i < len; ++i) {
            // "\r":CR(0x0D) "\n":LF(0x0A)
            if ((mReadLinefeedCode == LINEFEED_CODE_CR) && (rbuf[i] == 0x0D)) {
                mText.append(sCr);
                mText.append(BR);
            } else if ((mReadLinefeedCode == LINEFEED_CODE_LF) && (rbuf[i] == 0x0A)) {
                mText.append(sLf);
                mText.append(BR);
            } else if ((mReadLinefeedCode == LINEFEED_CODE_CRLF) && (rbuf[i] == 0x0D)
                    && (rbuf[i + 1] == 0x0A)) {
                mText.append(sCr);
                if (disp != DISP_CHAR) {
                    mText.append(" ");
                }
                mText.append(sLf);
                mText.append(BR);
                ++i;
            } else if ((mReadLinefeedCode == LINEFEED_CODE_CRLF) && (rbuf[i] == 0x0D)) {
                // case of rbuf[last] == 0x0D and rbuf[0] == 0x0A
                mText.append(sCr);
                lastDataIs0x0D = true;
            } else if (lastDataIs0x0D && (rbuf[0] == 0x0A)) {
                if (disp != DISP_CHAR) {
                    mText.append(" ");
                }
                mText.append(sLf);
                mText.append(BR);
                lastDataIs0x0D = false;
            } else if (lastDataIs0x0D && (i != 0)) {
                // only disable flag
                lastDataIs0x0D = false;
                --i;
            } else {
                switch (disp) {
                    case DISP_CHAR:
                        mText.append((char) rbuf[i]);
                        break;
                    case DISP_DEC:
                        tmpbuf = rbuf[i];
                        if (tmpbuf < 0) {
                            tmpbuf += 256;
                        }
                        mText.append(String.format("%1$03d", tmpbuf));
                        mText.append(" ");
                        break;
                    case DISP_HEX:
                        mText.append(IntToHex2((int) rbuf[i]));
                        mText.append(" ");
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private String IntToHex2(int Value) {
        char HEX2[] = {
                Character.forDigit((Value >> 4) & 0x0F, 16),
                Character.forDigit(Value & 0x0F, 16)
        };
        String Hex2Str = new String(HEX2);
        return Hex2Str;
    }

    public void start(){
        mStop=false;
        mRunningMainLoop = true;
        new Thread(mLoop).start();
    }

    public void stop(){
        mStop=true;
    }

    public boolean isRunning(){
        return mRunningMainLoop;
    }

    public interface SerialReceiverListener{
        void onReceive(String line);
    }

}
