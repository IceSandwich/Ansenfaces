package org.codeaurora.libaidl;

import org.codeaurora.libaidl.CameraInfo;

interface ICamerasManager {
    int GetServiceVersion();

    boolean AskPermission();

    String[] QueryCameraIdList();
    CameraInfo QueryCameraCharacteristics(String cameraId);

    int OpenCamera(String cameraId, int resolutionIdx, int format);



}