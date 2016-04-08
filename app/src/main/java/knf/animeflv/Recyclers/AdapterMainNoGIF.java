package knf.animeflv.Recyclers;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import knf.animeflv.ColorsRes;
import knf.animeflv.DownloadManager.CookieConstructor;
import knf.animeflv.DownloadManager.ManageDownload;
import knf.animeflv.Interfaces.MainRecyclerCallbacks;
import knf.animeflv.Parser;
import knf.animeflv.PicassoCache;
import knf.animeflv.R;
import knf.animeflv.Recientes.MainAnimeModel;
import knf.animeflv.StreamManager.StreamManager;
import knf.animeflv.TaskType;
import knf.animeflv.Utils.FileUtil;
import knf.animeflv.Utils.MainStates;
import knf.animeflv.Utils.NetworkUtils;
import knf.animeflv.Utils.UpdateUtil;
import knf.animeflv.Utils.eNums.DownloadTask;
import knf.animeflv.Utils.eNums.UpdateState;
import knf.animeflv.info.InfoNew;
import knf.animeflv.newMain;
import xdroid.toaster.Toaster;


public class AdapterMainNoGIF extends RecyclerView.Adapter<AdapterMainNoGIF.ViewHolder> {

    private Context context;
    private List<MainAnimeModel> Animes = new ArrayList<>();
    private MainRecyclerCallbacks callbacks;
    private Parser parser = new Parser();
    private Spinner sp;
    private MaterialDialog d;

    public AdapterMainNoGIF(Context context) {
        this.context = context;
        this.callbacks = (MainRecyclerCallbacks) context;
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile(String filePath) {
        String ret = "";
        try {
            File fl = new File(filePath);
            FileInputStream fin = new FileInputStream(fl);
            ret = convertStreamToString(fin);
            fin.close();
        } catch (IOException e) {
        } catch (Exception e) {
        }
        return ret;
    }

    public static String byte2HexFormatted(byte[] arr) {
        StringBuilder str = new StringBuilder(arr.length * 2);
        for (int i = 0; i < arr.length; i++) {
            String h = Integer.toHexString(arr[i]);
            int l = h.length();
            if (l == 1) h = "0" + h;
            if (l > 2) h = h.substring(l - 2, l);
            str.append(h.toUpperCase());
            if (i < (arr.length - 1)) str.append(':');
        }
        return str.toString();
    }

    @Override
    public AdapterMainNoGIF.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).
                inflate(R.layout.item_main_ng, parent, false);
        return new AdapterMainNoGIF.ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final AdapterMainNoGIF.ViewHolder holder, final int position) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("is_amoled", false)) {
            holder.card.setCardBackgroundColor(context.getResources().getColor(R.color.prim));
            holder.tv_tit.setTextColor(context.getResources().getColor(R.color.blanco));
            holder.ib_des.setColorFilter(ColorsRes.Holo_Dark(context));
            holder.ib_ver.setColorFilter(ColorsRes.Holo_Dark(context));
        } else {
            holder.ib_des.setColorFilter(ColorsRes.Holo_Light(context));
        }
        Boolean resaltar = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("resaltar", true);
        if (getCap(holder.getAdapterPosition()).equals("Capitulo 1") || getCap(holder.getAdapterPosition()).contains("OVA") || getCap(holder.getAdapterPosition()).contains("Pelicula")) {
            if (resaltar)
                holder.card.setCardBackgroundColor(Color.argb(100, 253, 250, 93));
        }
        final String favoritos = context.getSharedPreferences("data", Context.MODE_PRIVATE).getString("favoritos", "");
        final Boolean comp = favoritos.startsWith(Animes.get(position).getAid() + ":::") || favoritos.contains(":::" + Animes.get(position).getAid() + ":::") || favoritos.endsWith(":::" + Animes.get(position).getAid());
        if (comp) {
            if (resaltar)
                holder.card.setCardBackgroundColor(Color.argb(100, 26, 206, 246));
        }
        setUpWeb(holder.webView);
        holder.tv_num.setTextColor(getColor());
        PicassoCache.getPicassoInstance(context).load(new Parser().getBaseUrl(TaskType.NORMAL, context) + "imagen.php?certificate=" + getCertificateSHA1Fingerprint() + "&thumb=" + "http://cdn.animeflv.net/img/portada/thumb_80/" + Animes.get(holder.getAdapterPosition()).getAid() + ".jpg").error(R.drawable.ic_block_r).into(holder.iv_main);
        holder.tv_tit.setText(Animes.get(position).getTitulo());
        holder.tv_num.setText(getCap(holder.getAdapterPosition()));
        if (FileUtil.ExistAnime(Animes.get(holder.getAdapterPosition()).getEid())) {
            showPlay(holder.ib_ver);
            showDelete(holder.ib_des);
        } else {
            showCloudPlay(holder.ib_ver);
            showDownload(holder.ib_des);
        }
        if (MainStates.isProcessing()) {
            if (MainStates.getProcessingEid().equals(Animes.get(holder.getAdapterPosition()).getEid())) {
                showLoading(holder.ib_des);
            }
        }
        if (MainStates.WaitContains(Animes.get(holder.getAdapterPosition()).getEid())) {
            if (!FileUtil.ExistAnime(Animes.get(holder.getAdapterPosition()).getEid())) {
                showCloudPlay(holder.ib_ver);
                holder.ib_des.setImageResource(R.drawable.ic_waiting);
            } else {
                showPlay(holder.ib_ver);
                showDelete(holder.ib_des);
                MainStates.delFromWaitList(Animes.get(holder.getAdapterPosition()).getEid());
            }
        }
        holder.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (UpdateUtil.getState() == UpdateState.WAITING_TO_UPDATE) {
                    Toaster.toast("Actualizacion descargada, instalar para continuar");
                } else {
                    if (!MainStates.isListing()) {
                        Bundle bundleInfo = new Bundle();
                        bundleInfo.putString("aid", Animes.get(holder.getAdapterPosition()).getAid());
                        Intent intent = new Intent(context, InfoNew.class);
                        intent.putExtras(bundleInfo);
                        context.startActivity(intent);
                    } else {
                        MainStates.setListing(false);
                    }
                }
            }
        });
        holder.card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                MainStates.setListing(true);
                if (MainStates.WaitContains(Animes.get(holder.getAdapterPosition()).getEid())) {
                    MainStates.delFromWaitList(Animes.get(holder.getAdapterPosition()).getEid());
                    holder.ib_des.setImageResource(R.drawable.ic_get_r);
                    callbacks.onDelFromList();
                } else {
                    MainStates.addToWaitList(Animes.get(holder.getAdapterPosition()).getEid());
                    holder.ib_des.setImageResource(R.drawable.ic_waiting);
                    callbacks.onPutInList();
                }
                return false;
            }
        });
        holder.ib_des.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (UpdateUtil.getState() == UpdateState.WAITING_TO_UPDATE) {
                    Toaster.toast("Actualizacion descargada, instalar para continuar");
                } else {
                    if (!FileUtil.ExistAnime(Animes.get(holder.getAdapterPosition()).getEid())) {
                        if (!MainStates.isProcessing()) {
                            if (MainStates.WaitContains(Animes.get(holder.getAdapterPosition()).getEid())) {
                                final int pos = holder.getAdapterPosition();
                                new MaterialDialog.Builder(context)
                                        .content(
                                                "El capitulo " + Animes.get(pos).getNumero() +
                                                        " de " + Animes.get(pos).getTitulo() +
                                                        " se encuentra en lista de espera, si continua, sera removido de la lista, desea continuar?")
                                        .autoDismiss(true)
                                        .positiveText("Continuar")
                                        .negativeText("Cancelar")
                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                MainStates.delFromWaitList(Animes.get(pos).getEid());
                                                MainStates.setProcessing(true, Animes.get(holder.getAdapterPosition()).getEid());
                                                showLoading(holder.ib_des);
                                                new DownloadGetter(holder.ib_des, holder.ib_ver, holder.webView, Animes.get(holder.getAdapterPosition()).getEid()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                            }
                                        })
                                        .build().show();
                            } else {
                                MainStates.setProcessing(true, Animes.get(holder.getAdapterPosition()).getEid());
                                showLoading(holder.ib_des);
                                new DownloadGetter(holder.ib_des, holder.ib_ver, holder.webView, Animes.get(holder.getAdapterPosition()).getEid()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            }
                        } else {
                            Toaster.toast("Procesando");
                        }
                    } else {
                        MaterialDialog borrar = new MaterialDialog.Builder(context)
                                .title("Eliminar")
                                .titleGravity(GravityEnum.CENTER)
                                .content("Desea eliminar el capitulo " + Animes.get(holder.getAdapterPosition()).getNumero() + " de " + Animes.get(holder.getAdapterPosition()).getTitulo() + "?")
                                .positiveText("Eliminar")
                                .negativeText("Cancelar")
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        if (FileUtil.DeleteAnime(Animes.get(holder.getAdapterPosition()).getEid())) {
                                            ManageDownload.cancel(context, Animes.get(holder.getAdapterPosition()).getEid());
                                            showDownload(holder.ib_des);
                                            showCloudPlay(holder.ib_ver);
                                            Toaster.toast("Archivo Eliminado");
                                        } else {
                                            if (!FileUtil.ExistAnime(Animes.get(holder.getAdapterPosition()).getEid())) {
                                                showDownload(holder.ib_des);
                                                showCloudPlay(holder.ib_ver);
                                                Toaster.toast("Archivo Eliminado");
                                            } else {
                                                Toaster.toast("Error al Eliminar");
                                            }
                                        }
                                    }
                                })
                                .build();
                        borrar.show();
                    }
                }
            }
        });
        holder.ib_ver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (UpdateUtil.getState() == UpdateState.WAITING_TO_UPDATE) {
                    Toaster.toast("Actualizacion descargada, instalar para continuar");
                } else {
                    if (FileUtil.ExistAnime(Animes.get(holder.getAdapterPosition()).getEid())) {
                        StreamManager.Play(context, Animes.get(holder.getAdapterPosition()).getEid());
                    } else {
                        if (NetworkUtils.isNetworkAvailable()) {
                            if (!MainStates.isProcessing()) {
                                MainStates.setProcessing(true, Animes.get(holder.getAdapterPosition()).getEid());
                                showLoading(holder.ib_des);
                                new StreamGetter(holder.ib_des, holder.ib_ver, holder.webView, Animes.get(holder.getAdapterPosition()).getEid()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            }
                        }
                    }
                }
            }
        });
    }

    private void showLoading(final ImageButton button) {
        ((newMain) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.setImageResource(R.drawable.ic_warning);
                button.setEnabled(false);
            }
        });
    }

    private void showDownload(final ImageButton button) {
        ((newMain) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.setImageResource(R.drawable.ic_get_r);
                button.setEnabled(true);
            }
        });
    }

    private void showDelete(final ImageButton button) {
        ((newMain) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.setImageResource(R.drawable.ic_borrar_r);
                button.setEnabled(true);
            }
        });
    }

    private void showCloudPlay(final ImageButton button) {
        ((newMain) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.setImageResource(R.drawable.ic_cloud_play);
            }
        });
    }

    private void showPlay(final ImageButton button) {
        ((newMain) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.setImageResource(R.drawable.ic_play);
            }
        });
    }

    private String getCap(int position) {
        MainAnimeModel model = Animes.get(position);
        String res = "";
        switch (model.getTipo()) {
            case "Anime":
                res = "Capitulo " + model.getNumero();
                break;
            case "OVA":
                res = "OVA " + model.getNumero();
                break;
            case "Pelicula":
                res = "Pelicula";
                break;
        }
        return res;
    }

    private void setUpWeb(final WebView web) {
        web.getSettings().setJavaScriptEnabled(true);
        web.addJavascriptInterface(new JavaScriptInterface(context), "HtmlViewer");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (url.contains("zippyshare.com") || url.contains("blank")) {
                    web.loadUrl("javascript:("
                            + "function(){var l=document.getElementById('dlbutton');" + "var f=document.createEvent('HTMLEvents');" + "f.initEvent('click',true,true);" + "l.dispatchEvent(f);}"
                            + ")()");
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        web.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {
                String fileName = url.substring(url.lastIndexOf("/") + 1);
                String eid = fileName.replace(".mp4", "") + "E";
                if (MainStates.getDowloadTask() == DownloadTask.DESCARGA) {
                    if (!FileUtil.ExistAnime(eid) && MainStates.isProcessing()) {
                        showDelete(MainStates.getGifDownButton());
                        showPlay(MainStates.getDownStateButton());
                        String urlD = MainStates.getUrlZippy();
                        CookieManager cookieManager = CookieManager.getInstance();
                        String cookie = cookieManager.getCookie(url.substring(0, url.indexOf("/", 8)));
                        CookieConstructor constructor = new CookieConstructor(cookie, web.getSettings().getUserAgentString(), urlD);
                        ManageDownload.chooseDownDir(context, eid, url, constructor);
                        web.loadUrl("about:blank");
                    } else {
                        showDelete(MainStates.getGifDownButton());
                        showPlay(MainStates.getDownStateButton());
                        web.loadUrl("about:blank");
                    }
                }
                if (MainStates.getDowloadTask() == DownloadTask.STREAMING) {
                    int type = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("t_streaming", "0"));
                    String urlD = MainStates.getUrlZippy();
                    CookieManager cookieManager = CookieManager.getInstance();
                    String cookie = cookieManager.getCookie(url.substring(0, url.indexOf("/", 8)));
                    CookieConstructor constructor = new CookieConstructor(cookie, web.getSettings().getUserAgentString(), urlD);
                    showDownload(MainStates.getGifDownButton());
                    web.loadUrl("about:blank");
                    if (type == 1) {
                        StreamManager.mx(context).Stream(eid, url, constructor);
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            StreamManager.internal(context).Stream(eid, url, constructor);
                        } else {
                            if (FileUtil.isMXinstalled()) {
                                Toaster.toast("Version de android por debajo de lo requerido, reproduciendo en MXPlayer");
                                StreamManager.mx(context).Stream(eid, url, constructor);
                            } else {
                                Toaster.toast("No hay reproductor adecuado disponible");
                            }
                        }
                    }
                }
                MainStates.setProcessing(false, null);
            }
        });
    }

    private int getColor() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int accent = preferences.getInt("accentColor", ColorsRes.Naranja(context));
        int color = ColorsRes.Naranja(context);
        if (accent == ColorsRes.Rojo(context)) {
            color = ColorsRes.Rojo(context);
        }
        if (accent == ColorsRes.Naranja(context)) {
            color = ColorsRes.Naranja(context);
        }
        if (accent == ColorsRes.Gris(context)) {
            color = ColorsRes.Gris(context);
        }
        if (accent == ColorsRes.Verde(context)) {
            color = ColorsRes.Verde(context);
        }
        if (accent == ColorsRes.Rosa(context)) {
            color = ColorsRes.Rosa(context);
        }
        if (accent == ColorsRes.Morado(context)) {
            color = ColorsRes.Morado(context);
        }
        return color;
    }

    public void setData(List<MainAnimeModel> data) {
        Animes = new ArrayList<>();
        Animes.addAll(data);
        notifyDataSetChanged();
    }

    private String getCertificateSHA1Fingerprint() {
        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();
        int flags = PackageManager.GET_SIGNATURES;
        PackageInfo packageInfo = null;
        try {
            packageInfo = pm.getPackageInfo(packageName, flags);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Signature[] signatures = packageInfo.signatures;
        byte[] cert = signatures[0].toByteArray();
        InputStream input = new ByteArrayInputStream(cert);
        CertificateFactory cf = null;
        try {
            cf = CertificateFactory.getInstance("X509");
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        X509Certificate c = null;
        try {
            c = (X509Certificate) cf.generateCertificate(input);
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        String hexString = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] publicKey = md.digest(c.getEncoded());
            hexString = byte2HexFormatted(publicKey);
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        }
        return hexString;
    }

    @Override
    public int getItemCount() {
        return Animes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView iv_main;
        public TextView tv_tit;
        public TextView tv_num;
        public CardView card;
        public ImageButton ib_ver;
        public ImageButton ib_des;
        public WebView webView;

        public ViewHolder(View itemView) {
            super(itemView);
            this.iv_main = (ImageView) itemView.findViewById(R.id.img_main);
            this.tv_tit = (TextView) itemView.findViewById(R.id.tv_main_Tit);
            this.tv_num = (TextView) itemView.findViewById(R.id.tv_main_Cap);
            this.card = (CardView) itemView.findViewById(R.id.card_main);
            this.ib_ver = (ImageButton) itemView.findViewById(R.id.ib_main_ver);
            this.ib_des = (ImageButton) itemView.findViewById(R.id.ib_main_descargar);
            this.webView = (WebView) itemView.findViewById(R.id.wv_main);
        }
    }

    private class JavaScriptInterface {
        private Context ctx;

        JavaScriptInterface(Context ctx) {
            this.ctx = ctx;
        }

        @JavascriptInterface
        public void showHTML(String html) {
            String s_html_i = html.substring(21);
            String s_html_f = "{" + s_html_i.substring(0, s_html_i.length() - 7);
        }
    }

    private class DownloadGetter extends AsyncTask<String, String, String> {
        ImageButton button;
        ImageButton DownState;
        WebView web;
        String eid;

        public DownloadGetter(ImageButton button, ImageButton state, WebView web, String eid) {
            this.button = button;
            this.eid = eid;
            this.DownState = state;
            this.web = web;
        }

        @Override
        protected String doInBackground(String... params) {
            Looper.prepare();
            new SyncHttpClient().get(parser.getInicioUrl(TaskType.NORMAL, context) + "?url=" + parser.getUrlCached(eid) + "&certificate=" + parser.getCertificateSHA1Fingerprint(context) + "&newMain", null, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject jsonObject) {
                    super.onSuccess(statusCode, headers, jsonObject);
                    try {
                        JSONArray jsonArray = jsonObject.getJSONArray("downloads");
                        final List<String> nombres = new ArrayList<>();
                        final List<String> urls = new ArrayList<>();
                        try {
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject object = jsonArray.getJSONObject(i);
                                String u = object.getString("url");
                                if (!u.trim().equals("null")) {
                                    nombres.add(object.getString("name"));
                                    urls.add(u);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        d = new MaterialDialog.Builder(context)
                                .title("Opciones")
                                .titleGravity(GravityEnum.CENTER)
                                .customView(R.layout.dialog_down, false)
                                .cancelable(true)
                                .autoDismiss(false)
                                .positiveText("Descargar")
                                .negativeText("Cancelar")
                                .callback(new MaterialDialog.ButtonCallback() {
                                    @Override
                                    public void onPositive(MaterialDialog dialog) {
                                        super.onPositive(dialog);
                                        String des = nombres.get(sp.getSelectedItemPosition());
                                        final String ur = urls.get(sp.getSelectedItemPosition());
                                        Log.d("Descargar", "URL -> " + ur);
                                        switch (des.toLowerCase()) {
                                            case "zippyshare":
                                                MainStates.setZippyState(DownloadTask.DESCARGA, ur, button, DownState);
                                                web.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        web.loadUrl(ur);
                                                    }
                                                });
                                                d.dismiss();
                                                break;
                                            case "mega":
                                                d.dismiss();
                                                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ur)));
                                                MainStates.setProcessing(false, null);
                                                break;
                                            default:
                                                ManageDownload.chooseDownDir(context, eid, ur);
                                                MainStates.setProcessing(false, null);
                                                showDelete(button);
                                                showPlay(DownState);
                                                d.dismiss();
                                                break;
                                        }

                                    }

                                    @Override
                                    public void onNegative(MaterialDialog dialog) {
                                        super.onNegative(dialog);
                                        MainStates.setProcessing(false, null);
                                        showDownload(button);
                                        d.dismiss();
                                    }
                                })
                                .cancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        d.dismiss();
                                        MainStates.setProcessing(false, null);
                                        showDownload(button);
                                    }
                                })
                                .build();
                        sp = (Spinner) d.getCustomView().findViewById(R.id.spinner_down);
                        sp.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, nombres));
                        d.show();
                    } catch (Exception e) {
                        MainStates.setProcessing(false, null);
                        showDownload(button);
                        e.printStackTrace();
                        Toaster.toast("Error en JSON");
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    super.onFailure(statusCode, headers, responseString, throwable);
                    MainStates.setProcessing(false, null);
                    Log.e("Error", "Download", throwable);
                    showDownload(button);
                    if (parser.getUrlCached(eid).equals("null")) {
                        if (FileUtil.existDir()) {
                            Toaster.toast("Error, no se escuentra el directorio, porfavor abralo manualmente y reintente descargar");
                        } else {
                            Toaster.toast("Error, no se escuentra anime en el directorio, porfavor abralo manualmente para actualizar");
                        }
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    MainStates.setProcessing(false, null);
                    Log.e("Error", "Download", throwable);
                    showDownload(button);
                }
            });
            Looper.loop();
            return null;
        }
    }

    private class StreamGetter extends AsyncTask<String, String, String> {
        ImageButton button;
        ImageButton DownState;
        WebView web;
        String eid;

        public StreamGetter(ImageButton button, ImageButton state, WebView web, String eid) {
            this.button = button;
            this.eid = eid;
            this.DownState = state;
            this.web = web;
        }

        @Override
        protected String doInBackground(String... params) {
            Looper.prepare();
            new SyncHttpClient().get(parser.getInicioUrl(TaskType.NORMAL, context) + "?url=" + parser.getUrlCached(eid) + "&certificate=" + parser.getCertificateSHA1Fingerprint(context) + "&newMain", null, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject jsonObject) {
                    super.onSuccess(statusCode, headers, jsonObject);
                    try {
                        JSONArray jsonArray = jsonObject.getJSONArray("downloads");
                        final List<String> nombres = new ArrayList<>();
                        final List<String> urls = new ArrayList<>();
                        try {
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject object = jsonArray.getJSONObject(i);
                                String u = object.getString("url");
                                if (!u.trim().equals("null")) {
                                    nombres.add(object.getString("name"));
                                    urls.add(u);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        d = new MaterialDialog.Builder(context)
                                .title("Opciones")
                                .titleGravity(GravityEnum.CENTER)
                                .customView(R.layout.dialog_down, false)
                                .cancelable(true)
                                .autoDismiss(false)
                                .positiveText("Reproducirr")
                                .negativeText("Cancelar")
                                .callback(new MaterialDialog.ButtonCallback() {
                                    @Override
                                    public void onPositive(MaterialDialog dialog) {
                                        super.onPositive(dialog);
                                        String des = nombres.get(sp.getSelectedItemPosition());
                                        final String ur = urls.get(sp.getSelectedItemPosition());
                                        Log.d("Stream", "URL -> " + ur);
                                        switch (des.toLowerCase()) {
                                            case "zippyshare":
                                                MainStates.setZippyState(DownloadTask.STREAMING, ur, button, DownState);
                                                web.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        web.loadUrl(ur);
                                                    }
                                                });
                                                d.dismiss();
                                                break;
                                            case "mega":
                                                d.dismiss();
                                                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ur)));
                                                MainStates.setProcessing(false, null);
                                                break;
                                            default:
                                                StreamManager.Stream(context, eid, ur);
                                                MainStates.setProcessing(false, null);
                                                showDownload(button);
                                                d.dismiss();
                                                break;
                                        }

                                    }

                                    @Override
                                    public void onNegative(MaterialDialog dialog) {
                                        super.onNegative(dialog);
                                        MainStates.setProcessing(false, null);
                                        showDownload(button);
                                        d.dismiss();
                                    }
                                })
                                .cancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        d.dismiss();
                                        MainStates.setProcessing(false, null);
                                        showDownload(button);
                                    }
                                })
                                .build();
                        sp = (Spinner) d.getCustomView().findViewById(R.id.spinner_down);
                        sp.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, nombres));
                        d.show();
                    } catch (Exception e) {
                        MainStates.setProcessing(false, null);
                        showDownload(button);
                        e.printStackTrace();
                        Toaster.toast("Error en JSON");
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    super.onFailure(statusCode, headers, responseString, throwable);
                    MainStates.setProcessing(false, null);
                    Log.e("Error", "Download", throwable);
                    showDownload(button);
                    if (parser.getUrlCached(eid).equals("null")) {
                        if (FileUtil.existDir()) {
                            Toaster.toast("Error, no se escuentra el directorio, porfavor abralo manualmente y reintente");
                        } else {
                            Toaster.toast("Error, no se escuentra anime en el directorio, porfavor abralo manualmente para actualizar");
                        }
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    MainStates.setProcessing(false, null);
                    Log.e("Error", "Download", throwable);
                    showDownload(button);
                }
            });
            Looper.loop();
            return null;
        }
    }

}