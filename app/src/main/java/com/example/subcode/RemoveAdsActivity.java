package com.example.subcode;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.android.vending.billing.IInAppBillingService;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RemoveAdsActivity extends AppCompatActivity {

    // 发送广播通知其他页面去掉广告
    private static final String ACTION_REMOVE_ADS = "ACTION_REMOVE_ADS";
    // 调用订阅相关接口的 service
    IInAppBillingService mService;

    // 按月付费的变量 sku 相当于 id
    private String sku_month;
    // 按月付费的价格变相
    private String price_month;

    private String sku_jidu;
    private String price_jidu;

    private String sku_year;
    private String price_year;

    // 存储所有订阅信息的 list
    private List<VIPBean> vipBeans = new ArrayList<>();

    // 订阅字符串采用 subs 如果是内购就是 inapp
    private static final String SUBS = "subs";
    private static final int SUCEESS = 1001;
    private static final int FAILURE = 1002;
    private static final int REQUEST_CODE = 10000;

    // 在 Google Play console 创建的订阅条目的 sku 需要自行填写
    private static final String SKU_MONTH_ID = "";
    private static final String SKU_YEAR_ID = "";
    private static final String SKU_JIDU_ID = "";

    // 展示所有订阅信息的 adapter
    private AdapterVip adapterVip;
    private RecyclerView recyclerView;


    private TextView text_viped;

    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remove_ads);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");

        // 链接ServiceConn 固定都这么写
        Intent serviceIntent =
                new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);

        initView();
    }

    private void initView() {
        findViewById(R.id.img_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        recyclerView = findViewById(R.id.recylerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        adapterVip = new AdapterVip(R.layout.adapter_vip, vipBeans);
        recyclerView.setAdapter(adapterVip);

        text_viped = findViewById(R.id.text_viped);


    }


    // 初始化订阅信息
    private void initData(){
        vipBeans = new ArrayList<>();

        VIPBean vipBean = new VIPBean();
        vipBean.setId(sku_month);
        vipBean.setPrice(price_month);
        vipBean.setTimeStr("Month");
        vipBeans.add(vipBean);

        vipBean = new VIPBean();
        vipBean.setId(sku_jidu);
        vipBean.setPrice(price_jidu);
        vipBean.setTimeStr("Quarter");
        vipBeans.add(vipBean);

        vipBean = new VIPBean();
        vipBean.setId(sku_year);
        vipBean.setPrice(price_year);
        vipBean.setTimeStr("Year");
        vipBeans.add(vipBean);

        adapterVip.setNewData(vipBeans);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case SUCEESS:
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                    initData();
                    break;

                case FAILURE:
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                    break;
            }


        }
    };


    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);

            // 查询是否已经订阅 方法调用本质使用 AIDL 可以放在主线程 速度很快
            checkOwned();
        }
    };


    // 订阅某一个 sku
    private void buySku(String sku) {
        try {
            Bundle buyIntentBundle = mService.getBuyIntent(3, getPackageName(),
                    sku, "subs", "");
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            startIntentSenderForResult(pendingIntent.getIntentSender(),
                    REQUEST_CODE, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                    Integer.valueOf(0));
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
    }


    // 查询是否已经订阅 方法调用本质使用 AIDL 可以放在主线程 速度很快
    private void checkOwned() {
        try {
            Bundle ownedItems = mService.getPurchases(3, getPackageName(), SUBS, null);
            int response = ownedItems.getInt("RESPONSE_CODE");
            if (response == 0) {
                ArrayList<String> ownedSkus =
                        ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");

                if (ownedSkus != null && ownedSkus.size() > 0) {
                    // owved 已经购买
                    removeAds();
                } else {
                    text_viped.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);

                    progressDialog.show();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            // 获取所有 sku 列表
                            getSKUList();
                        }
                    }).start();
                }
            }


        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            if (resultCode == RESULT_OK) {
                try {
                    JSONObject jo = new JSONObject(purchaseData);
                    String sku = jo.getString("productId");
//                    LogUtils.e("jinguangyue", "You have bought the " + sku + ". Ads has been removed");
                    removeAds();

                    new AlertDialog.Builder(this)
                            .setMessage("Completed! Ads has been removed")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).show();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void removeAds() {
        RemoveAdsActivity.this.sendBroadcast(new Intent(ACTION_REMOVE_ADS));
        text_viped.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }


    // 获取所有 sku 列表
    private void getSKUList() {
        ArrayList<String> skuList = new ArrayList<String>();

        // 添加每一个 sku
        skuList.add(SKU_MONTH_ID);
        skuList.add(SKU_JIDU_ID);
        skuList.add(SKU_YEAR_ID);
        Bundle querySkus = new Bundle();
        querySkus.putStringArrayList("ITEM_ID_LIST", skuList);
        try {

            // sku 所有信息
            Bundle skuDetails = mService.getSkuDetails(3,
                    getPackageName(), "subs", querySkus);

            int response = skuDetails.getInt("RESPONSE_CODE");
            if (response == 0) {
                ArrayList<String> responseList
                        = skuDetails.getStringArrayList("DETAILS_LIST");
                for (String thisResponse : responseList) {
                    JSONObject object = null;
                    try {
                        object = new JSONObject(thisResponse);
                        String sku = object.getString("productId");
                        String price = object.getString("price");

                        if (sku.equals(SKU_MONTH_ID)) {
                            sku_month = sku;
                            price_month = price;
                        } else if (sku.equals(SKU_JIDU_ID)) {
                            sku_jidu = sku;
                            price_jidu = price;
                        } else if (sku.equals(SKU_YEAR_ID)) {
                            sku_year = sku;
                            price_year = price;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

                if (!TextUtils.isEmpty(sku_month) && !TextUtils.isEmpty(sku_year)) {
                    mHandler.sendEmptyMessage(SUCEESS);
                }

            } else {
                mHandler.sendEmptyMessage(FAILURE);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    class AdapterVip extends BaseQuickAdapter<VIPBean, BaseViewHolder> {

        public AdapterVip(int layoutResId, @Nullable List<VIPBean> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(BaseViewHolder helper, final VIPBean item) {
            helper.setText(R.id.btn_price,   item.getPrice()+ " / " + item.getTimeStr());
            helper.getView(R.id.btn_buy).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    buySku(item.getId());
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            unbindService(mServiceConn);
        }
    }
}
