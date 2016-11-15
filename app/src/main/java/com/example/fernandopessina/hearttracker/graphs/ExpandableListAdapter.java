package com.example.fernandopessina.hearttracker.graphs;

/**
 * Created by fernando.pessina on 09/11/2016.
 */

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.fernandopessina.hearttracker.R;

import java.util.ArrayList;
import java.util.List;

public class ExpandableListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int HEADER = 0;
    public static final int CHILD = 1;
    public static final int MIN_BPM = 40;
    public static final int MAX_BPM = 180;

    private List<Item> data;

    public ExpandableListAdapter(List<Item> data) {
        this.data = data;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        View view = null;
        Context context = parent.getContext();
        float dp = context.getResources().getDisplayMetrics().density;
        int subItemPaddingLeft = (int) (18 * dp);
        int subItemPaddingTopAndBottom = (int) (5 * dp);
        switch (type) {
            case HEADER:
                LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.list_header, parent, false);
                ListHeaderViewHolder header = new ListHeaderViewHolder(view);
                return header;
            case CHILD:
                LayoutInflater inflater1 = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater1.inflate(R.layout.list_child, parent, false);
                ListMeasurementViewHolder child = new ListMeasurementViewHolder(view);
                return child;
        }
        return null;
    }

    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final Item item = data.get(position);
        switch (item.type) {
            case HEADER:
                final ListHeaderViewHolder itemController = (ListHeaderViewHolder) holder;
                itemController.referralItem = item;
                itemController.header_title.setText(item.text);
                itemController.avg_tv.setText("Avg: "+item.average);
                if (item.invisibleChildren == null) {
                    itemController.btn_expand_toggle.setImageResource(R.drawable.circle_minus);
                } else {
                    itemController.btn_expand_toggle.setImageResource(R.drawable.circle_plus);
                }
                itemController.btn_expand_toggle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (item.invisibleChildren == null) {
                            item.invisibleChildren = new ArrayList<Item>();
                            int count = 0;
                            int pos = data.indexOf(itemController.referralItem);
                            while (data.size() > pos + 1 && data.get(pos + 1).type == CHILD) {
                                item.invisibleChildren.add(data.remove(pos + 1));
                                count++;
                            }
                            notifyItemRangeRemoved(pos + 1, count);
                            itemController.btn_expand_toggle.setImageResource(R.drawable.circle_plus);
                        } else {
                            int pos = data.indexOf(itemController.referralItem);
                            int index = pos + 1;
                            for (Item i : item.invisibleChildren) {
                                data.add(index, i);
                                index++;
                            }
                            notifyItemRangeInserted(pos + 1, index - pos - 1);
                            itemController.btn_expand_toggle.setImageResource(R.drawable.circle_minus);
                            item.invisibleChildren = null;
                        }
                    }
                });
                break;
            case CHILD:
                final ListMeasurementViewHolder itemCont = (ListMeasurementViewHolder) holder;
                itemCont.referralItem = item;
                itemCont.bpmProgBar.setProgress(item.bpm-MIN_BPM);
                itemCont.bpmProgBar.setMax(MAX_BPM-MIN_BPM);
                itemCont.bpm.setText(String.valueOf(item.bpm));
                itemCont.date.setText(item.date);
                itemCont.type.setText(item.mType);
                break;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return data.get(position).type;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private static class ListHeaderViewHolder extends RecyclerView.ViewHolder {
        public TextView header_title;
        public TextView avg_tv;
        public ImageView btn_expand_toggle;
        public Item referralItem;

        public ListHeaderViewHolder(View itemView) {
            super(itemView);
            avg_tv = (TextView) itemView.findViewById(R.id.average);
            header_title = (TextView) itemView.findViewById(R.id.header_title);
            btn_expand_toggle = (ImageView) itemView.findViewById(R.id.btn_expand_toggle);
        }
    }

    private static class ListMeasurementViewHolder extends RecyclerView.ViewHolder{
        public TextView bpm;
        public TextView type;
        public TextView date;
        public ProgressBar bpmProgBar;
        public Item referralItem;

        public ListMeasurementViewHolder(View itemView){
            super(itemView);
            bpm = (TextView) itemView.findViewById(R.id.bpm);
            type = (TextView) itemView.findViewById(R.id.bpmType);
            date = (TextView) itemView.findViewById(R.id.bpmDate);
            bpmProgBar = (ProgressBar) itemView.findViewById(R.id.bpmProgBar);
        }
    }

    public static class Item {
        public int type;
        public String text;
        public int bpm;
        public int average;
        public String date;
        public String mType;
        public List<Item> invisibleChildren;

        public Item(int type, String text, int average) {
            this.type = type;
            this.text = text;
            this.average = average;
        }

        public Item(int type, int bpm, String mType, String date) {
            this.type = type;
            this.bpm = bpm;
            this.date = date;
            this.mType = mType;
        }
    }
}