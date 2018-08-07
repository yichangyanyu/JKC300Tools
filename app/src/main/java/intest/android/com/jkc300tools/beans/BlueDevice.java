package intest.android.com.jkc300tools.beans;

import android.os.Parcel;
import android.os.Parcelable;

public class BlueDevice implements Parcelable {
    private String mac;
    private String name;

    protected BlueDevice(Parcel in) {
        mac = in.readString();
        name = in.readString();
    }

    public BlueDevice() {
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mac);
        dest.writeString(name);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BlueDevice> CREATOR = new Creator<BlueDevice>() {
        @Override
        public BlueDevice createFromParcel(Parcel in) {
            return new BlueDevice(in);
        }

        @Override
        public BlueDevice[] newArray(int size) {
            return new BlueDevice[size];
        }
    };

    public String getMac() {
        return mac;
    }

    public String getName() {
        return name;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public void setName(String name) {
        this.name = name;
    }
}
