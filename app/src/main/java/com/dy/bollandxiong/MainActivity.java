package com.dy.bollandxiong;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dy.bollandxiong.base.MyBaseActivity;
import com.dy.fastframework.tablayout.RecyclerTabLayout;

import java.util.ArrayList;
import java.util.List;

import yin.deng.superbase.fragment.BasePagerAdapter;

public class MainActivity extends MyBaseActivity {
    private LinearLayout llTitle;
    private LinearLayout llEtSearch;
    private TextView tvTitle;
    private ImageView ivBack;
    private TextView tvBottomLine;
    private RecyclerTabLayout foldTabLayout;
    private ViewPager foldContentLayout;
    private List<Fragment> fgs=new ArrayList<>();
    private List<String> titles=new ArrayList<>();
    private BasePagerAdapter fragmentAdapter;
    private PageFragment fg1;
    private PageFragment fg2;
    private PageFragment fg3;

    @Override
    public int setLayout() {
        return R.layout.activity_main;
    }

    @Override
    public void bindViewWithId() {
        llTitle = (LinearLayout) findViewById(R.id.ll_title);
        llEtSearch = (LinearLayout) findViewById(R.id.ll_et_search);
        tvTitle = (TextView) findViewById(R.id.tv_title);
        ivBack = (ImageView) findViewById(R.id.iv_back);
        tvBottomLine = (TextView) findViewById(R.id.tv_bottom_line);
        foldTabLayout = (RecyclerTabLayout) findViewById(R.id.fold_tab_layout);
        foldContentLayout = (ViewPager) findViewById(R.id.fold_content_layout);
        tvTitle.setText(getResources().getString(R.string.app_name));
        ivBack.setVisibility(View.GONE);
    }

    @Override
    public void initFirst() {
        initPageItem();
    }


    @Override
    public boolean setIsExitActivity() {
        return true;
    }

    private void initPageItem() {
        titles.clear();
        fgs.clear();
        titles.add("上证指数");
        titles.add("深证成指");
        titles.add("创业板指");
        fg1=new PageFragment();
        Bundle bundle1=new Bundle();
        bundle1.putInt("position", 1);
        fg1.setArguments(bundle1);
        fg2=new PageFragment();
        Bundle bundle2=new Bundle();
        bundle2.putInt("position", 2);
        fg2.setArguments(bundle2);
        fg3=new PageFragment();
        Bundle bundle3=new Bundle();
        bundle3.putInt("position", 3);
        fg3.setArguments(bundle3);
        fgs.add(fg1);
        fgs.add(fg2);
        fgs.add(fg3);
        fragmentAdapter=new BasePagerAdapter(getSupportFragmentManager(), fgs,titles);
        foldContentLayout.setOffscreenPageLimit(titles.size());
        foldContentLayout.setAdapter(fragmentAdapter);
        foldTabLayout.setUpWithViewPager(foldContentLayout);
    }
}
