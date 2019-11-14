package de.avm.rttdemo;

import android.content.Context;
import android.net.wifi.rtt.RangingResult;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RttResultAdapter extends RecyclerView.Adapter<RttResultAdapter.ViewHolder> {
    private List<RangingResult> results;
    private LayoutInflater inflater;
    private Map<String, String> apNameMap;

    RttResultAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
        this.results = new ArrayList<>();
        apNameMap = new HashMap<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.result_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String mac = results.get(position).getMacAddress().toString().toUpperCase(Locale.US);
        String name = apNameMap.get(mac);
        if (name == null) {
            name = mac;
        }
        String distance;
        if (results.get(position).getStatus() == RangingResult.STATUS_SUCCESS) {
            distance = results.get(position).getDistanceMm() / 10 + "cm";
        } else {
            distance = "RTT nicht unterstützt!";
        }
        holder.name.setText(name);
        holder.distance.setText(distance);
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public void setResults(List<RangingResult> results, Map<String, String> apNameMap) {
        this.results = results;
        this.apNameMap = apNameMap;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private TextView distance;

        public ViewHolder(View itemView) {
            super(itemView);
            this.name = itemView.findViewById(R.id.name);
            this.distance = itemView.findViewById(R.id.distance);
        }
    }
}
