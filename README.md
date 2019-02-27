# KeepCountdownView
仿Keep运动休息倒计时控件

## 效果

## 使用方法


xml：
```
<com.KeepCountdownView.KeepCountdownView
            android:id="@+id/keep1"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            //各种可选属性
            app:arcColor="#FB5858"
            app:numColor="#7CB4EF"
            app:XXX/>
```

code：
```
 @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.keep_act);
        ButterKnife.bind(this);
        //倒计时监听
        keep1.setCountdownListener(new KeepCountdownView.CountdownListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onEnd() {
               
            }
        });
    }

    @OnClick(R.id.b1)
    public void b1(View v) {
        keep1.plus(5);//增加5s

    }

    @OnClick(R.id.b2)
    public void b2() {
        keep1.post(new Runnable() {
            @Override
            public void run() {
                keep1.startCountDown();//开始倒计时
            }
        });
    }

    @OnClick(R.id.b4)
    public void b4() {
        keep1.reset();//重置
    }
```

## 可选属性

|描述|xml对应属性|默认值|
|---|---|---|
|圆弧颜色|arcColor|0xff33cc66|
|数字颜色|numColor|0xff33cc66|
|圆弧厚度|arcStrokeWidth|5dp|
|数字大小|numTextSize|70sp|
|倒计时开始角度|initDegree|270°|
|倒计时时间|maxNum|20|
|是否顺时针倒计时|isCW|false|
|圆半径|radius|90dp|
|动态增加/减少时间时圆弧增长动画时长|plusNumAnimDuration|0.8s|


