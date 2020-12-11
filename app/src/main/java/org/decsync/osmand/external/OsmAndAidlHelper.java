package org.decsync.osmand.external;

// Stripped and slightly modified from main.java.net.osmand.osmandapidemo
// <https://github.com/osmandapp/osmand-api-demo/blob/2408f5109f596dc9171fed5a59b8363425ff306f/OsmAnd-api-sample/app/src/main/java/net/osmand/osmandapidemo/OsmAndAidlHelper.java>

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import net.osmand.aidlapi.IOsmAndAidlInterface;
import net.osmand.aidlapi.favorite.AFavorite;
import net.osmand.aidlapi.favorite.AddFavoriteParams;
import net.osmand.aidlapi.favorite.RemoveFavoriteParams;
import net.osmand.aidlapi.favorite.UpdateFavoriteParams;
import net.osmand.aidlapi.favorite.group.AFavoriteGroup;
import net.osmand.aidlapi.favorite.group.AddFavoriteGroupParams;
import net.osmand.aidlapi.favorite.group.RemoveFavoriteGroupParams;
import net.osmand.aidlapi.favorite.group.UpdateFavoriteGroupParams;

public class OsmAndAidlHelper {

    private static final String OSMAND_PLUS_PACKAGE_NAME = "net.osmand.plus";
    private static final String OSMAND_PACKAGE_NAME = OSMAND_PLUS_PACKAGE_NAME;
    private static final String TAG = "OsmAndAidlHelper";

    private final Context mContext;
    private final OnOsmandMissingListener mOsmandMissingListener;
    private IOsmAndAidlInterface mIOsmAndAidlInterface;

    public interface OnOsmandMissingListener {
        void osmandMissing();
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mIOsmAndAidlInterface = IOsmAndAidlInterface.Stub.asInterface(service);
            Log.d(TAG, "OsmAnd service connected");
        }
        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mIOsmAndAidlInterface = null;
            Log.d(TAG, "OsmAnd service disconnected");
        }
    };

    public OsmAndAidlHelper(Context context, OnOsmandMissingListener listener) {
        this.mContext = context;
        this.mOsmandMissingListener = listener;
        bindService();
    }

    private boolean bindService() {
        if (mIOsmAndAidlInterface == null) {
            Intent intent = new Intent("net.osmand.aidl.OsmandAidlServiceV2");
            intent.setPackage(OSMAND_PACKAGE_NAME);
            boolean res = mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            if (res) {
                return true;
            } else {
                mOsmandMissingListener.osmandMissing();
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * Add favorite group with given params.
     *
     * @param name    - group name.
     * @param color   - group color. Can be one of: "red", "orange", "yellow",
     *                "lightgreen", "green", "lightblue", "blue", "purple", "pink", "brown".
     * @param visible - group visibility.
     */
    public boolean addFavoriteGroup(String name, String color, boolean visible) {
        if (mIOsmAndAidlInterface != null) {
            try {
                AFavoriteGroup favoriteGroup = new AFavoriteGroup(name, color, visible);
                return mIOsmAndAidlInterface.addFavoriteGroup(new AddFavoriteGroupParams(favoriteGroup));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Update favorite group with given params.
     *
     * @param namePrev    - group name (current).
     * @param colorPrev   - group color (current).
     * @param visiblePrev - group visibility (current).
     * @param nameNew     - group name (new).
     * @param colorNew    - group color (new).
     * @param visibleNew  - group visibility (new).
     */
    public boolean updateFavoriteGroup(String namePrev, String colorPrev, boolean visiblePrev,
                                       String nameNew, String colorNew, boolean visibleNew) {
        if (mIOsmAndAidlInterface != null) {
            try {
                AFavoriteGroup favoriteGroupPrev = new AFavoriteGroup(namePrev, colorPrev, visiblePrev);
                AFavoriteGroup favoriteGroupNew = new AFavoriteGroup(nameNew, colorNew, visibleNew);
                return mIOsmAndAidlInterface.updateFavoriteGroup(new UpdateFavoriteGroupParams(favoriteGroupPrev, favoriteGroupNew));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Remove favorite group with given name.
     *
     * @param name - name of favorite group.
     */
    public boolean removeFavoriteGroup(String name) {
        if (mIOsmAndAidlInterface != null) {
            try {
                AFavoriteGroup favoriteGroup = new AFavoriteGroup(name, "", false);
                return mIOsmAndAidlInterface.removeFavoriteGroup(new RemoveFavoriteGroupParams(favoriteGroup));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Add favorite at given location with given params.
     *
     * @param lat         - latitude.
     * @param lon         - longitude.
     * @param name        - name of favorite item.
     * @param description - description of favorite item.
     * @param category    - category of favorite item.
     * @param color       - color of favorite item. Can be one of: "red", "orange", "yellow",
     *                    "lightgreen", "green", "lightblue", "blue", "purple", "pink", "brown".
     * @param visible     - should favorite item be visible after creation.
     */
    public boolean addFavorite(double lat, double lon, String name, String description, String address,
                               String category, String color, boolean visible) {
        if (mIOsmAndAidlInterface != null) {
            try {
                AFavorite favorite = new AFavorite(lat, lon, name, description,address, category, color, visible);
                return mIOsmAndAidlInterface.addFavorite(new AddFavoriteParams(favorite));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Update favorite at given location with given params.
     *
     * @param latPrev        - latitude (current favorite).
     * @param lonPrev        - longitude (current favorite).
     * @param namePrev       - name of favorite item (current favorite).
     * @param categoryPrev   - category of favorite item (current favorite).
     * @param latNew         - latitude (new favorite).
     * @param lonNew         - longitude (new favorite).
     * @param nameNew        - name of favorite item (new favorite).
     * @param descriptionNew - description of favorite item (new favorite).
     * @param categoryNew    - category of favorite item (new favorite). Use only to create a new category,
     *                       not to update an existing one. If you want to  update an existing category,
     *                       use the {@link #updateFavoriteGroup(String, String, boolean, String, String, boolean)} method.
     * @param colorNew       - color of new category. Can be one of: "red", "orange", "yellow",
     *                       "lightgreen", "green", "lightblue", "blue", "purple", "pink", "brown".
     * @param visibleNew     - should new category be visible after creation.
     */
    public boolean updateFavorite(double latPrev, double lonPrev, String namePrev, String categoryPrev,
                                  double latNew, double lonNew, String nameNew, String descriptionNew,
                                  String categoryNew, String colorNew, boolean visibleNew) {
        if (mIOsmAndAidlInterface != null) {
            try {
                AFavorite favoritePrev = new AFavorite(latPrev, lonPrev, namePrev, "","", categoryPrev, "", false);
                AFavorite favoriteNew = new AFavorite(latNew, lonNew, nameNew, descriptionNew,"", categoryNew, colorNew, visibleNew);
                return mIOsmAndAidlInterface.updateFavorite(new UpdateFavoriteParams(favoritePrev, favoriteNew));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Remove favorite at given location with given params.
     *
     * @param lat      - latitude.
     * @param lon      - longitude.
     * @param name     - name of favorite item.
     * @param category - category of favorite item.
     */
    public boolean removeFavorite(double lat, double lon, String name, String category) {
        if (mIOsmAndAidlInterface != null) {
            try {
                AFavorite favorite = new AFavorite(lat, lon, name, "","", category, "", false);
                return mIOsmAndAidlInterface.removeFavorite(new RemoveFavoriteParams(favorite));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}