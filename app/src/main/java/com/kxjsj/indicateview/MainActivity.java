package com.kxjsj.indicateview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;


public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new WrapStaggeredManager().setCount(3));
        recyclerView.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                if(viewType==0)
                return SimpleViewHolder.createViewHolder(parent,R.layout.tt);
                else if(viewType==1)
                return SimpleViewHolder.createViewHolder(parent,R.layout.tt2);
                else
                return SimpleViewHolder.createViewHolder(parent,R.layout.tt3);
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                SimpleViewHolder holde= (SimpleViewHolder) holder;
                holde.setText(R.id.tv,position+"");
                holde.itemView.setBackgroundColor(getcolor(position));
                holde.itemView.getLayoutParams().height=60+position%4*30;
            }

            @Override
            public int getItemViewType(int position) {
//                if(position%4==0)
//                    return 1;
//                if(position%5==0){
//                    return 1;
//                }else
//                    return 3;
                return 0;
            }

            @Override
            public int getItemCount() {
                return Integer.MAX_VALUE;
            }
        });


    }

    private int getcolor(int position){
        if(position%3==0)
                    return 0xff00ff00;
                if(position%3==2){
                    return 0xffffff00;
                }else
                    return 0xffff00ff;
    }
    public void VV(View v){
        recyclerView.scrollToPosition(100);
    }
    public void VV2(View v){
        recyclerView.smoothScrollToPosition(99);
    }
}
