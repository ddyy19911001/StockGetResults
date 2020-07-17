package com.dy.bollandxiong;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.dy.bollandxiong.base.MyBaseFragment;
import com.dy.bollandxiong.base.UrlConstance;
import com.dy.bollandxiong.data.StockData;
import com.dy.fastframework.util.HtmlStrUtils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.vise.xsnow.http.ViseHttp;
import com.vise.xsnow.http.callback.ACallback;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import yin.deng.normalutils.utils.LogUtils;
import yin.deng.normalutils.utils.MyUtils;
import yin.deng.normalutils.utils.NoDoubleClickListener;

public class PageFragment extends MyBaseFragment {
    private LineChart mLineChart;
    private EditText etPoint;
    private Button btAddPoint;
    private EditText etDayCount;
    private Button btMoveDays;
    private Button btReset;
    private float maxOfAllListData;
    private float minOfAllListData;
    private List<List<String>> datas=new ArrayList<>();
    private List<List<String>> datasQx=new ArrayList<>();
    private List<List<String>> datas5=new ArrayList<>();
    private List<List<String>> datas10=new ArrayList<>();
    private List<List<String>> datas20=new ArrayList<>();
    private float toDeleteLine;//减仓线5日线拐头向下未交于10日线或20日线时
    private float toBottomLine;//清仓线5日线拐头向下交于10日线或20日线时
    private float toTopLine;//建仓线5日线拐头向上且低于10日线和20日线
    private float toAddLine;//加仓线5日线拐头向上且高于20日线
    private int dayCount=550;
    private String shangZheng="sh000001";
    private String shenZheng="sz399001";
    private String chuangYe="sz399006";
    private List<Float> addPoints=new ArrayList<>();
    private List<Float> deletePoints=new ArrayList<>();
    private List<Float> createPoints=new ArrayList<>();
    private List<Float> clearPoints=new ArrayList<>();
    private int position;
    private List<List<String>> needAddPoint=new ArrayList<>();
    private String moveCurrentDayTo;//需要前移的天数
    private TextView tvNowpoint;

    @Override
    public int setContentView() {
        return R.layout.page_fg;
    }

    @Override
    public void bindViewWithId(View view) {
        mLineChart = (LineChart) view.findViewById(R.id.mLineChart);
        etPoint = (EditText) view.findViewById(R.id.et_point);
        btAddPoint = (Button) view.findViewById(R.id.bt_add_point);
        etDayCount = (EditText) view.findViewById(R.id.et_day_count);
        btMoveDays = (Button) view.findViewById(R.id.bt_move_days);
        btReset = (Button) view.findViewById(R.id.bt_reset);
        tvNowpoint = (TextView) view.findViewById(R.id.tv_now_point);


    }

    @Override
    public void init() {
        initDataAndMaxMin();
        initClickListener();
    }

    private void initClickListener() {
        btAddPoint.setOnClickListener(new NoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View v) {
                if(isOkOfAddPoint()){
                    SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd");
                    List<String> list=new ArrayList<>();
                    list.add(dateFormat.format(new Date()));
                    list.add(datas.get(0).get(1));
                    list.add(etPoint.getText().toString());
                    list.add(datas.get(0).get(3));
                    list.add(datas.get(0).get(4));
                    needAddPoint.add(0,list);
                    initDataAndMaxMin();
                }
            }
        });
        btReset.setOnClickListener(new NoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View v) {
                needAddPoint.clear();
                moveCurrentDayTo =null;
                initDataAndMaxMin();
            }
        });

        btMoveDays.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isOkOfRemoveDay()){
                    moveCurrentDayTo =etDayCount.getText().toString();
                    initDataAndMaxMin();
                }
            }
        });
    }

    private boolean isOkOfAddPoint() {
        if(MyUtils.isEmpty(etPoint)){
            showTs("请输入内容");
            return false;
        }
        return true;
    }

    private boolean isOkOfRemoveDay() {
        if(MyUtils.isEmpty(etDayCount)){
            showTs("请输入内容");
            return false;
        }
        return true;
    }

    @Override
    public void onFragmentVisibleChange(boolean isVisible) {
        super.onFragmentVisibleChange(isVisible);
    }

    /**
     * 获取最近20日均线的数据和最高值与最低值
     */
    private void initDataAndMaxMin() {
        datas.clear();
        datasQx.clear();
        datas5.clear();
        datas10.clear();
        datas20.clear();
        position= (int) getArguments().get("position");
        if(position==1){
            getData(shangZheng);
        }else if(position==2){
            getData(shenZheng);
        }else{
            getData(chuangYe);
        }

    }





    public  String getAnotherDay(int dayCount){
        Calendar calendar = Calendar.getInstance(); // 得到日历
        calendar.setTime(new Date());// 把开始日期赋给日历
        calendar.add(Calendar.DAY_OF_MONTH, dayCount); // 设置为num天
        //calendar.add(Calendar.HOUR_OF_DAY, num); // 设置为num小时
        //calendar.add(Calendar.MONTH, num); // 设置为num月
        Date resultDate = calendar.getTime(); // 得到时间
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd");
        return format.format(resultDate);
    }

    public void getData(String type){
        ViseHttp.GET(String.format(UrlConstance.getData,type,getAnotherDay(-600),dayCount))
                .tag(this)
                .request(new ACallback<StockData>() {
                    @Override
                    public void onSuccess(StockData data) {
                        if(data!=null&&data.getData()!=null){
                            boolean isFind=false;
                            if(position==1) {
                                Collections.reverse(data.getData().getSh000001().getDay());
                                if(needAddPoint.size()>0){
                                    datas.addAll(needAddPoint);
                                }
                                LogUtils.i("当前移动位置："+moveCurrentDayTo);
                                String day = data.getData().getSh000001().getDay().get(0).get(0);
                                LogUtils.i("第一个位置："+day);
                                if(!MyUtils.isEmpty(moveCurrentDayTo)){
                                    for(int i=0;i<150;i++) {
                                        if(data.getData().getSh000001().getDay().get(i).get(0).equals(moveCurrentDayTo)){
                                            isFind=true;
                                        }
                                        if(isFind){
                                            datas.add(data.getData().getSh000001().getDay().get(i));
                                        }
                                    }
                                }else {
                                    for(int i=0;i<150;i++) {
                                        datas.add(data.getData().getSh000001().getDay().get(i));
                                    }
                                }
                            }else if(position==2){
                                Collections.reverse(data.getData().getSz399001().getDay());
                                if(needAddPoint.size()>0){
                                    datas.addAll(needAddPoint);
                                }
                                if(!MyUtils.isEmpty(moveCurrentDayTo)){
                                    for(int i=0;i<150;i++) {
                                        if(data.getData().getSz399001().getDay().get(i).get(0).equals(moveCurrentDayTo)){
                                            isFind=true;
                                        }
                                        if(isFind){
                                            datas.add(data.getData().getSz399001().getDay().get(i));
                                        }
                                    }
                                }else {
                                    for(int i=0;i<150;i++) {
                                        datas.add(data.getData().getSz399001().getDay().get(i));
                                    }
                                }
                            }else if(position==3){
                                Collections.reverse(data.getData().getSz399006().getDay());
                                if(needAddPoint.size()>0){
                                    datas.addAll(needAddPoint);
                                }
                                if(!MyUtils.isEmpty(moveCurrentDayTo)){
                                    for(int i=0;i<150;i++) {
                                        if(data.getData().getSz399006().getDay().get(i).get(0).equals(moveCurrentDayTo)){
                                            isFind=true;
                                        }
                                        if(isFind){
                                            datas.add(data.getData().getSz399006().getDay().get(i));
                                        }
                                    }
                                }else {
                                    for(int i=0;i<150;i++) {
                                        datas.add(data.getData().getSz399006().getDay().get(i));
                                    }
                                }
                            }
                            initDataOfMaxAndMin();
                            Collections.reverse(datasQx);
                            Collections.reverse(datas5);
                            Collections.reverse(datas10);
                            Collections.reverse(datas20);
                            initLineChart();
                        }
                    }

                    @Override
                    public void onFail(int errCode, String errMsg) {
                        LogUtils.i("请求失败：\n"+errMsg);
                    }
                });
    }

    private void initDataOfMaxAndMin() {
        LogUtils.i("当日点位："+getFloat(datas.get(0)));
        maxOfAllListData= new BigDecimal(getFloat(datas.get(0))+
                getFloat(datas.get(1))+
                getFloat(datas.get(2))+
                getFloat(datas.get(3))+
                getFloat(datas.get(4))).divide(new BigDecimal(5),
                2,BigDecimal.ROUND_HALF_UP).floatValue();
        LogUtils.i("五日均线："+maxOfAllListData+"日期："+datas.get(0).get(0));
        minOfAllListData=maxOfAllListData;
        for(int i=0;i<datas.size();i++){
            //填充情绪数据
            if(datasQx.size()<27){
                initQxData(i);
            }
            if(datas5.size()<20){
                initFivePerData(i);
            }
            //填充10日均线
            if(datas10.size()<20){
                initTenPerData(i);
            }
            //填充20日均线
            if(datas20.size()<20){
                initTweenTyPerData(i);
            }
        }
    }

    private void initQxData(int i) {
        datasQx.add(datas.get(i));
    }

    private void initTweenTyPerData(int i) {
        if(i<datas.size()-1-20){
            float nowData=new BigDecimal(getFloat(datas.get(i))+
                    getFloat(datas.get(i+1))+
                    getFloat(datas.get(i+2))+
                    getFloat(datas.get(i+3))+
                    getFloat(datas.get(i+4))+
                    getFloat(datas.get(i+5))+
                    getFloat(datas.get(i+6))+
                    getFloat(datas.get(i+7))+
                    getFloat(datas.get(i+8))+
                    getFloat(datas.get(i+9))+
                    getFloat(datas.get(i+10))+
                    getFloat(datas.get(i+11))+
                    getFloat(datas.get(i+12))+
                    getFloat(datas.get(i+13))+
                    getFloat(datas.get(i+14))+
                    getFloat(datas.get(i+15))+
                    getFloat(datas.get(i+16))+
                    getFloat(datas.get(i+17))+
                    getFloat(datas.get(i+18))+
                    getFloat(datas.get(i+19)))
                    .divide(new BigDecimal(20),
                            2,BigDecimal.ROUND_HALF_UP).floatValue();
            List<String> listData=new ArrayList<>();
            listData.add("");
            listData.add("");
            if(maxOfAllListData<nowData){
                maxOfAllListData=nowData;
            }
            if(minOfAllListData>nowData){
                minOfAllListData=nowData;
            }
            listData.add(String.valueOf(nowData));
            listData.add("");
            listData.add("");

            datas20.add(listData);
        }
    }

    private void initTenPerData(int i) {
        if(i<datas.size()-1-10){
            float nowData=new BigDecimal(getFloat(datas.get(i))+
                    getFloat(datas.get(i+1))+
                    getFloat(datas.get(i+2))+
                    getFloat(datas.get(i+3))+
                    getFloat(datas.get(i+4))+
                    getFloat(datas.get(i+5))+
                    getFloat(datas.get(i+6))+
                    getFloat(datas.get(i+7))+
                    getFloat(datas.get(i+8))+
                    getFloat(datas.get(i+9)))
                    .divide(new BigDecimal(10),
                    2,BigDecimal.ROUND_HALF_UP).floatValue();
            List<String> listData=new ArrayList<>();
            listData.add("");
            listData.add("");
            listData.add(String.valueOf(nowData));
            listData.add("");
            listData.add("");
            if(maxOfAllListData<nowData){
                maxOfAllListData=nowData;
            }
            if(minOfAllListData>nowData){
                minOfAllListData=nowData;
            }
            datas10.add(listData);
        }
    }

    private void initFivePerData(int i) {
        if(i<datas.size()-1-5){
            float nowData=new BigDecimal(getFloat(datas.get(i))+
                    getFloat(datas.get(i+1))+
                    getFloat(datas.get(i+2))+
                    getFloat(datas.get(i+3))+
                    getFloat(datas.get(i+4))).divide(new BigDecimal(5),
                    2,BigDecimal.ROUND_HALF_UP).floatValue();
            List<String> listData=new ArrayList<>();
            listData.add("");
            listData.add("");
            listData.add(String.valueOf(nowData));
            listData.add("");
            listData.add("");
            if(maxOfAllListData<nowData){
                maxOfAllListData=nowData;
            }
            if(minOfAllListData>nowData){
                minOfAllListData=nowData;
            }
            datas5.add(listData);
        }
    }

    public float getFloat(List<String> strings){
        return Float.parseFloat(strings.get(2));
    }

    public void initLineChart(){
        String qx =dealWithMarketQx();
        boolean isToUp=false;//是否是上涨趋势
        double nowPoint = Double.parseDouble(datas.get(0).get(2));
        double day5PerLast = Double.parseDouble(datas5.get(datas5.size() - 2).get(2));
        double day10PerLast = Double.parseDouble(datas10.get(datas10.size() - 2).get(2));
        double day20PerLast = Double.parseDouble(datas20.get(datas20.size() - 2).get(2));
        double day5PerNow = Double.parseDouble(datas5.get(datas5.size() - 1).get(2));
        double day10PerNow = Double.parseDouble(datas10.get(datas10.size() - 1).get(2));
        double day20PerNow = Double.parseDouble(datas20.get(datas20.size() - 1).get(2));
        if(day5PerNow-day5PerLast<=0){
            isToUp=false;
        }else{
            isToUp=true;
        }
        String nowCaoZuo="无操作";
        double perData = dealWithData(day5PerNow, day10PerNow, day20PerNow);//5日线与10日线距离和10日线与20日线距离比值
        double perDataLastDay=dealWithData(day5PerLast, day10PerLast, day20PerLast);
        if(day5PerNow>=day10PerNow){
            if(day5PerNow<=day20PerNow) {
                //5日线在10日线上方
                //5日线在20日线下方
                nowCaoZuo = "抄底建仓（加仓信号）";
            }else{
                //5日线在10日线上方
                //5日线在20日线上方
                if(nowPoint-day5PerNow>=0) {
                    //当前点位在5日线上方
                    if(perData>1.2) {
                        if(perData>=perDataLastDay) {
                            nowCaoZuo = "上涨趋势强烈（可加仓）";
                        }else{
                            nowCaoZuo="持有待涨";
                        }
                    }else if(perData>0.3){
                        if(perData<=perDataLastDay){
                            nowCaoZuo="减仓操作";
                        }else{
                            nowCaoZuo="持有待涨";
                        }
                    }else{
                        if(perData<=perDataLastDay){
                            nowCaoZuo="持有待涨";
                        }else {
                            nowCaoZuo = "减至半仓";
                        }
                    }
                }else{
                    if(nowPoint-day10PerNow>0) {
                        //5日线在10日线上方
                        //5日线在20日线上方
                        //当前点在5日线下方
                        //当前点位在10日线上方
                        if(nowPoint-day5PerNow>0){
                            //当前点位比5日均线高
                            if (perData > 1.2&&perData>=perDataLastDay) {
                                nowCaoZuo="可加仓";
                            }else{
                                nowCaoZuo = "持有待涨";
                            }
                        }else {
                            if (perData > 1.2) {
                                if (perData >= perDataLastDay) {
                                    nowCaoZuo = "持有待涨";
                                } else {
                                    nowCaoZuo = "减仓操作";
                                }
                            } else if (perData > 0.3) {
                                if (perData <= perDataLastDay) {
                                    nowCaoZuo = "减至1层仓";
                                } else {
                                    nowCaoZuo = "持有3层仓";
                                }
                            } else {
                                nowCaoZuo = "清仓等下次机会";
                            }
                        }
                    }else{
                        //5日线在10日线上方
                        //5日线在20日线上方
                        //当前点在5日线下方
                        //当前点位在10日线下方
                        //大跌趋势
                        nowCaoZuo="强烈下跌趋势（清仓）";
                    }
                }
            }
        }else{
            if(day5PerNow<=day20PerNow) {
                //5日线在10日线下方
                //5日线在20日线下方
                if(perData<=0.8){
                    //5日线与10日线距离靠近，拐头趋势明显
                    LogUtils.d("趋势：5日线与10日线距离靠近，拐头趋势明显");
                    if(perDataLastDay-perData>0) {
                        if(nowPoint>=day10PerNow) {
                            nowCaoZuo = "可建仓（抄底信号）";
                        }else if(nowPoint>=day5PerNow){
                            nowCaoZuo="可建仓（抄底信号）";
                        }else if(nowPoint>=day20PerNow){
                            nowCaoZuo="拐头趋势强烈（可抄底）";
                        }else{
                            nowCaoZuo = "等待抄底（空仓观望）";
                        }
                    }else{
                        nowCaoZuo = "等待抄底（空仓观望）";
                    }
                }else{
                    //5日线与10日线
                    // 距离偏远，拐头趋势还不明显
                    LogUtils.d("趋势：5日线与10日线距离偏远，拐头趋势还不明显");
                    if(perData>1){
                        nowCaoZuo = "等待抄底（极速下跌状态）";
                    }else {
                        if (perDataLastDay - perData > 0.2) {
                            if (nowPoint >= day10PerNow) {
                                nowCaoZuo = "可建仓（抄底信号）";
                            } else if (nowPoint >= day5PerNow) {
                                nowCaoZuo = "少量建仓（抄底信号）";
                            } else if (nowPoint >= day20PerNow) {
                                nowCaoZuo = "强烈抄底信号（可建仓）";
                            } else {
                                nowCaoZuo = "等待抄底（空仓观望）";
                            }
                        } else {
                            nowCaoZuo = "等待抄底（空仓观望）";
                        }
                    }
                }
            }else {
                //5日线在10日线下方
                //5日线在20日线上方
                if (isToUp) {
                    //上涨趋势
                    if (nowPoint - day10PerNow > 0) {
                        //当前点位在10日线上方
                        if(nowPoint-day5PerNow>0){
                            //当前点位在5日线上方
                            if(perData<0.3) {
                                if (perData <= perDataLastDay) {
                                    nowCaoZuo = "可加仓";
                                }else{
                                    nowCaoZuo = "持有待涨";
                                }
                            }
                        }else {
                            if (perData <0.3) {
                                if (perData <= perDataLastDay) {
                                    nowCaoZuo = "可加仓";
                                } else {
                                    nowCaoZuo = "持有待涨";
                                }
                            } else if (perData < 1.2) {
                                nowCaoZuo = "减仓操作";
                            } else {
                                nowCaoZuo = "减至3层仓";
                            }
                        }
                    } else {
                        //当前点位在10日线下方
                        nowCaoZuo = "清仓等下次机会";
                    }
                }else{
                    //当日在下跌
                    //5日线在10日线下方
                    //5日线在20日线上方
                    if (nowPoint - day10PerNow > 0) {
                        //当前点位在10日线上方
                        if(nowPoint-day5PerNow>0){
                            //当前点位在5日线上方
                            if(perData<0.3) {
                                if (perData <= perDataLastDay) {
                                    nowCaoZuo = "持有待涨";
                                }else{
                                    nowCaoZuo = "适量减仓";
                                }
                            }
                        }else {
                            if (perData < 0.5) {
                                if (perData <= perDataLastDay) {
                                    nowCaoZuo = "减仓操作";
                                } else {
                                    nowCaoZuo = "减至半仓";
                                }
                            } else if (perData < 1.2) {
                                nowCaoZuo = "减仓至1层";
                            } else {
                                nowCaoZuo = "清仓操作";
                            }
                        }
                    } else {
                        //当前点位在10日线下方
                        if(nowPoint-day5PerNow>0){
                            //当前点位在5日线上方
                            if(perData<0.4) {
                                if (perData <= perDataLastDay) {
                                    nowCaoZuo = "持有待涨";
                                }else{
                                    nowCaoZuo = "适量减仓";
                                }
                            }else{
                                if (perData <= perDataLastDay) {
                                    nowCaoZuo = "减仓操作";
                                } else {
                                    nowCaoZuo = "减至半仓";
                                }
                            }
                        }else{
                            //当前点位在5日线下方
                            if(nowPoint-day20PerLast>0){
                                //在20日均线下方
                                nowCaoZuo="清仓等下次机会";
                            }else{
                                //没有跌破20日均线
                                LogUtils.d("昨日");
                                nowCaoZuo = "两层仓博反弹";
                            }
                        }
                    }
                }
            }
        }
        String qsl="";
        if(day5PerNow>=day10PerNow){
            if(day10PerNow>=day20PerNow){
                qsl="趋势率：";
            }else{
                if(day5PerNow>=day20PerNow){
                    qsl="趋势率(极速拉升)：";
                }else{
                    qsl="趋势率:(稳步拉升)";
                }
            }
        }else{
            if(day10PerNow>=day20PerNow){
                if(day5PerNow>day20PerNow) {
                    qsl = "趋势率：";
                }
            }else{
                qsl="趋势率(极速下跌)：";
            }
        }
        String htmlData="当前点位："+datas.get(0).get(2)+"<br><font color='#6DB1FF' size=\"25\">"+qx+"</font> <br><font color='#FF2727' size=\"25\">建议操作："+nowCaoZuo+"   "+qsl+""+perData+"</font> ";
        tvNowpoint.setText(HtmlStrUtils.getHtmlStr(getActivity(), tvNowpoint, htmlData));
        mLineChart.getDescription().setEnabled(false);
        mLineChart.setBackgroundColor(Color.WHITE);

        //自定义适配器，适配于X轴
        ValueFormatter xAxisFormatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                java.text.DecimalFormat myformat=new java.text.DecimalFormat("0");
                return myformat.format(value);
            }
        };

        XAxis xAxis = mLineChart.getXAxis();
        xAxis.setAvoidFirstLastClipping(true);//图表将避免第一个和最后一个标签条目被减掉在图表或屏幕的边缘
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelCount(20, false);
        xAxis.setGranularity(2f);
        xAxis.setValueFormatter(xAxisFormatter);

        //自定义适配器，适配于Y轴
        ValueFormatter yAxisFormatter = new ValueFormatter(){
            @Override
            public String getFormattedValue(float value) {
                java.text.DecimalFormat myformat=new java.text.DecimalFormat("0");
                return myformat.format(value);
            }

        };

        YAxis leftAxis = mLineChart.getAxisLeft();
        leftAxis.setLabelCount(12, false);
        leftAxis.setValueFormatter(yAxisFormatter);
        leftAxis.setDrawTopYLabelEntry(true);
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setSpaceTop(20f);
        leftAxis.setSpaceBottom(20f);
        LogUtils.i("最大值："+maxOfAllListData+"\n最小值："+minOfAllListData);
        leftAxis.setAxisMaximum(maxOfAllListData+10);
        leftAxis.setAxisMinimum(minOfAllListData-10);
        List<Entry> valsComp1 = new ArrayList<>();
        for(int i=0;i<datas5.size();i++){
            valsComp1.add(new Entry((i+1),Float.parseFloat(datas5.get(i).get(2))));
        }
        List<Entry> valsComp2 = new ArrayList<>();
        for(int i=0;i<datas10.size();i++){
            valsComp2.add(new Entry((i+1),Float.parseFloat(datas10.get(i).get(2))));
        }
        List<Entry> valsComp3 = new ArrayList<>();
        for(int i=0;i<datas20.size();i++){
            valsComp3.add(new Entry((i+1),Float.parseFloat(datas20.get(i).get(2))));
        }
        //这里，每重新new一个LineDataSet，相当于重新画一组折线
        //每一个LineDataSet相当于一组折线。比如:这里有两个LineDataSet：setComp1，setComp2。
        //则在图像上会有两条折线图，分别表示公司1 和 公司2 的情况.还可以设置更多
        LineDataSet setComp1 = new LineDataSet(valsComp1, 5+"日点位："+datas5.get(datas5.size()-1).get(2)+" ");
        setComp1.setAxisDependency(YAxis.AxisDependency.LEFT);
        setComp1.setColor(getResources().getColor(R.color.very_red));
        setComp1.setDrawCircles(false);
        setComp1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        LineDataSet setComp2 = new LineDataSet(valsComp2, 10+"日点位："+datas10.get(datas10.size()-1).get(2)+" ");
        setComp2.setAxisDependency(YAxis.AxisDependency.LEFT);
        setComp2.setColor(getResources().getColor(R.color.minute_yellow));
        setComp2.setDrawCircles(false);
        setComp2.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        LineDataSet setComp3 = new LineDataSet(valsComp3, 20+"日点位："+datas20.get(datas20.size()-1).get(2)+" ");
        setComp3.setAxisDependency(YAxis.AxisDependency.LEFT);
        setComp3.setColor(getResources().getColor(R.color.retry_color));
        setComp3.setDrawCircles(false);
        setComp3.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(setComp1);
        dataSets.add(setComp2);
        dataSets.add(setComp3);
        LineData lineData = new LineData(dataSets);
        mLineChart.setData(lineData);
        mLineChart.getAxisRight().setEnabled(false);
        mLineChart.getAxisLeft().removeAllLimitLines();
        LimitLine nowLine = new LimitLine(Float.parseFloat(datas.get(0).get(2)), "当前："+datas.get(0).get(2));
        nowLine.setLineColor(getResources().getColor(R.color.progress_bar_color));
        // 获得左侧侧坐标轴
        leftAxis.addLimitLine(nowLine);
        //这里开始画线
        if(toBottomLine!=0) {
            LimitLine yLimitLine = new LimitLine(toBottomLine, "清仓点");
            yLimitLine.setLineColor(Color.GREEN);
            // 获得左侧侧坐标轴
            leftAxis.addLimitLine(yLimitLine);
        }
        if(toTopLine!=0) {
            LimitLine yLimitLine = new LimitLine(toTopLine, "建仓点");
            yLimitLine.setLineColor(Color.RED);
            // 获得左侧侧坐标轴
            leftAxis.addLimitLine(yLimitLine);
        }
        if(toAddLine!=0) {
            LimitLine yLimitLine = new LimitLine(toTopLine, "加仓点");
            yLimitLine.setLineColor(Color.YELLOW);
            // 获得左侧侧坐标轴
            leftAxis.addLimitLine(yLimitLine);
        }
        if(toDeleteLine!=0) {
            LimitLine yLimitLine = new LimitLine(toDeleteLine, "减仓点");
            yLimitLine.setLineColor(Color.YELLOW);
            // 获得左侧侧坐标轴
            leftAxis.addLimitLine(yLimitLine);
        }
        List<ILineDataSet> sets = mLineChart.getData()
                .getDataSets();

        for (ILineDataSet iSet : sets) {
            LineDataSet set = (LineDataSet) iSet;
            set.setDrawValues(false);  //不显示数值
        }
        mLineChart.invalidate();
    }


    /**
     * 计算近20日市场情绪
     * @return
     *  N日BR=N日内(H-CY)之和除以N日内(CY -L)之和，
     *  其中.H为当日最高价，L为当日最低价，CY为前一交易日的收盘价，
     *  N为设定的时间参数，一般原始参数日设定为26日。
     *     应用法则:
     *     (I）BR值高于300以上时，需注意股价的回档行情。
     *     (2)BR值低于50以下时，需注意股价的反弹行情。
     *     (3 BR值的波较AR值敏感，当BR值在70一150之间波动时，属盘整行情，应观望。
     *     (I)AR, BR急速上升.意味着距价高峰已近.持股应获利了结。
     *     时开收高低
     */
    private String dealWithMarketQx() {
        int br=0;
        double heOfH=0;
        double heOfL=0;
        for(int i=0;i<datasQx.size()-1;i++){
            if(i==0){
                continue;
            }
            int lastPosition = i - 1;
            List<String> lastDay = datasQx.get(lastPosition);
            List<String> toDay = datasQx.get(i);
            double h=Double.parseDouble(toDay.get(3));
            double l=Double.parseDouble(toDay.get(4));
            double cy=Double.parseDouble(lastDay.get(2));
            heOfH+=h-cy;
            heOfL+=cy-l;
        }
        br= (int) (heOfH/heOfL)*100;
        String qxStr="一般";
        if(br>=300){
            qxStr="买入情绪极强";
        }else if(br>150){
            qxStr="买入情绪强";
        }else if(br>70){
            qxStr="买卖情绪持平";
        }else if(br>50){
            qxStr="卖出情绪强";
        }else{
            qxStr="市场陷入冰点";
        }
        double qxl=new BigDecimal(br)
                .divide(new BigDecimal(300),2,BigDecimal.ROUND_HALF_UP)
                .doubleValue();
        qxStr+="  情绪率："+qxl*100+"%";
        return qxStr;
    }

    private double dealWithData(double day5PerNow, double day10PerNow, double day20PerNow) {
        double topLeave=Math.abs(day5PerNow-day10PerNow);
        double bottomLeave=Math.abs(day10PerNow-day20PerNow);
        double per = new BigDecimal(topLeave).divide(new BigDecimal(bottomLeave), 2, BigDecimal.ROUND_HALF_UP).doubleValue();
        LogUtils.i("比值："+per);
        return per;
    }
}
