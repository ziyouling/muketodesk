package com.muketang.mukescreen;


import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**定义菜单项类*/
class BarlMenuItem {
    String menuTitle ;
    int menuIcon ;

    //构造方法
    public BarlMenuItem(String menuTitle , int menuIcon ){
        this.menuTitle = menuTitle ;
        this.menuIcon = menuIcon ;
    }

}
/**自定义设置侧滑菜单ListView的Adapter*/
public class MenuAdapter extends BaseAdapter{

    //存储侧滑菜单中的各项的数据
    List<BarlMenuItem> MenuItems = new ArrayList<BarlMenuItem>( ) ;
    //构造方法中传过来的activity
    Context context ;

    //构造方法
    public MenuAdapter(Context context ){

        this.context = context ;

        MenuItems.add(new BarlMenuItem("后置摄像头", com.serenegiant.common.R.color.TRANSPARENT));
        MenuItems.add(new BarlMenuItem("前置摄像头", com.serenegiant.common.R.color.TRANSPARENT));
        MenuItems.add(new BarlMenuItem("共享屏幕", com.serenegiant.common.R.color.TRANSPARENT));
//        MenuItems.add(new BarlMenuItem("重连", com.serenegiant.common.R.color.TRANSPARENT));
        MenuItems.add(new BarlMenuItem("退出", com.serenegiant.common.R.color.TRANSPARENT));
    }

    @Override
    public int getCount() {

        return MenuItems.size();

    }

    @Override
    public BarlMenuItem getItem(int position) {

        return MenuItems.get(position) ;
    }

    @Override
    public long getItemId(int position) {

        return position ;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout view = (LinearLayout)convertView ;
        if(view == null){
            view =(LinearLayout)LayoutInflater.from(context).inflate(R.layout.bar_item, parent, false);
            ImageView icon= view.findViewById(R.id.mi_icon);
            icon.setImageResource(getItem(position).menuIcon);
            TextView text=view.findViewById(R.id.mi_text);
            text.setText(getItem(position).menuTitle);
        }
        return view ;
    }

}