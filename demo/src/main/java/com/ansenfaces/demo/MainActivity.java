package com.ansenfaces.demo;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.codeaurora.libaidl.AnsenfacesService;
import org.codeaurora.libaidl.CameraInfo;
import org.codeaurora.libaidl.ICamerasManager;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    private ICamerasManager m_cameraManager;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            m_cameraManager = ICamerasManager.Stub.asInterface(iBinder);
            Log.i(TAG, "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            m_cameraManager = null;
            Log.i(TAG, "onServiceDisconnected");
        }
    };

    private AppCompatActivity m_this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (AnsenfacesService.Bind(this, serviceConnection) == false) {
            Log.e(TAG, "onCreate: Failed to bind service");
        }

        m_this = this;
        findViewById(R.id.button).setOnClickListener(onClickListener);
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (m_cameraManager == null) {
                Log.e(TAG, "onClick: no camera manager");
                return;
            }

            CameraInfo selectedCamera = null;
            try {
                String[] ids = m_cameraManager.QueryCameraIdList();
                for (String id : ids) {
                    Log.d(TAG, "onClick: found id: " + id);

                    CameraInfo cameraInfo = m_cameraManager.QueryCameraCharacteristics(id);
                    Log.d(TAG, "onClick: camera: " + cameraInfo.toString());

                    if (id.compareTo(ids[0]) == 0) {
                        selectedCamera = cameraInfo;
                    }
                }
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }

            if (selectedCamera == null) {
                Log.e(TAG, "onClick: cannot select a camera");
                return;
            }
            Log.d(TAG, "onClick: select a camera: " + selectedCamera.toString());

            try {
                if (!m_cameraManager.AskPermission()) {
                    Toast.makeText(getBaseContext(),"You should goto your snapcam and get camera permission", Toast.LENGTH_LONG);
                    return;
                }
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }



        }
    };
}