package org.codeaurora.snapcam;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private MainActivity m_this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_this = this;
        Button but = (Button) findViewById(R.id.but_ask_permission);
        but.setOnClickListener(onButAskPermissionClick);

        // init check
        checkForbiddenPermissionAndSetStatus();
    }

    private View.OnClickListener onButAskPermissionClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            ArrayList<String> forbiddenPermission = checkForbiddenPermission();
            if (!forbiddenPermission.isEmpty()) {
                String[] permissionString = forbiddenPermission.toArray(new String[forbiddenPermission.size()]);
                m_this.requestPermissions(permissionString, 1);
            }

            checkForbiddenPermissionAndSetStatus();
        }
    };

    private static final String[] permissions = {
//        Manifest.permission.READ_EXTERNAL_STORAGE,
//        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    };

    public ArrayList<String> checkForbiddenPermission() {
        ArrayList<String> permissionList = new ArrayList<>();
        for (String permission : permissions) {
            if (this.checkSelfPermission(permission)  != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(permission);
            }
        }
        return permissionList;
    }

    public ArrayList<String> checkForbiddenPermissionAndSetStatus() {
        ArrayList<String> forbiddenPermission = checkForbiddenPermission();
        TextView tv = (TextView) findViewById(R.id.txt_ask_permission);
        if (forbiddenPermission.isEmpty()) {
            tv.setText("Current: Granted");
            CameraService.SetPermissionStatus(true);
        } else {
            tv.setText("Current: Forbidden");
            CameraService.SetPermissionStatus(false);
        }
        return forbiddenPermission;
    }

}