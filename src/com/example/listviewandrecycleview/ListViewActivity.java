package com.example.listviewandrecycleview;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ListViewActivity extends Activity {

	private ListView mNormalListview;
	private ListView mSimpleListview;
	private MyAdapter simpleAdapter;
	private List<Map<String, Object>> mData;
	private HashMap<String, Object> map;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_view);
		
		mNormalListview = (ListView)findViewById(R.id.normalListview);
		
		mSimpleListview = (ListView)findViewById(R.id.simpleListview);
		mData = getDataForSimple();
		simpleAdapter = new MyAdapter(this);
		mSimpleListview.setAdapter(simpleAdapter);
			
	}
	
	private List<Map<String, Object>> getDataForSimple() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
 
        map = new HashMap<String, Object>();
        map.put("title", "G1");
        map.put("info", "google 1");
        map.put("img", getResources().getDrawable(R.mipmap.test));
        list.add(map);
 
       /* Map<String, Object> map2 = new HashMap<String, Object>();
        map2.put("title", "G2");
        map2.put("info", "google 2");
        map2.put("img", R.drawable.i2);
        list.add(map2);
 
        Map<String, Object> map3 = new HashMap<String, Object>();
        map3.put("title", "G3");
        map3.put("info", "google 3");
        map3.put("img", R.drawable.i3);
        list.add(map3);*/
         
        return list;
    }
	
	public class ViewHolder{
		public ImageView img;
        public TextView title;
        public TextView info;
        public Button viewBtn;
	}

	public class MyAdapter extends BaseAdapter{
		private LayoutInflater mInflater;
        
        
        public MyAdapter(Context context){
            this.mInflater = LayoutInflater.from(context);
        }
        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return mData.size();
        }
 
        @Override
        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            return null;
        }
 
        @Override
        public long getItemId(int arg0) {
            // TODO Auto-generated method stub
            return 0;
        }
 
        @SuppressWarnings("deprecation")
		@Override
        public View getView(int position, View convertView, ViewGroup parent) {
             
            ViewHolder holder = null;
            if (convertView == null) {
                 
                holder=new ViewHolder();  
                 
                convertView = mInflater.inflate(R.layout.simple_listview, null);
                holder.img = (ImageView)convertView.findViewById(R.id.img);
                holder.title = (TextView)convertView.findViewById(R.id.title);
                holder.info = (TextView)convertView.findViewById(R.id.info);
                holder.viewBtn = (Button)convertView.findViewById(R.id.view_btn);
                convertView.setTag(holder);
                 
            }else {
                 
                holder = (ViewHolder)convertView.getTag();
            }
             
             
//            holder.img.setBackgroundResource((Integer)mData.get(position).get("img"));
            holder.img.setBackgroundDrawable((Drawable)mData.get(position).get("img"));
            holder.title.setText((String)mData.get(position).get("title"));
            holder.info.setText((String)mData.get(position).get("info"));
             
            holder.viewBtn.setOnClickListener(new View.OnClickListener() {
                 
                @Override
                public void onClick(View v) {
                    showInfo();                 
                }
            });
             
             
            return convertView;
        }
         
    }
	
	public void showInfo(){
        new AlertDialog.Builder(this)
        .setTitle("我的listview")
        .setMessage("介绍...")
        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        })
        .show();
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.list_view, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch(id){
		case R.id.action_settings:
			return true;
		case R.id.normal:
			mSimpleListview.setVisibility(View.GONE);
			mNormalListview.setVisibility(View.VISIBLE);
			mNormalListview.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1, getData()));
			return true;
		case R.id.simple:
			mNormalListview.setVisibility(View.GONE);
			mSimpleListview.setVisibility(View.VISIBLE);
			String imageUrl = "http://img.baidu.com/img/image/ilogob.gif"; 
			new NetworkPhoto().execute(imageUrl); 
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}

	private List<String> getData() {
		List<String> data = new ArrayList<String>();
		for(int i=0;i<30;i++){
			data.add("测试数据"+i);
		}
		return data;
	}
	
	
	class NetworkPhoto extends AsyncTask<String, Integer, Bitmap> {
		public NetworkPhoto() {
		}

		// doInBackground(Params...)，后台进程执行的具体计算在这里实现，是AsyncTask的关键，此方法必须重载。
		@Override
		protected Bitmap doInBackground(String... urls) {
			URL url = null;
			Bitmap bitmap = null;
			HttpURLConnection conn = null;
			InputStream is = null;
			try {
				url = new URL(urls[0]);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			try {
				conn = (HttpURLConnection) url.openConnection();
				conn.setDoInput(true);
				conn.connect();
				is = conn.getInputStream();
				bitmap = BitmapFactory.decodeStream(is);
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (conn != null) {
					conn.disconnect();
					conn = null;
				}
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					is = null;
				}
			}
			return bitmap;
		}

		// onPostExecute(Result)，运行于UI线程，可以对后台任务的结果做出处理，结果
		// 就是doInBackground(Params...)的返回值。
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			// 返回结果bitmap显示在ImageView控件
//			imView.setImageBitmap(bitmap);
			Drawable dra = new BitmapDrawable(bitmap);
			map.put("img", dra);
			simpleAdapter.notifyDataSetChanged();
		}
	}
		
}
