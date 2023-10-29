package com.yowal.xfifi;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static EditText ed_accuracy;
    private static EditText ed_fluency;
    private static EditText ed_integrity;
    private static EditText ed_semantic;
    private static EditText ed_total;

    public int dp2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    private int getNum(int i, int i2) {
        if (i2 > i) {
            return new Random().nextInt(i2 - i) + i;
        }
        return -1;
    }

    public int ReadScore(Context context, String str) {
        String[] split = context.getSharedPreferences("ScoreConfig", 0).getString(str, "-1").split("~");
        if (split.length == 1) {
            return toInt(split[0]);
        }
        return getNum(toInt(split[0]), toInt(split[1]));
    }

    private String ReadScore(Context context, String str, int i) {
        String string = context.getSharedPreferences("ScoreConfig", 0).getString(str, "-1");
        return string.equals("-1") ? "" : string;
    }

    public void WriteScore(Context context, String str, String str2) {
        SharedPreferences.Editor edit = context.getSharedPreferences("ScoreConfig", 0).edit();
        edit.putString(str, str2);
        edit.apply();
    }

    private static int toInt(String str) {
        try {
            if (!"".equals(str)) {
                return Integer.parseInt(str);
            }
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam == null || !lpparam.packageName.equals("com.fifedu.tsdx")) return;
        Class<?> mClassImpl = null;
        String methodName;
        try {
            mClassImpl = XposedHelpers.findClass("com.stub.StubApp", lpparam.classLoader);
            methodName = "attachBaseContext";
        } catch (XposedHelpers.ClassNotFoundError e) {
            mClassImpl = Application.class;
            methodName = "attach";
        }
        XposedHelpers.findAndHookMethod(mClassImpl, methodName, Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Context mContext = (Context) param.args[0];
                NoUpdate(mContext); // 屏蔽更新
                addSetting(mContext);// 添加设置弹窗
                replaceAi(mContext); // 修改成绩单主分数
                replaceAll(mContext);// 修改总成绩 流利 语法 完整度(先后执行)
            }
        });
    }

    private void NoUpdate(Context context) {
        ClassLoader classLoader = context.getClassLoader();
        try {
            Class<?> mClassImpl = XposedHelpers.findClass("com.fifedu.kyxl_library_base.utils.BaseCommonUtils", classLoader);
            XposedBridge.hookAllMethods(mClassImpl, "getVersionVersionName", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    super.afterHookedMethod(methodHookParam);
                    methodHookParam.setResult("9999.999.999");
                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            e.printStackTrace();
        }
    }


    private int getId(Context context, String name) {
        return context.getResources().getIdentifier(name, "id", context.getPackageName());
    }

    private void addSetting(Context context) {
        ClassLoader classLoader = context.getClassLoader();
        try {
            Class<?> mClassImpl = XposedHelpers.findClass("com.fifedu.tsdx.ui.activity.me.settings.SelfSettingsActivity", classLoader);
            XposedBridge.hookAllMethods(mClassImpl, "createSuccessView", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    View inflate = (View) methodHookParam.getResult();
                    RelativeLayout rl_apkDownload = (RelativeLayout) inflate.findViewById(getId(context, "rl_apkDownload"));
                    rl_apkDownload.setOnClickListener(view -> {
                        showDialog(view.getContext());
                    });
                    for (int i = 0; i < rl_apkDownload.getChildCount(); i++) {
                        View child = rl_apkDownload.getChildAt(i);
                        if (child instanceof TextView) {
                            ((TextView) child).setText("修改分数");
                            break;
                        }
                    }
                    ImageView iv_apkDownload = (ImageView) inflate.findViewById(getId(context, "iv_apkDownload"));
                    iv_apkDownload.setVisibility(View.GONE);
                    super.afterHookedMethod(methodHookParam);
                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            e.printStackTrace();
        }
    }

    private void showDialog(Context context) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(dp2px(context, 10), dp2px(context, 10), 0, 0);

        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams2.setMargins(dp2px(context, 10), 0, dp2px(context, 10), 0);

        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams3.setMargins(dp2px(context, 10), dp2px(context, 5), 0, dp2px(context, 10));

        TextView textView = new TextView(context);
        textView.setLayoutParams(layoutParams);
        textView.setText("总分:");
        linearLayout.addView(textView);
        ed_total = new EditText(context);
        ed_total.setHint("请输入分数");
        ed_total.setLayoutParams(layoutParams2);
        ed_total.setSingleLine(true);
        linearLayout.addView(ed_total);
        TextView textView2 = new TextView(context);
        textView2.setLayoutParams(layoutParams);
        textView2.setText("语义:");
        linearLayout.addView(textView2);
        ed_semantic = new EditText(context);
        ed_semantic.setHint("请输入分数");
        ed_semantic.setSingleLine(true);
        ed_semantic.setLayoutParams(layoutParams2);
        linearLayout.addView(ed_semantic);
        TextView textView3 = new TextView(context);
        textView3.setLayoutParams(layoutParams);
        textView3.setText("完整:");
        linearLayout.addView(textView3);
        ed_integrity = new EditText(context);
        ed_integrity.setHint("请输入分数");
        ed_integrity.setSingleLine(true);
        ed_integrity.setLayoutParams(layoutParams2);
        linearLayout.addView(ed_integrity);
        TextView textView4 = new TextView(context);
        textView4.setLayoutParams(layoutParams);
        textView4.setText("流畅度:");
        linearLayout.addView(textView4);
        ed_fluency = new EditText(context);
        ed_fluency.setLayoutParams(layoutParams2);
        ed_fluency.setSingleLine(true);
        ed_fluency.setHint("请输入分数");
        linearLayout.addView(ed_fluency);
        TextView textView5 = new TextView(context);
        textView5.setLayoutParams(layoutParams);
        textView5.setText("准确性/发音:");
        linearLayout.addView(textView5);
        ed_accuracy = new EditText(context);
        ed_accuracy.setLayoutParams(layoutParams2);
        ed_accuracy.setSingleLine(true);
        ed_accuracy.setHint("请输入分数");
        linearLayout.addView(ed_accuracy);
        TextView textView6 = new TextView(context);
        textView6.setText("总分是什么关系我也不知道，你们看着办吧\n支持范围填写,例如 85~100 \n注意的是,符号一定要小写!\nBy King丶枫岚");
        textView6.setLayoutParams(layoutParams3);
        linearLayout.addView(textView6);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("请输入修改分数");
        builder.setView(linearLayout);
        builder.setPositiveButton("保存", (dialog, which) -> {
            WriteScore(context, "total", ed_total.getText().toString());
            WriteScore(context, "semantic", ed_semantic.getText().toString());
            WriteScore(context, "integrity", ed_integrity.getText().toString());
            WriteScore(context, "fluency", ed_fluency.getText().toString());
            WriteScore(context, "accuracy", ed_accuracy.getText().toString());
            Toast.makeText(context, "保存成功！", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("取消", null);
        builder.setNeutralButton("重置", (dialog, which) -> {
            WriteScore(context, "total", "-1");
            WriteScore(context, "semantic", "-1");
            WriteScore(context, "integrity", "-1");
            WriteScore(context, "fluency", "-1");
            WriteScore(context, "accuracy", "-1");
            Toast.makeText(context, "重置成功！", Toast.LENGTH_SHORT).show();
        });
        AlertDialog create = builder.create();
        create.setCanceledOnTouchOutside(false);
        create.show();
        ed_total.setText(ReadScore(context, "total", 0));
        ed_semantic.setText(ReadScore(context, "semantic", 0));
        ed_integrity.setText(ReadScore(context, "integrity", 0));
        ed_fluency.setText(ReadScore(context, "fluency", 0));
        ed_accuracy.setText(ReadScore(context, "accuracy", 0));
    }

    // 6.0.2  语义 发音 流利度 完整度(上部分 虚的)
    private void replaceAi(Context contetxt) {
        ClassLoader classLoader = contetxt.getClassLoader();
        try {
            Class<?> mClassImpl = XposedHelpers.findClass("com.fifedu.kyxl_library_study.ui.view.custom.ReportScoreView", classLoader);
            XposedBridge.hookAllMethods(mClassImpl, "setCommentScore", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    super.beforeHookedMethod(methodHookParam);
                    int score = ReadScore(contetxt, "total");
                    if (score > 0 && methodHookParam.args.length == 2) {
                        methodHookParam.args[1] = String.valueOf(score);
                    }
                }
            });
            // 6.0.2  语义 发音 流利度 完整度(上部分 虚的)
            XposedBridge.hookAllMethods(mClassImpl, "setDimensionScore", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    super.beforeHookedMethod(methodHookParam);
                    if (methodHookParam.args.length == 4) {
                        int score = ReadScore(contetxt, "semantic");
                        if (score > 0) {
                            methodHookParam.args[0] = String.valueOf(score);
                        }
                        score = ReadScore(contetxt, "accuracy");
                        if (score > 0) {
                            methodHookParam.args[1] = String.valueOf(score);
                        }
                        score = ReadScore(contetxt, "fluency");
                        if (score > 0) {
                            methodHookParam.args[2] = String.valueOf(score);
                        }
                        methodHookParam.args[3] = "";
                    }
                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            e.printStackTrace();
        }

        try {
            Class<?> mClassImpl = XposedHelpers.findClass("com.fifedu.tsdx.bean.task.report.LevelInfo", classLoader);
            XposedBridge.hookAllMethods(mClassImpl, "getScore", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    super.afterHookedMethod(methodHookParam);
                    int score = ReadScore(contetxt, "total");
                    if (score > 0) {
                        methodHookParam.setResult(String.valueOf(score));
                    }
                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            e.printStackTrace();
        }

        try {
            Class<?> mClassImpl = XposedHelpers.findClass("com.fifedu.kyxl_library_study.ui.view.custom.practice.PracticeRecordView", classLoader);
            XposedBridge.hookAllMethods(mClassImpl, "setContent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    super.beforeHookedMethod(methodHookParam);
                    int score = ReadScore(contetxt, "total");
                    if (score > 0 && methodHookParam.args.length == 2) {
                        methodHookParam.args[1] = String.valueOf(score);
                    }
                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            e.printStackTrace();
        }

        try {
            Class<?> mClassImpl = XposedHelpers.findClass("com.fifedu.tsdx.bean.task.report.TaskReportInfo", classLoader);
            XposedBridge.hookAllMethods(mClassImpl, "getScore", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    super.afterHookedMethod(methodHookParam);
                    int score = ReadScore(contetxt, "total");
                    if (score > 0) {
                        methodHookParam.setResult(String.valueOf(score));
                    }
                }
            });

            XposedBridge.hookAllMethods(mClassImpl, "getLld", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    super.afterHookedMethod(methodHookParam);
                    int score = ReadScore(contetxt, "fluency");
                    if (score > 0) {
                        methodHookParam.setResult(String.valueOf(score));
                    }
                }
            });

            XposedBridge.hookAllMethods(mClassImpl, "getWzd", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    super.afterHookedMethod(methodHookParam);
                    int score = ReadScore(contetxt, "integrity");
                    if (score > 0) {
                        methodHookParam.setResult(String.valueOf(score));
                    }
                }
            });

            XposedBridge.hookAllMethods(mClassImpl, "getZqd", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    super.afterHookedMethod(methodHookParam);
                    int score = ReadScore(contetxt, "accuracy");
                    if (score > 0) {
                        methodHookParam.setResult(String.valueOf(score));
                    }
                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            e.printStackTrace();
        }
    }

    private void replaceAll(Context contetxt) {
        ClassLoader classLoader = contetxt.getClassLoader();
        // 修改总成绩 流利 语法 完整度(先后执行)
        String[] strArr = {"getTotalScore", "getSemanticScore", "getIntegrityScore", "getFluencyScore", "getAccuracyScore"};
        for (String s : strArr) {
            XposedHelpers.findAndHookMethod("com.fifedu.kyxl_library_study.bean.help.TempContentInfo", classLoader, s, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    super.afterHookedMethod(methodHookParam);
                    String methodName = methodHookParam.method.getName();
                    switch (methodName) {
                        case "getTotalScore": {
                            int score = ReadScore(contetxt, "total");
                            if (score > 0) {
                                methodHookParam.setResult(score);
                            }
                            break;
                        }
                        case "getSemanticScore": {
                            int score = ReadScore(contetxt, "semantic");
                            if (score > 0) {
                                methodHookParam.setResult(score);
                            }
                            break;
                        }
                        case "getIntegrityScore": {
                            int score = ReadScore(contetxt, "integrity");
                            if (score > 0) {
                                methodHookParam.setResult(score);
                            }
                            break;
                        }
                        case "getFluencyScore": {
                            int score = ReadScore(contetxt, "fluency");
                            if (score > 0) {
                                methodHookParam.setResult(score);
                            }
                            break;
                        }
                        case "getAccuracyScore": {
                            int score = ReadScore(contetxt, "accuracy");
                            if (score > 0) {
                                methodHookParam.setResult(score);
                            }
                            break;
                        }
                    }
                }
            });
        }
    }
}
