package com.example.kingqi.paykeep;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.bigkoo.pickerview.builder.TimePickerBuilder;
import com.bigkoo.pickerview.listener.OnTimeSelectListener;
import com.bigkoo.pickerview.view.TimePickerView;

import org.litepal.LitePal;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PayKeep";
    private List<Pay> pays = null;
    private RecyclerView recyclerView;
    private PayAdapter adapter;
    private TextView sum,time;
    private FloatingActionButton fab;
    private int yearToSearch,monthToSearch;
    private Toolbar toolbar;
    private int mod = 0;
//    private String ip = "---insert your ip and port like :127.0.0.1:8000----";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        添加第一次启动的工作
        SharedPreferences preferences = MyApplication.getContext().getSharedPreferences("paykeep_data",MODE_PRIVATE);
        boolean first = preferences.getBoolean("first",true);
        if (first){
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("first",false);
            editor.apply();
//            关闭Android P的兼容性弹窗
            closeAndroidPDialog();
        }
        init();
//        sync();
    }

    private void init(){
//        fitWindows();
         toolbar = (Toolbar) findViewById(R.id.toolbar);
        Calendar calendar = Calendar.getInstance();
        monthToSearch = calendar.get(Calendar.MONTH)+1;
        yearToSearch = calendar.get(Calendar.YEAR);
        toolbar.setSubtitle(monthToSearch+"月账单:");
        setSupportActionBar(toolbar);

        pays = getDataFromDB(yearToSearch,monthToSearch);

        recyclerView= (RecyclerView)findViewById(R.id.recycler_view);
        adapter = new PayAdapter(pays);
        adapter.setOnItemClickListener(new PayAdapter.OnItemOnClickListener() {
            @Override
            public void onItemOnClick(View view, int pos) {
                Toast.makeText(MainActivity.this,"长按可删除哦~",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onItemLongOnClick(View view, int pos) {
                showPopMenu(view,pos);
            }
        });
        recyclerView.setAdapter(adapter);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                //进入编辑页面：
                Intent intent = new Intent(MainActivity.this,EditPay.class);
                startActivityForResult(intent,1);
            }
        });

        sum = (TextView)findViewById(R.id.sum);
        sum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mod==0)
                    mod=1;
                else
                    mod = 0;
                refreshSum();
            }
        });
        sum.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
//                将本月的信息按照某一格式输出
                getPre();
                return true;
            }
        });

        time = (TextView)findViewById(R.id.time);
        time.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                pickTime();
                return true;
            }
        });
        refreshSum();
    }
    private void saveFile(int year,int month){
        List<Pay> toSave = getDataFromDB(year,month);
        StringBuilder builder = new StringBuilder();
        for (Pay pay:toSave){
            builder.append(pay.getYear()).append("/").append(pay.getMonth()).append("\t").append(pay.getTag())
                    .append("\t").append(pay.getName()).append("\t").append(pay.getMoney())
                    .append("\t").append(pay.isPrivate()).append("\n");
        }
        DataOutputStream dataOutputStream = null;
        String dirPath = Environment.getExternalStoragePublicDirectory("")+"/paykeep/";
        File file = null,dir;
        try {
            dir = new File(dirPath);
            if (!dir.exists())
                dir.mkdir();
            file = new File(dirPath+year+"_"+month+".txt");

            dataOutputStream = new DataOutputStream(new FileOutputStream(file));
            dataOutputStream.writeUTF(builder.toString());
        }catch (IOException e){
            e.printStackTrace();
            Toast.makeText(MainActivity.this,"导出失败了哎~",Toast.LENGTH_SHORT).show();
        }finally {
            try {
                if (dataOutputStream!=null) {
                    dataOutputStream.close();
                    Toast.makeText(MainActivity.this,"导出成功了~",Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Toast.makeText(MainActivity.this,"导出失败了哎~",Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void pickTime(){
        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager.isActive()){
            inputMethodManager.hideSoftInputFromWindow(time.getApplicationWindowToken(),0);
        }
        Calendar startTime = Calendar.getInstance();
        startTime.set(2019,0,1);
        TimePickerView timePickerView = new TimePickerBuilder(MainActivity.this, new OnTimeSelectListener() {
            @Override
            public void onTimeSelect(Date d, View v) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(d);
                yearToSearch = calendar.get(Calendar.YEAR);
                monthToSearch = calendar.get(Calendar.MONTH)+1;
                pays = getDataFromDB(yearToSearch,monthToSearch);
                adapter = new PayAdapter(pays);
                recyclerView.setAdapter(adapter);
                toolbar.setSubtitle(monthToSearch+"月账单:");
                refreshSum();
            }
        }).setType(new boolean[]{true,true,false,false,false,false}).setRangDate(startTime,Calendar.getInstance()).build();
        timePickerView.show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case 1:
                if (resultCode == RESULT_OK ){
                    final Pay pay =(Pay) data.getSerializableExtra("pay");

                    pay.setId(LitePal.count(Pay.class)+1);
//                    pay.setUploaded(false);
                    pay.save();

                    pays.add(0,pay);
                    adapter.notifyItemInserted(0);
                    recyclerView.scrollToPosition(0);

                    addPay(pay,1);

                    Snackbar.make(fab,"添加成功",Snackbar.LENGTH_LONG)
                            .setAction("撤销", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    deletePay(pay,0);
                                }
                            })
                            .show();

                }
        }
    }
//    private void sync(){
//        List<Pay> pays = LitePal.where("uploaded=?","0").find(Pay.class);
//        for (Pay pay:pays){
//            addPay(pay,0);
//        }
//    }
    private void addPay(final Pay pay,final int n){
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                OkHttpClient client = new OkHttpClient();
//                RequestBody requestBody = new FormBody.Builder()
//                        .add("id",String.valueOf(pay.getId()))
//                        .add("item_spend",pay.getName())
//                        .add("money",String.valueOf(pay.getMoney()))
//                        .add("year",String.valueOf(pay.getYear()))
//                        .add("month",String.valueOf(pay.getMonth()))
//                        .add("day",String.valueOf(pay.getDay()))
//                        .add("isPri",pay.isPrivate()?"True":"False")
//                        .add("fun",String.valueOf(1))
//                        .build();
//                Request request = new Request.Builder()
//                        .url("http://"+ip+"/add_pay/")
//                        .post(requestBody)
//                        .build();
//                try {
//                    Response response = client.newCall(request).execute();
//                    String rd = response.body().string();
//                    Log.d(TAG, "get response : "+rd);
//                    if (rd.isEmpty()){
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                if (n==1)
//                                    Toast.makeText(MainActivity.this,"啊咧咧~下次再上传~",Toast.LENGTH_SHORT).show();
//                            }
//                        });
//                    }else {
//                        if (rd.equals("add_ok")){
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    pay.setUploaded(true);
                                    pay.save();
                                    refreshSum();
//                                    if (n==1)
//                                        Toast.makeText(MainActivity.this,"添加成功",Toast.LENGTH_SHORT).show();
//                                }
//                            });
//                        }else {
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    if(n==1)
//                                        Toast.makeText(MainActivity.this,"添加失败",Toast.LENGTH_SHORT).show();
//                                }
//                            });
//                        }
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (n==1)
//                                Toast.makeText(MainActivity.this,"啊咧咧~下次再上传~",Toast.LENGTH_SHORT).show();
//                        }
//                    });
//                }
//            }
//        }).start();
    }

    private void deletePay(final Pay pay,final int pos){
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                OkHttpClient client = new OkHttpClient();
//                RequestBody requestBody = new FormBody.Builder()
//                        .add("id",String.valueOf(pay.getId()))
//                        .add("item_spend",pay.getName())
//                        .add("money",String.valueOf(pay.getMoney()))
//                        .add("year",String.valueOf(pay.getYear()))
//                        .add("month",String.valueOf(pay.getMonth()))
//                        .add("day",String.valueOf(pay.getDay()))
//                        .add("isPri",pay.isPrivate()?"True":"False")
//                        .add("fun",String.valueOf(2))
//                        .build();
//                Request request = new Request.Builder()
//                        .url("http://"+ip+"/add_pay/")
//                        .post(requestBody)
//                        .build();
//                try {
//                    Response response = client.newCall(request).execute();
//                    String rd = response.body().string();
//                    Log.d(TAG, "get response : "+rd);
//                    if (rd.isEmpty()){
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                Toast.makeText(MainActivity.this,"啊咧咧~电波无法到达~",Toast.LENGTH_SHORT).show();
//                            }
//                        });
//                    }else {
//                        if (rd.equals("delete_ok")){
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
                                    pays.remove(pos);
                                    adapter.notifyItemRemoved(pos);
                                    recyclerView.scrollToPosition(pos);
                                    LitePal.delete(Pay.class,pay.getId());
                                    refreshSum();
                                    Toast.makeText(MainActivity.this,"消除成功",Toast.LENGTH_SHORT).show();
//                                }
//                            });
//                        }else {
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    Toast.makeText(MainActivity.this,"消除失败",Toast.LENGTH_SHORT).show();
//                                }
//                            });
//                        }
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(MainActivity.this,"啊咧咧~电波无法到达~",Toast.LENGTH_SHORT).show();
//                        }
//                    });
//                }
//            }
//        }).start();
    }

    private void testDB(){
        List<Pay> pays = LitePal.findAll(Pay.class);
        String t="";
        for (Pay pay:pays){
            t+=pay.toString();
        }
        Log.d(TAG, "testDB: "+t);
    }
    private List<Pay> getDataFromDB(int year,int month){
//        Calendar calendar = Calendar.getInstance();
//        int year = calendar.get(Calendar.YEAR);
//        int month = calendar.get(Calendar.MONTH)+1;
        List<Pay> pays = LitePal.where("year=?",String.valueOf(year))
                                .where("month=?",String.valueOf(month))
                                .find(Pay.class);
        Collections.reverse(pays);
        return pays;
    }
    protected void fitWindows(){
        Window window = getWindow();//设置系统栏是否适应的
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }
    private float getSum(){
        float sum =0;
        for (Pay pay:pays){
            if (mod==0){
                if (pay.isPrivate()==false)
                    sum+=pay.getMoney();
            }else {
                if (pay.isPrivate()==true)
                    sum+=pay.getMoney();
            }
        }
        return sum;
    }
    private void refreshSum(){
        sum.setText((mod==0?"日常花费:":"小金库花费:")+String.valueOf(getSum())+"元");
    }
    public void showPopMenu(View view,final int pos){
        PopupMenu popupMenu = new PopupMenu(this,view);
        popupMenu.getMenuInflater().inflate(R.menu.menu_item,popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                Pay pa = pays.get(pos);
                deletePay(pa,pos);
                return false;
            }
        });
        popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
            }
        });
        popupMenu.show();
    }
    private void getPre(){
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }else
            saveFile(yearToSearch,monthToSearch);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    saveFile(yearToSearch,monthToSearch);
                }else {
                    Toast.makeText(MainActivity.this,"开启权限呀~zz",Toast.LENGTH_SHORT).show();
                }
        }
    }
    private void closeAndroidPDialog(){
        try {
            Class aClass = Class.forName("android.content.pm.PackageParser$Package");
            Constructor declaredConstructor = aClass.getDeclaredConstructor(String.class);
            declaredConstructor.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Class cls = Class.forName("android.app.ActivityThread");
            Method declaredMethod = cls.getDeclaredMethod("currentActivityThread");
            declaredMethod.setAccessible(true);
            Object activityThread = declaredMethod.invoke(null);
            Field mHiddenApiWarningShown = cls.getDeclaredField("mHiddenApiWarningShown");
            mHiddenApiWarningShown.setAccessible(true);
            mHiddenApiWarningShown.setBoolean(activityThread, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

