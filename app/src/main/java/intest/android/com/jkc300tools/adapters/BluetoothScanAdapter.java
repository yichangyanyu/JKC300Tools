package intest.android.com.jkc300tools.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import intest.android.com.jkc300tools.R;
import intest.android.com.jkc300tools.beans.BlueDevice;

public class BluetoothScanAdapter extends BaseAdapter {

    private Context mContext;
    private List<BlueDevice> mlistDev = null;

    public BluetoothScanAdapter(Context context) {
        mContext = context;
    }

    public void setData(List<BlueDevice> list_dev) {
        mlistDev = list_dev;
    }

    @Override
    public int getCount() {
        return mlistDev != null ? mlistDev.size() : 0;
    }

    @Override
    public BlueDevice getItem(int position) {
        return mlistDev.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(mContext, R.layout.bluetooth_scan_item, null);
        }
        BlueDevice blueDevice = mlistDev.get(position);
        TextView tv_dev_name_id = UniversalViewHolder.get(convertView, R.id.tv_dev_name_id);
        TextView tv_dev_mac_id = UniversalViewHolder.get(convertView, R.id.tv_dev_mac_id);
        tv_dev_name_id.setText(blueDevice.getName());
        tv_dev_mac_id.setText(blueDevice.getMac());
        return convertView;
    }
}
