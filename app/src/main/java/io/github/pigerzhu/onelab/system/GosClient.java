package io.github.pigerzhu.onelab.system;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Parcel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class GosClient {
    private static final String GOS_PACKAGE = "com.samsung.android.game.gos";
    private static final String GOS_SERVICE_ACTION = "com.samsung.android.game.gos.IGosService";
    private static final String GOS_SERVICE_DESCRIPTOR = "com.samsung.android.game.gos.IGosService";
    private static final int GOS_REQUEST_TRANSACTION = 1;

    private GosClient() {
    }

    public static void request(Context context, String command, String json, Callback callback) {
        new Thread(() -> {
            final IBinder[] binder = new IBinder[1];
            CountDownLatch connected = new CountDownLatch(1);
            ServiceConnection connection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    binder[0] = service;
                    connected.countDown();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    binder[0] = null;
                }
            };

            Intent intent = new Intent(GOS_SERVICE_ACTION);
            intent.setPackage(GOS_PACKAGE);
            boolean bound = false;
            try {
                bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
                if (!bound) {
                    callback.onError("连接 GOS 失败：服务不可绑定。");
                    return;
                }
                if (!connected.await(3L, TimeUnit.SECONDS) || binder[0] == null) {
                    callback.onError("连接 GOS 超时。");
                    return;
                }
                callback.onResult(transact(binder[0], command, json));
            } catch (Exception e) {
                callback.onError("GOS 调用失败：" + e.getMessage());
            } finally {
                if (bound) {
                    try {
                        context.unbindService(connection);
                    } catch (Exception ignored) {
                    }
                }
            }
        }, "OneLab-GOS").start();
    }

    private static String transact(IBinder binder, String command, String json) throws Exception {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(GOS_SERVICE_DESCRIPTOR);
            data.writeString(command);
            data.writeString(json);
            if (!binder.transact(GOS_REQUEST_TRANSACTION, data, reply, 0)) {
                throw new IllegalStateException("Binder transact 返回 false");
            }
            reply.readException();
            return reply.readString();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public interface Callback {
        void onResult(String result);

        void onError(String message);
    }
}
