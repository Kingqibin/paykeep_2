package com.example.kingqi.paykeep;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bigkoo.pickerview.builder.TimePickerBuilder;
import com.bigkoo.pickerview.listener.OnTimeSelectListener;
import com.bigkoo.pickerview.view.TimePickerView;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import me.imid.swipebacklayout.lib.app.SwipeBackActivity;

public class EditPay extends SwipeBackActivity {

    private TextView date , tag;
    private TextInputEditText item_spend , money;
    private RadioButton yes,no;
    private ImageButton button,tagManage;
    private Pay pay;
    private static final String TAG = "EditPay";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_pay);
        init();
    }
    private void init(){
        setSwipeBackEnable(true);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle("Add Pay");
        setSupportActionBar(toolbar);

        date=(TextView)findViewById(R.id.date);
        tag = (TextView)findViewById(R.id.tag);
        item_spend = (TextInputEditText)findViewById(R.id.item_spend);
        money = (TextInputEditText)findViewById(R.id.money);
        yes = (RadioButton) findViewById(R.id.yes);
        no = (RadioButton)findViewById(R.id.no);
        no.setChecked(true);
        button = (ImageButton)findViewById(R.id.ok);

        pay = new Pay();

        Calendar calendar = Calendar.getInstance();
        final int year = calendar.get(Calendar.YEAR);
        pay.setYear(year);
        int month = calendar.get(Calendar.MONTH)+1;
        pay.setMonth(month);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        pay.setDay(day);
        date.setText(year+"/"+month+"/"+day);
        date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickTime();
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (item_spend.getText().toString().trim().isEmpty()){
                    Toast.makeText(EditPay.this,"买了啥呀！？",Toast.LENGTH_SHORT).show();
                    return;
                }
                String t = money.getText().toString().trim();
                if (t.isEmpty()){
                    Toast.makeText(EditPay.this,"多少钱呀！？",Toast.LENGTH_SHORT).show();
                    return;
                }
                pay.setName(item_spend.getText().toString());
                pay.setMoney(Double.valueOf(t));
                t = tag.getText().toString().trim();
                if (t.equals("Tag")){
                    Toast.makeText(EditPay.this,"标签呢？！",Toast.LENGTH_SHORT).show();
                    return;
                }
                if (yes.isChecked())
                    pay.setPrivate(true);
                else
                    pay.setPrivate(false);
                Intent intent = new Intent();
                intent.putExtra("pay",pay);
                setResult(RESULT_OK,intent);
                finish();
            }
        });
        tag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Set<String> tagSet = new HashSet<>();
                SharedPreferences preferences = getSharedPreferences("paykeep_data",MODE_PRIVATE);
                tagSet.add("电子产品");
                tagSet.add("周边");
                tagSet.add("出行");
                tagSet.add("饭卡充值");
                tagSet.add("学习打印");
                tagSet.add("日常用品");
                tagSet.add("水果");
                tagSet.add("娱乐");
                tagSet.add("穿衣");
                tagSet.add("药");
                tagSet.add("其它");
                tagSet.add("恋爱");
                Set<String> getSet= preferences.getStringSet("tags", new HashSet<String>());
                tagSet.addAll(getSet);
                final String[] items = new String[tagSet.size()];
                tagSet.toArray(items);
//                默认设置为第0个
                pay.setTag(items[0]);
                final AlertDialog dialog = new AlertDialog.Builder(EditPay.this)
                        .setTitle("什么钱？")
                        .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                pay.setTag(items[i]);
                                tag.setText(items[i]);
                            }
                        })
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(EditPay.this,"用于 "+pay.getTag(),Toast.LENGTH_SHORT).show();
                                dialogInterface.dismiss();
                            }
                        }).create();
                dialog.show();
            }
        });
        tag.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                View dialog_view = getLayoutInflater().inflate(R.layout.edit_dialog_view,null);
                SharedPreferences preferences = getSharedPreferences("paykeep_data",MODE_PRIVATE);
                final Set<String> getSet = preferences.getStringSet("tags", new HashSet<String>());
                final TextInputEditText editText = (TextInputEditText) dialog_view.findViewById(R.id.define_tag);
                AlertDialog dialog = new AlertDialog.Builder(EditPay.this)
                        .setTitle("定义一个标签并使用")
                        .setView(dialog_view)
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String s = editText.getText().toString();
                                pay.setTag(s);
                                tag.setText(s);
                                getSet.add(s);
                                Toast.makeText(EditPay.this,"用于 "+s,Toast.LENGTH_SHORT).show();
                                SharedPreferences.Editor editor = getSharedPreferences("paykeep_data",MODE_PRIVATE).edit();
                                editor.putStringSet("tags",getSet);
                                editor.apply();
                                dialogInterface.dismiss();
                            }
                        }).create();
                dialog.show();
                return true;
            }
        });
        tagManage = (ImageButton) findViewById(R.id.manage_tags);
        tagManage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences sharedPreferences = getSharedPreferences("paykeep_data",MODE_PRIVATE);
                final Set<String> getSet= sharedPreferences.getStringSet("tags", new HashSet<String>());
                final String[] items = new String[getSet.size()];
                final boolean[] checked = new boolean[items.length];
                for (int i = 0;i<checked.length;i++) checked[i] = false;
                getSet.toArray(items);
                AlertDialog dialog = new AlertDialog.Builder(EditPay.this)
                        .setTitle(items.length==0?"没有什么要删的~":"选择要删除的标签")
                        .setMultiChoiceItems(items,checked, new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                                checked[i] = b;
                            }
                        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                SharedPreferences.Editor editor = getSharedPreferences("paykeep_data",MODE_PRIVATE).edit();
                                getSet.clear();
                                for (int j = 0;j<checked.length;j++){
                                    if (!checked[j]){
                                        getSet.add(items[j]);
                                    }
                                }
                                editor.putStringSet("tags",getSet);
                                editor.apply();
                                dialogInterface.dismiss();
                            }
                        }).create();
                dialog.show();
            }
        });
    }
    private void pickTime(){
        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager.isActive()){
            inputMethodManager.hideSoftInputFromWindow(date.getApplicationWindowToken(),0);
        }
        Calendar now = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        start.set(now.get(Calendar.YEAR),0,1);
        TimePickerView timePickerView = new TimePickerBuilder(EditPay.this, new OnTimeSelectListener() {
            @Override
            public void onTimeSelect(Date d, View v) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(d);
                int year = calendar.get(Calendar.YEAR);
                pay.setYear(year);
                int month = calendar.get(Calendar.MONTH)+1;
                pay.setMonth(month);
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                pay.setDay(day);
                date.setText(year+"/"+month+"/"+day);
            }
        }).setType(new boolean[]{false,true,true,false,false,false}).setRangDate(start,now).build();
        timePickerView.show();
    }
    protected void fitWindows(){
        Window window = getWindow();//设置系统栏是否适应的
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }
}
