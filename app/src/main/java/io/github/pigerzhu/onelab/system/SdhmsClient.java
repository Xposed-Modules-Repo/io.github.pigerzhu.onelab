package io.github.pigerzhu.onelab.system;

import android.os.IBinder;
import android.os.Parcel;

public final class SdhmsClient {
    private static final String SDHMS_SERVICE = "sdhms";
    private static final String SDHMS_DESCRIPTOR =
            "com.sec.android.sdhms.ISamsungDeviceHealthManager";

    private SdhmsClient() {
    }

    public static int getInt(int transactionCode, int fallback) {
        try {
            IBinder binder = getSystemServiceBinder(SDHMS_SERVICE);
            if (binder == null) {
                return fallback;
            }
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(SDHMS_DESCRIPTOR);
                if (!binder.transact(transactionCode, data, reply, 0)) {
                    return fallback;
                }
                reply.readException();
                return reply.readInt();
            } finally {
                reply.recycle();
                data.recycle();
            }
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static boolean setInt(int transactionCode, int value) {
        try {
            IBinder binder = getSystemServiceBinder(SDHMS_SERVICE);
            if (binder == null) {
                return false;
            }
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(SDHMS_DESCRIPTOR);
                data.writeInt(value);
                if (!binder.transact(transactionCode, data, reply, 0)) {
                    return false;
                }
                reply.readException();
                return reply.readInt() != 0;
            } finally {
                reply.recycle();
                data.recycle();
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    private static IBinder getSystemServiceBinder(String serviceName) throws Exception {
        Class<?> serviceManager = Class.forName("android.os.ServiceManager");
        Object binder = serviceManager.getMethod("getService", String.class).invoke(null, serviceName);
        return binder instanceof IBinder ? (IBinder) binder : null;
    }
}
