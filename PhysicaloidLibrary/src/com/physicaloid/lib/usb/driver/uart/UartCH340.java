package com.physicaloid.lib.usb.driver.uart;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.util.Log;

import com.physicaloid.BuildConfig;
import com.physicaloid.lib.framework.SerialCommunicator;
import com.physicaloid.lib.usb.driver.uart.ext.CH340AndroidDriver;
import com.physicaloid.misc.RingBuffer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author LuoJ
 * @date 2016-1-3
 * @package com.physicaloid.lib.usb.driver.uart -- UartCH340.java
 * @Description 
 */
public class UartCH340 extends SerialCommunicator {
	private static final String TAG = UartCH340.class.getSimpleName();
    private static final boolean DEBUG_SHOW = false && BuildConfig.DEBUG;
//    private UsbCdcConnection mUsbConnetionManager;

    private UartConfig mUartConfig;
    private static final int RING_BUFFER_SIZE       = 1024;
    public static final int USB_READ_BUFFER_SIZE   = 512;
//    byte[] readBuffer=new byte[USB_READ_BUFFER_SIZE];
    private static final int USB_WRITE_BUFFER_SIZE  = 512;
    private RingBuffer mBuffer;

    private UsbDeviceConnection mConnection;
    private int mInterfaceNum;
    private boolean isOpened;

    //----
    CH340AndroidDriver mCH340AndroidDriver;
    
    
    public UartCH340(Context context) {
        super(context);
//        mUsbConnetionManager = new UsbCdcConnection(context);
        mUartConfig = new UartConfig();
        mBuffer = new RingBuffer(RING_BUFFER_SIZE);
        isOpened = false;
        //----
        mCH340AndroidDriver=new CH340AndroidDriver(context);
    }

    @Override
    public boolean open() {
//        for(UsbVidList id : UsbVidList.values()) {
//            if(open(new UsbVidPid(id.getVid(), 0))){
//                return true;
//            }
//        }
//        return false;
    	UsbDevice device = mCH340AndroidDriver.EnumerateDevice();
    	if(null!=device){
    		boolean open=mCH340AndroidDriver.OpenDevice(device);
    		if(open&&mCH340AndroidDriver.isConnected()){
    			boolean uartInit = mCH340AndroidDriver.UartInit();
    			if(uartInit){
    				mCH340AndroidDriver.SetConfig(mUartConfig.baudrate, mUartConfig.dataBits, mUartConfig.stopBits, mUartConfig.parity, 0);
    				isOpened=true;
    				return true;
    			}
    		}
    	}
    	return false;
    }

    @Override
    public boolean close() {
        isOpened = false;
        return mCH340AndroidDriver.CloseDevice();
    }

    @Override
    public int read(byte[] buf, int size) {
    	return mCH340AndroidDriver.ReadData(buf, size);
    }

    @Override
    public int write(byte[] buf, int size) {
    	try {
			return mCH340AndroidDriver.WriteData(buf, size);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return 0;
    }

    /**
     * Sets Uart configurations
     * @param config configurations
     * @return true : successful, false : fail
     */
    public boolean setUartConfig(UartConfig config) {
        boolean res = true;
        boolean ret = true;
        if(mUartConfig.baudrate != config.baudrate) {
            res = setBaudrate(config.baudrate);
            ret = ret && res;
        }

        if(mUartConfig.dataBits != config.dataBits) {
            res = setDataBits(config.dataBits);
            ret = ret && res;
        }

        if(mUartConfig.parity != config.parity) {
            res = setParity(config.parity);
            ret = ret && res;
        }

        if(mUartConfig.stopBits != config.stopBits) {
            res = setStopBits(config.stopBits);
            ret = ret && res;
        }

        if(mUartConfig.dtrOn != config.dtrOn ||
           mUartConfig.rtsOn != config.rtsOn) {
            res = setDtrRts(config.dtrOn, config.rtsOn);
            ret = ret && res;
        }

        return ret;
    }

    @Override
    public boolean isOpened() {
        return isOpened;
    }

    /**
     * Sets baudrate
     * @param baudrate baudrate e.g. 9600
     * @return true : successful, false : fail
     */
    public boolean setBaudrate(int baudrate) {
        byte[] baudByte = new byte[4];

        baudByte[0] = (byte) (baudrate & 0x000000FF);
        baudByte[1] = (byte) ((baudrate & 0x0000FF00) >> 8);
        baudByte[2] = (byte) ((baudrate & 0x00FF0000) >> 16);
        baudByte[3] = (byte) ((baudrate & 0xFF000000) >> 24);
        int ret = mConnection.controlTransfer(0x21, 0x20, 0, mInterfaceNum, new byte[] {
                baudByte[0], baudByte[1], baudByte[2], baudByte[3], 0x00, 0x00,
                0x08}, 7, 100);
        if(ret < 0) { 
            if(DEBUG_SHOW) { Log.d(TAG, "Fail to setBaudrate"); }
            return false;
        }
        mUartConfig.baudrate = baudrate;
        return true;
    }

    /**
     * Sets Data bits
     * @param dataBits data bits e.g. UartConfig.DATA_BITS8
     * @return true : successful, false : fail
     */
    public boolean setDataBits(int dataBits) {
        // TODO : implement
        if(DEBUG_SHOW) { Log.d(TAG, "Fail to setDataBits"); }
        mUartConfig.dataBits = dataBits;
        return false;
    }

    /**
     * Sets Parity bit
     * @param parity parity bits e.g. UartConfig.PARITY_NONE
     * @return true : successful, false : fail
     */
    public boolean setParity(int parity) {
        // TODO : implement
        if(DEBUG_SHOW) { Log.d(TAG, "Fail to setParity"); }
        mUartConfig.parity = parity;
        return false;
    }

    public boolean setFlowControl(int flowControl){
    	if(DEBUG_SHOW) { Log.d(TAG, "Fail to setFlowControl"); }
        mUartConfig.flowControl = flowControl;
        return false;
    }
    
    /**
     * Sets Stop bits
     * @param stopBits stop bits e.g. UartConfig.STOP_BITS1
     * @return true : successful, false : fail
     */
    public boolean setStopBits(int stopBits) {
        // TODO : implement
        if(DEBUG_SHOW) { Log.d(TAG, "Fail to setStopBits"); }
        mUartConfig.stopBits = stopBits;
        return false;
    }

    @Override
    public boolean setDtrRts(boolean dtrOn, boolean rtsOn) {
        int ctrlValue = 0x0000;
        if(dtrOn) {
            ctrlValue |= 0x0001;
        }
        if(rtsOn) {
            ctrlValue |= 0x0002;
        }
        int ret = mConnection.controlTransfer(0x21, 0x22, ctrlValue, mInterfaceNum, null, 0, 100);
        if(ret < 0) { 
            if(DEBUG_SHOW) { Log.d(TAG, "Fail to setDtrRts"); }
            return false;
        }
        mUartConfig.dtrOn = dtrOn;
        mUartConfig.rtsOn = rtsOn;
        return true;
    }

    @Override
    public UartConfig getUartConfig() {
        return mUartConfig;
    }

    @Override
    public int getBaudrate() {
        return mUartConfig.baudrate;
    }

    @Override
    public int getDataBits() {
        return mUartConfig.dataBits;
    }

    @Override
    public int getParity() {
        return mUartConfig.parity;
    }

    public int getFlowControl(){
    	return mUartConfig.flowControl;
    }
    
    @Override
    public int getStopBits() {
        return mUartConfig.stopBits;
    }

    @Override
    public boolean getDtr() {
        return mUartConfig.dtrOn;
    }

    @Override
    public boolean getRts() {
        return mUartConfig.rtsOn;
    }

    @Override
    public void clearBuffer() {
        mBuffer.clear();
    }

    //////////////////////////////////////////////////////////
    // Listener for reading uart
    //////////////////////////////////////////////////////////
    private List<ReadLisener> uartReadListenerList
        = new ArrayList<ReadLisener>();
    private boolean mStopReadListener = false;

    @Override
    public void addReadListener(ReadLisener listener) {
        uartReadListenerList.add(listener);
    }

    @Override
    public void clearReadListener() {
        uartReadListenerList.clear();
    }

    @Override
    public void startReadListener() {
        mStopReadListener = false;
    }

    @Override
    public void stopReadListener() {
        mStopReadListener = true;
    }

    private void onRead(int size) {
        if(mStopReadListener) return;
        for (ReadLisener listener: uartReadListenerList) {
            listener.onRead(size);
        }
    }
    //////////////////////////////////////////////////////////
}


