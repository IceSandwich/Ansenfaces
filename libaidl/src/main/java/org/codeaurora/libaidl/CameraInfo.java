package org.codeaurora.libaidl;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.Size;

import java.util.ArrayList;

public class CameraInfo implements Parcelable {
    private final static String TAG = CameraInfo.class.getSimpleName();

    public enum CameraFacing {
        Front,
        Back,
        Unknown
    }

    private String m_name;
    private CameraFacing m_facing;
    private Size[] m_resolutions;

    public CameraInfo(String id, CameraCharacteristics cameraCharacteristics) {
        this.m_name = id;

        Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
        switch (facing) {
            case CameraCharacteristics.LENS_FACING_FRONT:
                this.m_facing = CameraFacing.Front;
                break;
            case CameraCharacteristics.LENS_FACING_BACK:
                this.m_facing = CameraFacing.Back;
                break;
            default:
                Log.d("CameraInfo", "camera Id: " + id + " got unknown facing: " + facing);
                this.m_facing = CameraFacing.Unknown;
                break;
        }

        StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            Log.e(TAG, "Can't get stream configuration map for id: " + id);
            this.m_resolutions = new Size[0];
        } else {
            this.m_resolutions = map.getOutputSizes(SurfaceTexture.class);
        }
    }

    public String GetId() {
        return m_name;
    }

    public CameraFacing GetFacing() {
        return m_facing;
    }

    public Size[] GetResolutions() {
        return m_resolutions;
    }

    protected CameraInfo(Parcel in) {
        this.m_name = in.readString();
        this.m_facing = CameraFacing.values()[in.readInt()];

        this.m_resolutions = new Size[in.readInt()];
        for (int j = 0; j < this.m_resolutions.length; j++) {
            int width = in.readInt(), height = in.readInt();
            this.m_resolutions[j] = new Size(width, height);
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.m_name);
        parcel.writeInt(this.m_facing.ordinal());
        parcel.writeInt(this.m_resolutions.length);
        for (int j = 0; j < this.m_resolutions.length; j++) {
            parcel.writeInt(this.m_resolutions[j].getWidth());
            parcel.writeInt(this.m_resolutions[j].getHeight());
        }
    }

    @Override
    public String toString() {
        ArrayList<String> parts = new ArrayList<>();

        parts.add("Camera{ ");
        parts.add("Name=`" + this.m_name + "`");
        parts.add("Facing=" + this.m_facing.name());

        {
            String resolution = "";
            for (Size s : this.m_resolutions) {
                resolution += "(" + s.getWidth() + ", " + s.getHeight() + "), ";
            }
            parts.add("Resolution=[" + resolution + "]");
        }

        parts.add(" }");

        String ret = "";
        for (int i = 0; i < parts.size(); i++) {
            ret += parts.get(i);
            if (i != 0 && i != parts.size() - 1) {
                ret += ", ";
            }
        }
        return ret;
    }

    public static final Creator<CameraInfo> CREATOR = new Creator<CameraInfo>() {
        @Override
        public CameraInfo createFromParcel(Parcel in) {
            return new CameraInfo(in);
        }

        @Override
        public CameraInfo[] newArray(int size) {
            return new CameraInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}
