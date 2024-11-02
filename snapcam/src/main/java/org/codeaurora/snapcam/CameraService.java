package org.codeaurora.snapcam;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;

//import androidx.core.app.NotificationCompat;

import org.codeaurora.libaidl.CameraInfo;
import org.codeaurora.libaidl.ICamerasManager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class CameraService extends Service {

    private final static String TAG = CameraService.class.getSimpleName();

    private static boolean m_permissionStatus;

    public static void SetPermissionStatus(boolean status) {
        m_permissionStatus = status;
    }

    @Override
    public void onCreate() {
        super.onCreate();
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel_id")
//                .setContentTitle("My foreground service")
//                .setContentText("service is running in the foreground");
//        startForeground(1, builder.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new CameraManagerBinder();
    }

    public enum SessionStatus {
        Init,
        Opened,
        Disconnected,
        Error,
    }
    private class CameraManagerBinder extends ICamerasManager.Stub {

        private CameraManager m_cameraManager;

        private class Session {
            private CameraInfo m_cameraInfo;
            private ImageReader m_imageReader;

            private SessionStatus m_status = SessionStatus.Init;

            private CameraCaptureSession m_captureSession;
            private CameraDevice m_cameraDevice;

            public void Close() {
                if (m_captureSession != null) {
                    m_captureSession.close();
                    m_captureSession = null;
                }

                if (m_cameraDevice != null) {
                    m_cameraDevice.close();
                    m_cameraDevice = null;
                }
            }
        }

        private int m_sessionIdCounter = 0;
        private Map<Integer, Session> m_sessions;

        protected CameraManager getCameraManager() {
            if (m_cameraManager == null) {
                m_cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            }
            return m_cameraManager;
        }

        @Override
        public int GetServiceVersion() throws RemoteException {
            return 0;
        }

        @Override
        public boolean AskPermission() throws RemoteException {
            return m_permissionStatus;
        }

        @Override
        public String[] QueryCameraIdList() throws RemoteException {
            try {
                return getCameraManager().getCameraIdList();
            } catch (CameraAccessException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        @Override
        public CameraInfo QueryCameraCharacteristics(String cameraId) throws RemoteException {
            try {
                return new CameraInfo(cameraId, getCameraManager().getCameraCharacteristics(cameraId));
            } catch (CameraAccessException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public int OpenCamera(String cameraId, int resolutionIdx, int format) throws RemoteException {
            CameraInfo info = QueryCameraCharacteristics(cameraId);
            Size resolution = info.GetResolutions()[resolutionIdx];

            try {
                m_cameraManager.openCamera(cameraId, new CameraStateCallback(info, resolution, format), null);
            } catch (CameraAccessException e) {
                Log.e(TAG, "OpenCamera: Failed to open camera " + cameraId);
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            return -1;
        }

        private class CameraStateCallback extends CameraDevice.StateCallback {
            private Session m_session = new Session();

            public CameraStateCallback(CameraInfo info, Size resolution, int format) {
                super();
                m_session.m_cameraInfo = info;
                m_session.m_imageReader = ImageReader.newInstance(resolution.getWidth(), resolution.getHeight(), format, 2);
                m_session.m_imageReader.setOnImageAvailableListener(new ImageReaderListener(m_session), null);
            }

            @Override
            public void onOpened(@NonNull CameraDevice cameraDevice) {
                m_session.m_cameraDevice = cameraDevice;
                m_session.m_status = SessionStatus.Opened;
                int currentId = m_sessionIdCounter;
                m_sessionIdCounter += 1;
                m_sessions.put(currentId, m_session);

                try {
                    cameraDevice.createCaptureSession(Arrays.asList(
                            m_session.m_imageReader.getSurface()
                    ), new CaptureStateRequest(m_session, cameraDevice), null);
                } catch (CameraAccessException e) {
                    Log.e(TAG, "onOpened: Cannot create capture session");
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                Log.i(TAG, "onDisconnected: Camera " + m_session.m_cameraInfo.GetId());
                m_session.m_status = SessionStatus.Disconnected;
            }

            @Override
            public void onError(@NonNull CameraDevice cameraDevice, int i) {
                Log.e(TAG, "onError: Camera " + m_session.m_cameraInfo.GetId());
                m_session.m_status = SessionStatus.Error;
            }

        }

        private class CaptureStateRequest extends CameraCaptureSession.StateCallback {
            private Session m_session;
            private CameraDevice m_cameraDevice;
            public CaptureStateRequest(Session session, CameraDevice cameraDevice) {
                m_session = session;
                m_cameraDevice = cameraDevice;
            }

            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                m_session.m_captureSession = cameraCaptureSession;
                try {
                    CaptureRequest.Builder requestBuilder = m_cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                    requestBuilder.addTarget(m_session.m_imageReader.getSurface());
                    requestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

                    cameraCaptureSession.setRepeatingRequest(requestBuilder.build(), null, null);
                } catch (CameraAccessException e) {
                    m_session.m_status = SessionStatus.Error;
                    Log.e(TAG, "onOpened: Cannot create capture request");
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                Log.e(TAG, "onConfigureFailed: Cannot configure camera capture session");
            }
        }

        private class ImageReaderListener implements ImageReader.OnImageAvailableListener {
            private Session m_session;
            public ImageReaderListener(Session session) {
                m_session = session;
            }
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Image img = imageReader.acquireLatestImage();
                if (img == null) return;

                // 获取YUV数据
                Image.Plane[] planes = img.getPlanes();
                ByteBuffer Y = planes[0].getBuffer(); //h*w
                ByteBuffer UV = planes[1].getBuffer(); //h*w/2

                // TODO

                img.close();
            }
        }
    }

}
