package com.galian.samples;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PathPermission;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.os.PatternMatcher.PATTERN_LITERAL;
import static android.os.PatternMatcher.PATTERN_PREFIX;

public class ServerActvitity extends AppCompatActivity {

    private final static String TAG = "ServerActivity";
    private boolean mRunning = false;
    private final static int PORT = 3652;

    private ServerSocket mServerSocket = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_actvitity);
        ButterKnife.bind(this);
    }

    public void dispToast(String text) {
        Toast.makeText(ServerActvitity.this, text, Toast.LENGTH_LONG).show();
    }

    private class ServerThread extends Thread {
        @Override
        public void run() {
            if (!mRunning) {
                try {
                    mServerSocket = new ServerSocket(PORT);
                    mRunning = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dispToast("Server is started.");
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }

                while (mRunning) {
                    if (mServerSocket == null) {
                        break;
                    }
                    Socket socket;
                    try {
                        socket = mServerSocket.accept();
                        new ProcessClientRequestThread(socket).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class ProcessClientRequestThread extends Thread {
        private Socket mSocket = null;

        ProcessClientRequestThread(Socket socket) {
            mSocket = socket;
        }

        @Override
        public void run() {
            while (mRunning) {
                if (!mSocket.isConnected()) {
                    break;
                }
                InputStream in;
                OutputStream out;
                try {
                    in = mSocket.getInputStream();
                    out = mSocket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                InputStreamReader reader = new InputStreamReader(in);
                try {
                    char[] buf = new char[10240];
                    int cnt = reader.read(buf);
                    if (cnt > 0) {
                        String msg = new String(buf, 0, cnt);
                        Log.d(TAG, "Receive: " + msg);

                        String GET_ATTACK_SURFACE = "getattacksurface";
                        if (msg.startsWith(GET_ATTACK_SURFACE)) {
                            String pkgName = msg.substring(GET_ATTACK_SURFACE.length()).trim();
                            Log.d(TAG, "Get Attack Surface for " + pkgName);
                            ArrayList<String> results = getAttackSurface(pkgName);
                            for (String res : results) {
                                out.write(res.getBytes());
                                out.flush();
                            }
                        } else {
                            String reply = "Server Said: I received '" + msg + "'";
                            out.write(reply.getBytes());
                            out.flush();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class BaseInfo {
        String name;
        String permission;

        BaseInfo(String n, String p) {
            name = n;
            permission = p;
        }
    }

    class MyPattern {
        int type;
        String pattern;

        MyPattern(int type, String pattern) {
            this.type = type;
            this.pattern = pattern;
        }

        String getTypeStr() {
            String typeStr = "?";
            switch (this.type) {
                case PATTERN_LITERAL:
                    typeStr = "LITERAL";
                    break;
                case PATTERN_PREFIX:
                    typeStr = "PREFIX";
                    break;
                case PatternMatcher.PATTERN_SIMPLE_GLOB:
                    typeStr = "GLOB";
                    break;
                case 3:
                    typeStr = "ADVANCED";
                    break;
            }
            return typeStr;
        }
    }

    private class MyPathPermission extends MyPattern {
        String readPermission;
        String writePermission;

        MyPathPermission(int type, String pattern,
                         String readPermission, String writePermission) {
            super(type, pattern);
            this.readPermission = readPermission;
            this.writePermission = writePermission;
        }
    }

    private class MyProviderInfo {
        String name;
        String authority;
        String readPermission;
        String writePermission;
        boolean grantUriPermissions;
        boolean multiprocess;
        ArrayList<MyPattern> uriPermissionPatterns;
        ArrayList<MyPathPermission> pathPermissions;

        MyProviderInfo(String name, String authority, String readPermission,
                       String writePermission, boolean grantUriPermissions, boolean multiprocess) {
            this.name = name;
            this.authority = authority;
            this.readPermission = readPermission;
            this.writePermission = writePermission;
            this.grantUriPermissions = grantUriPermissions;
            this.multiprocess = multiprocess;
        }
    }

    private ArrayList<String> getAttackSurface(String pkgName) {
        /* format:
        Package: com.xxx
        Attack Surface:
          28 activities exported
          24 broadcast receivers exported
          15 content providers exported
          10 services exported
            Shared UID (android.uid.xxx)
        01 XXXActivity
            permission: XXX
        02 XXXActivity
            permission: XXX

        01 XXXReceiver
            permission: XXX

        01 XXXService
            permission: XXX

        01 XXXProvider
            Authority: XXX
            Read Permission: XXX
            Write Permission: XXX
            Multiprocess Allowed: True or False
            Grant Uri Permissions: True or False
            uriPermissionPatterns:
                Type: Pattern
            pathPermissions:
                Type: Pattern
                    Read permission:
                    Write permission:
    */
        PackageManager pm = getPackageManager();
        int[] exportedCnt = new int[4]; // 0 for activity, 1 for receiver, 2 for service, 3 for provider

        String sharedUid = null;
        // for not exceed binder buffer, get activities solely
        int flags = PackageManager.GET_ACTIVITIES;
        ArrayList<BaseInfo> activities = new ArrayList<>();
        try {
            PackageInfo pkgInfo = pm.getPackageInfo(pkgName, flags);
            sharedUid = pkgInfo.sharedUserId;
            for (ActivityInfo ai : pkgInfo.activities) {
                if (ai.exported) {// not check 'enabled', because 'disabled' activity can be enabled later
                    exportedCnt[0]++;
                    activities.add(new BaseInfo(ai.name, ai.permission));
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        ArrayList<BaseInfo> services = new ArrayList<>();
        ArrayList<BaseInfo> receivers = new ArrayList<>();
        ArrayList<MyProviderInfo> providers = new ArrayList<>();
        flags = PackageManager.GET_PROVIDERS |
                PackageManager.GET_SERVICES | PackageManager.GET_RECEIVERS;
        try {
            PackageInfo pkgInfo = pm.getPackageInfo(pkgName, flags);
            if (pkgInfo.receivers != null && pkgInfo.receivers.length > 0) {
                for (ActivityInfo ri : pkgInfo.receivers) {
                    if (ri.exported) {
                        exportedCnt[1]++;
                        receivers.add(new BaseInfo(ri.name, ri.permission));
                    }
                }
            }

            if (pkgInfo.services != null && pkgInfo.services.length > 0) {
                for (ServiceInfo si : pkgInfo.services) {
                    if (si.exported) {
                        exportedCnt[2]++;
                        services.add(new BaseInfo(si.name, si.permission));
                    }
                }
            }

            if (pkgInfo.providers != null && pkgInfo.providers.length > 0) {
                for (ProviderInfo pi : pkgInfo.providers) {
                    if (pi.exported) {
                        exportedCnt[3]++;
                        MyProviderInfo mpi = new MyProviderInfo(pi.name, pi.authority, pi.readPermission,
                                pi.writePermission, pi.grantUriPermissions, pi.multiprocess);
                        if (pi.uriPermissionPatterns != null && pi.uriPermissionPatterns.length > 0) {
                            mpi.uriPermissionPatterns = new ArrayList<>();
                            for (PatternMatcher patternMatcher : pi.uriPermissionPatterns) {
                                mpi.uriPermissionPatterns.add(new MyPattern(patternMatcher.getType(), patternMatcher.getPath()));
                            }
                        }
                        if (pi.pathPermissions != null && pi.pathPermissions.length > 0) {
                            mpi.pathPermissions = new ArrayList<>();
                            for (PathPermission pathPermission : pi.pathPermissions) {
                                mpi.pathPermissions.add(new MyPathPermission(pathPermission.getType(), pathPermission.getPath(),
                                        pathPermission.getReadPermission(), pathPermission.getWritePermission()));
                            }
                        }
                        providers.add(mpi);
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        ArrayList<String> results = new ArrayList<>();
        StringBuffer buf = new StringBuffer();
        buf.append("Package: ").append(pkgName).append("\n");
        buf.append("Attack Surface:").append("\n");
        buf.append("  ").append(exportedCnt[0]).append(" activities exported").append("\n");
        buf.append("  ").append(exportedCnt[1]).append(" broadcast receivers exported").append("\n");
        buf.append("  ").append(exportedCnt[2]).append(" services exported").append("\n");
        buf.append("  ").append(exportedCnt[3]).append(" content providers exported").append("\n");
        buf.append("    Shared UID ( ").append(sharedUid).append(" )").append("\n");
        buf.append("\n");
        results.add(buf.toString());

        buf = new StringBuffer();
        buf.append("Exported activities: ").append(exportedCnt[0]).append("\n");
        for (int i = 0; i < activities.size(); i++) {
            BaseInfo bi = activities.get(i);
            buf.append("  ").append(i + 1).append("  ").append(bi.name).append("\n");
            buf.append("    Permission: ").append(bi.permission).append("\n");
        }
        buf.append("\n");
        results.add(buf.toString());

        buf = new StringBuffer();
        buf.append("Exported receivers: ").append(exportedCnt[1]).append("\n");
        for (int i = 0; i < receivers.size(); i++) {
            BaseInfo bi = receivers.get(i);
            buf.append("  ").append(i + 1).append("  ").append(bi.name).append("\n");
            buf.append("    Permission: ").append(bi.permission).append("\n");
        }
        buf.append("\n");
        results.add(buf.toString());

        buf = new StringBuffer();
        buf.append("Exported services: ").append(exportedCnt[2]).append("\n");
        for (int i = 0; i < services.size(); i++) {
            BaseInfo bi = services.get(i);
            buf.append("  ").append(i + 1).append("  ").append(bi.name).append("\n");
            buf.append("    Permission: ").append(bi.permission).append("\n");
        }
        buf.append("\n");
        results.add(buf.toString());

        buf = new StringBuffer();
        buf.append("Exported providers: ").append(exportedCnt[3]).append("\n");
        for (int i = 0; i < providers.size(); i++) {
            MyProviderInfo mpi = providers.get(i);
            buf.append("  ").append(i + 1).append("  ").append(mpi.name).append("\n");
            buf.append("    Authority: ").append(mpi.authority).append("\n");
            buf.append("    Read Permission: ").append(mpi.readPermission).append("\n");
            buf.append("    Write Permission: ").append(mpi.writePermission).append("\n");
            buf.append("    Multiprocess Allowed: ").append(mpi.multiprocess ? "True" : "False").append("\n");
            buf.append("    Grant Uri Permissions: ").append(mpi.grantUriPermissions ? "True" : "False").append("\n");
            if (mpi.uriPermissionPatterns != null && mpi.uriPermissionPatterns.size() > 0) {
                buf.append("    UriPermissionPatterns: ").append("\n");
                for (int j = 0; j < mpi.uriPermissionPatterns.size(); j++) {
                    MyPattern myPattern = mpi.uriPermissionPatterns.get(i);
                    buf.append("      ").append(j + 1).append(" Type: ").append(myPattern.getTypeStr());
                    buf.append(" ").append(myPattern.pattern).append("\n");
                }
            }
            if (mpi.pathPermissions != null && mpi.pathPermissions.size() > 0) {
                buf.append("    PathPermissions: ").append("\n");
                for (int j = 0; j < mpi.pathPermissions.size(); j++) {
                    MyPathPermission myPathPermission = mpi.pathPermissions.get(i);
                    buf.append("      ").append(j + 1).append(" Type: ").append(myPathPermission.getTypeStr());
                    buf.append(" ").append(myPathPermission.pattern).append("\n");
                    buf.append("      Read permission: ").append(myPathPermission.readPermission).append("\n");
                    buf.append("      Write permission: ").append(myPathPermission.writePermission).append("\n");
                }
            }
        }
        buf.append("\n");
        results.add(buf.toString());
        return results;
    }

    @OnClick(R.id.start_server)
    void startServer() {
        new ServerThread().start();
    }

    @OnClick(R.id.stop_server)
    void stopServer() {
        if (mRunning) {
            mRunning = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mServerSocket.close();
                        mServerSocket = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}
