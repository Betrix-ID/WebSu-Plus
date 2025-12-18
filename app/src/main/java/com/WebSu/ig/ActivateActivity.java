package com.WebSu.ig;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.scottyab.rootbeer.*;
import rikka.shizuku.*;

public class ActivateActivity extends Activity {

    private static final int REQUEST_CODE_SHIZUKU = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activate);
        updateStatusBarColor.updateStatusBarColor(this);

        final Activity context = this;

        Button btnSizuku = findViewById(R.id.btn_instruction);
        Button btnRoot = findViewById(R.id.btn_launch_rootbeer);
        ImageView btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					finish(); 
				}
			});

        Shizuku.addRequestPermissionResultListener(new Shizuku.OnRequestPermissionResultListener() {
				@Override
				public void onRequestPermissionResult(int requestCode, int grantResult) {
					if (requestCode == REQUEST_CODE_SHIZUKU) {
						if (grantResult == PackageManager.PERMISSION_GRANTED) {
							Intent intent = new Intent("com.WebSu.ig.PERMISSION_GRANTED");
							sendBroadcast(intent);
							Toast.makeText(context, "Izin Shizuku disetujui", Toast.LENGTH_SHORT).show();
							bindShizukuService();
							notifyPermissionGranted(); 
						} else {
							Toast.makeText(context, "Izin Shizuku ditolak", Toast.LENGTH_SHORT).show();
						}
					}
				}
			});

        btnSizuku.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					handleShizuku(context);
				}
			});

        btnRoot.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					handleRootAccess(context);
				}
			});
    }

    private void handleShizuku(Activity context) {
        try {
            if (!Shizuku.pingBinder()) {
                Toast.makeText(context, "Shizuku belum aktif, membuka aplikasinya...", Toast.LENGTH_SHORT).show();

                if (!openExplicit(context, "moe.shizuku.manager", "moe.shizuku.manager.MainActivity")) {
                    if (!openExplicit(context, "moe.shizuku.privileged.api", "moe.shizuku.manager.MainActivity")) {
                        Toast.makeText(context, "Shizuku tidak terpasang.", Toast.LENGTH_SHORT).show();
                        openPlayStore(context, "moe.shizuku.manager");
                    }
                }
                return;
            }

            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Izin Shizuku sudah diberikan", Toast.LENGTH_SHORT).show();
                bindShizukuService();
                notifyPermissionGranted(); 
            } else {
                Toast.makeText(context, "Meminta izin Shizuku...", Toast.LENGTH_SHORT).show();
                Shizuku.requestPermission(REQUEST_CODE_SHIZUKU);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Gagal mengakses Shizuku API.", Toast.LENGTH_SHORT).show();
        }
    }

    private void bindShizukuService() {
        try {
            ComponentName component = new ComponentName(getPackageName(), "com.WebSu.ig.DummyService");

            Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(component)
				.daemon(false)
				.processNameSuffix("shizuku_demo")
				.debuggable(true);

            Shizuku.bindUserService(args, new ServiceConnection() {
					@Override
					public void onServiceConnected(ComponentName name, IBinder service) {
						Toast.makeText(getApplicationContext(), "Terhubung ke Shizuku service!", Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onServiceDisconnected(ComponentName name) {
						Toast.makeText(getApplicationContext(), "Shizuku service terputus.", Toast.LENGTH_SHORT).show();
					}
				});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleRootAccess(Activity context) {
        try {
            RootBeer rootBeer = new RootBeer(context);
            if (rootBeer.isRooted()) {
                Toast.makeText(context, "Akses root terdeteksi", Toast.LENGTH_SHORT).show();
                notifyPermissionGranted(); 
            } else {
                boolean opened = openApp(context, "com.topjohnwu.magisk")
					|| openApp(context, "me.ksu.ksud")
					|| openApp(context, "io.sukisu.manager");

                if (!opened) {
                    Toast.makeText(context,
								   "Tidak ada aplikasi root terdeteksi.\nInstal Magisk, KSU, atau SukiSu terlebih dahulu.",
								   Toast.LENGTH_LONG).show();
                }
            }
        } catch (Throwable t) {
            Toast.makeText(context, "Gagal memeriksa akses root.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean openApp(Activity context, String packageName) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                context.startActivity(intent);
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private boolean openExplicit(Activity context, String pkg, String cls) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(pkg, cls));
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void openPlayStore(Activity context, String packageName) {
        try {
            Intent storeIntent = new Intent(Intent.ACTION_VIEW,
											Uri.parse("market://details?id=" + packageName));
            context.startActivity(storeIntent);
        } catch (Exception e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
											 Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }

    private void notifyPermissionGranted() {
        try {
            Intent intent = new Intent("com.WebSu.ig.PERMISSION_GRANTED");
            sendBroadcast(intent);
        } catch (Exception ignored) {}
    }
}
