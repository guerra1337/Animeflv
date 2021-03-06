package knf.animeflv.PlayBack;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.view.Menu;

import com.connectsdk.device.DevicePicker;

import es.munix.multidisplaycast.CastControlsActivity;
import es.munix.multidisplaycast.CastManager;
import es.munix.multidisplaycast.interfaces.CastListener;
import es.munix.multidisplaycast.interfaces.PlayStatusListener;
import knf.animeflv.Directorio.DB.DirectoryHelper;
import knf.animeflv.Parser;
import knf.animeflv.Seen.SeenManager;
import knf.animeflv.TaskType;
import knf.animeflv.Utils.ThemeUtils;
import knf.animeflv.history.adapter.HistoryHelper;
import xdroid.toaster.Toaster;

public class CastPlayBackManager implements CastListener, PlayStatusListener {
    private static final String TAG = "CastPlayBackManager";
    private static CastPlayBackManager manager;
    private Context activity;
    private boolean isConnected = false;
    private PlayBackInterface playBackInterface;
    private String castingEid = "null";

    public CastPlayBackManager(Context activity) {
        this.activity = activity;
        CastManager.getInstance().setTheme(ThemeUtils.isAmoled(activity) ? DevicePicker.Theme.DARK : DevicePicker.Theme.LIGTH);
        CastManager.getInstance().setDiscoveryManager();
        CastManager.getInstance().setCastListener(TAG, this);
        CastManager.getInstance().setPlayStatusListener(TAG, this);
    }

    public static CastPlayBackManager get(Activity activity) {
        if (manager == null)
            manager = new CastPlayBackManager(activity);
        return manager;
    }

    public void setPlayBackListener(PlayBackInterface playBackListener) {
        this.playBackInterface = playBackListener;
    }

    public void registrerMenu(Menu menu, int id) {
        CastManager.getInstance().registerForActivity((Activity) activity, menu, id);
    }

    public void destroyManager() {
        CastManager.getInstance().unsetCastListener(TAG);
        CastManager.getInstance().unsetPlayStatusListener(TAG);
        CastManager.getInstance().onDestroy();
    }

    public boolean isDeviceConnected() {
        return isConnected || CastManager.getInstance().isConnected();
    }

    public void play(String url, String eid) {
        try {
            Looper.prepare();
        } catch (Exception e) {
        }
        if (isDeviceConnected()) {
            castingEid = eid;
            SeenManager.get(activity).setSeenState(eid, true);
            String[] semi = eid.replace("E", "").split("_");
            String aid = semi[0];
            String num = semi[1];
            HistoryHelper.addToList(activity, aid, DirectoryHelper.get(activity).getTitle(aid), num);
            CastManager.getInstance().playMedia(url, "video/mp4", DirectoryHelper.get(activity).getTitle(aid), "Capítulo " + num, getPreviewUrl());
        } else {
            isConnected = false;
            Toaster.toast("No hay dispositivos conectados");
        }
        try {
            Looper.loop();
        } catch (Exception e) {
        }
    }

    public void stop() {
        castingEid = "null";
        CastManager.getInstance().stop();
    }

    public String getCastingEid() {
        return castingEid;
    }

    public boolean isCasting(String eid) {
        return getCastingEid().equals(eid);
    }

    private String getPreviewUrl() {
        String base = new Parser().getBaseUrl(TaskType.NORMAL, activity).replace("api2.", "") + "/images/";
        return base + (ThemeUtils.isAmoled(activity) ? "preview_dark.png" : "preview_light.png");
    }

    @Override
    public void isConnected() {
        isConnected = true;
    }

    @Override
    public void isDisconnected() {
        isConnected = false;
    }

    @Override
    public void onPlayStatusChanged(int i) {
        switch (i) {
            case STATUS_START_PLAYING:
                if (playBackInterface != null)
                    playBackInterface.onPlay();
                CastControlsActivity.open(activity, ThemeUtils.getAcentColor(activity));
                break;
            case STATUS_FINISHED:
            case STATUS_STOPPED:
                castingEid = "null";
                if (playBackInterface != null)
                    playBackInterface.onStop();
                break;

            case STATUS_PAUSED:
                if (playBackInterface != null)
                    playBackInterface.onPause();
                break;

            case STATUS_NOT_SUPPORT_LISTENER:
                Toaster.toast("No soportado!!");
                if (playBackInterface != null)
                    playBackInterface.onExit();
                break;
        }
    }

    @Override
    public void onPositionChanged(long l) {

    }

    @Override
    public void onTotalDurationObtained(long l) {

    }

    @Override
    public void onSuccessSeek() {

    }

    public interface PlayBackInterface {
        void onPlay();

        void onPause();

        void onStop();

        void onExit();
    }
}
