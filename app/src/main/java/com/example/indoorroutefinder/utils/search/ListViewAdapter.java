package com.example.indoorroutefinder.utils.search;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.indoorroutefinder.R;
import com.example.indoorroutefinder.utils.poiSelection.PoiGeoJsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ListViewAdapter extends BaseAdapter {

    // Declare Variables

    Context mContext;
    LayoutInflater inflater;
    private List<POI> poiList = null;
    private ArrayList<POI> arraylist;

    public ListViewAdapter(Context context, List<POI> animalNamesList) {
        mContext = context;
        this.poiList = animalNamesList;
        inflater = LayoutInflater.from(mContext);
        this.arraylist = new ArrayList<POI>();
        this.arraylist.addAll(animalNamesList);
    }

    public class ViewHolder {
        TextView name;
    }

    @Override
    public int getCount() {
        return poiList.size();
    }

    @Override
    public POI getItem(int position) {
        return poiList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(final int position, View view, ViewGroup parent) {
        final ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            view = inflater.inflate(R.layout.list_view_items, null);
            // Locate the TextViews in listview_item.xml
            holder.name = (TextView) view.findViewById(R.id.name);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        // Set the results into TextViews
        holder.name.setText(poiList.get(position).getPOIname());
        return view;
    }

    // Filter Class
    public void filter(String charText) {
        charText = charText.toLowerCase(Locale.getDefault());
        poiList.clear();
        if (charText.length() == 0) {
            poiList.addAll(arraylist);
        } else {
            for (POI wp : arraylist) {
                if (wp.getPOIname().toLowerCase(Locale.getDefault()).contains(charText)) {
                    poiList.add(wp);
                }
            }
        }
        notifyDataSetChanged();
    }

}
