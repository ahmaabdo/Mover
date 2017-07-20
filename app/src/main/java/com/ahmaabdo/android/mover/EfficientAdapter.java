package com.ahmaabdo.android.mover;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Ahmad on Jul 19, 2017.
 */

public class EfficientAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final Handler handler = new Handler();
    private final AppPicker ap;

    public EfficientAdapter(final Context c, final AppPicker ap) {
        inflater = LayoutInflater.from(c);
        this.ap = ap;
    }

    @Override
    public int getCount() {
        return ap.apps.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (position < ap.apps.size()) {
            final ViewHolder holder;

            if (convertView == null || !(convertView.getTag() instanceof ViewHolder)) {
                convertView = inflater.inflate(R.layout.list_view_item, null);
                holder = new ViewHolder();
                holder.text = (TextView) convertView.findViewById(R.id.text);
                holder.pack = (TextView) convertView.findViewById(R.id.pack);
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                holder.system = (TextView) convertView.findViewById(R.id.system);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    holder.text.setText(ap.apps.get(position).loadLabel(ap.pm));
                    holder.pack.setText(ap.apps.get(position).packageName);
                    holder.system.setVisibility(((ap.apps.get(position).flags & ApplicationInfo.FLAG_SYSTEM) == 1) ? View.VISIBLE
                            : View.GONE);
                    if (position < ap.icons.size())
                        holder.icon.setImageDrawable(ap.icons.get(position));
                }
            });

        }
        return convertView;
    }

    private class ViewHolder {
        TextView text;
        TextView pack;
        ImageView icon;
        TextView system;

    }
}
