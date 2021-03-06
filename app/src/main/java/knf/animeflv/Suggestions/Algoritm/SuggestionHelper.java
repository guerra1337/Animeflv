package knf.animeflv.Suggestions.Algoritm;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import knf.animeflv.Directorio.AnimeClass;
import knf.animeflv.Directorio.DB.DirectoryDB;
import knf.animeflv.Directorio.DB.DirectoryHelper;
import knf.animeflv.Favorites.FavoriteHelper;
import knf.animeflv.JsonFactory.BaseGetter;
import knf.animeflv.JsonFactory.JsonTypes.ANIME;
import knf.animeflv.JsonFactory.JsonTypes.DIRECTORIO;
import knf.animeflv.Random.AnimeObject;

/**
 * Created by Jordy on 21/04/2017.
 */

public class SuggestionHelper {
    public static final int DIRECTORY_ERROR_CODE = 0;
    public static final int NO_INFO_CODE = 1;

    public static void register(final Context context, final String aid, final SuggestionAction value) {
        BaseGetter.getJson(context, new ANIME(aid), new BaseGetter.AsyncInterface() {
            @Override
            public void onFinish(String json) {
                try {
                    if (json.equals("null"))
                        throw new Exception("Json is null!!!");
                    JSONObject object = new JSONObject(json);
                    String[] generos = object.getString("generos").split(",");
                    for (String g : generos) {
                        registerPoints(context, g.trim(), value.value, Integer.parseInt(aid) <= 1200);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("Suggestion", "Error registering " + aid + " with value " + value.value);
                }
            }
        });
    }

    public static void getSuggestions(final Context context, final SuggestionCreate suggestionCreate) {
        BaseGetter.getJson(context, new DIRECTORIO(), new BaseGetter.AsyncProgressDBInterface() {
            @Override
            public void onFinish(List<AnimeClass> l) {
                try {
                    if (!DirectoryHelper.get(context).isDirectoryValid())
                        throw new FileNotFoundException("Directory null!!!");
                    List<SuggestionDB.Suggestion> list = getGenresCount(context);
                    if (list.size() < 3) throw new IllegalStateException("List minor of 3");
                    Collections.sort(list, new SuggestionCompare());
                    List<DirectoryDB.DirectoryItem> items = new ArrayList<>();
                    items.addAll(new DirectoryDB(context).getAllAnimesCGen(list.get(0).name, true));
                    items.addAll(new DirectoryDB(context).getAllAnimesCGen(list.get(1).name, true));
                    items.addAll(new DirectoryDB(context).getAllAnimesCGen(list.get(2).name, true));
                    HashSet<DirectoryDB.DirectoryItem> hashSet = new HashSet<>(items);
                    items.clear();
                    items.addAll(hashSet);
                    Log.e("Suggestions", "From dir: " + items.size());
                    List<AnimeObject> finalList = PreferenceManager.getDefaultSharedPreferences(context).getString("sug_order", "0").equals("0") ? searchNew(context, list, items) : searchOld(context, list, items);
                    Log.e("Suggestions", "Found " + finalList.size() + " Animes");
                    suggestionCreate.onListCreated(finalList, list);
                } catch (FileNotFoundException fe) {
                    fe.printStackTrace();
                    suggestionCreate.onError(DIRECTORY_ERROR_CODE);
                } catch (IllegalStateException ise) {
                    ise.printStackTrace();
                    suggestionCreate.onError(NO_INFO_CODE);
                } catch (Exception e) {
                    e.printStackTrace();
                    suggestionCreate.onError(-1);
                }
            }

            @Override
            public void onProgress(int progress) {

            }

            @Override
            public void onError(Throwable throwable) {

            }
        });
    }

    private static List<AnimeObject> searchNew(Context context, List<SuggestionDB.Suggestion> list, List<DirectoryDB.DirectoryItem> array) throws Exception {
        List<AnimeObject> abc = new ArrayList<>();
        List<AnimeObject> ab = new ArrayList<>();
        List<AnimeObject> ac = new ArrayList<>();
        List<AnimeObject> bc = new ArrayList<>();
        List<AnimeObject> a_b_c = new ArrayList<>();
        List<String> blacklist = getExcluded(context);
        Collections.reverse(array);
        for (DirectoryDB.DirectoryItem item : array) {
            String genres = item.genres;
            String aid = item.aid;
            boolean skip = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("skip_favs", true) && FavoriteHelper.isFav(context, aid);
            if (!skip)
                if (genres.contains(list.get(0).name) || genres.contains(list.get(1).name) || genres.contains(list.get(2).name) && !isExcluded(blacklist, genres)) {
                    AnimeObject current = new AnimeObject(aid, item.name, item.type);
                    if (genres.contains(list.get(0).name) && genres.contains(list.get(1).name) && genres.contains(list.get(2).name)) {
                        abc.add(current);
                    } else if (genres.contains(list.get(0).name) && genres.contains(list.get(1).name)) {
                        ab.add(current);
                    } else if (genres.contains(list.get(0).name) && genres.contains(list.get(2).name)) {
                        ac.add(current);
                    } else if (genres.contains(list.get(1).name) && genres.contains(list.get(2).name)) {
                        bc.add(current);
                    } else {
                        a_b_c.add(current);
                    }
                }
        }
        Collections.sort(abc, new SuggestionNameCompare());
        Collections.sort(ab, new SuggestionNameCompare());
        Collections.sort(ac, new SuggestionNameCompare());
        Collections.sort(bc, new SuggestionNameCompare());
        Collections.sort(a_b_c, new SuggestionNameCompare());
        List<AnimeObject> finalList = new ArrayList<>();
        finalList.add(new AnimeObject(getSuggestionGenres(list.get(0).name, list.get(1).name, list.get(2).name), true, new ArrayList<AnimeObject>()));
        finalList.addAll(abc);
        finalList.add(new AnimeObject(getSuggestionGenres(list.get(0).name, list.get(1).name), true, new ArrayList<AnimeObject>()));
        finalList.addAll(ab);
        finalList.add(new AnimeObject(getSuggestionGenres(list.get(0).name, list.get(2).name), true, new ArrayList<AnimeObject>()));
        finalList.addAll(ac);
        finalList.add(new AnimeObject(getSuggestionGenres(list.get(1).name, list.get(2).name), true, new ArrayList<AnimeObject>()));
        finalList.addAll(bc);
        finalList.add(new AnimeObject(getSuggestionGenresFinals(list.get(0).name, list.get(1).name, list.get(2).name), true, new ArrayList<AnimeObject>()));
        finalList.addAll(a_b_c);
        return finalList;
    }

    private static List<AnimeObject> searchOld(Context context, List<SuggestionDB.Suggestion> list, List<DirectoryDB.DirectoryItem> array) throws Exception {
        List<AnimeObject> abc = new ArrayList<>();
        List<AnimeObject> ab = new ArrayList<>();
        List<AnimeObject> ac = new ArrayList<>();
        List<AnimeObject> bc = new ArrayList<>();
        List<AnimeObject> a_b_c = new ArrayList<>();
        List<String> blacklist = getExcluded(context);
        for (DirectoryDB.DirectoryItem item : array) {
            String genres = item.genres;
            String aid = item.aid;
            boolean skip = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("skip_favs", true) && FavoriteHelper.isFav(context, aid);
            if (!skip)
                if (genres.contains(list.get(0).name) || genres.contains(list.get(1).name) || genres.contains(list.get(2).name) && !isExcluded(blacklist, genres)) {
                    AnimeObject current = new AnimeObject(aid, item.name, item.type);
                    if (genres.contains(list.get(0).name) && genres.contains(list.get(1).name) && genres.contains(list.get(2).name)) {
                        abc.add(current);
                    } else if (genres.contains(list.get(0).name) && genres.contains(list.get(1).name)) {
                        ab.add(current);
                    } else if (genres.contains(list.get(0).name) && genres.contains(list.get(2).name)) {
                        ac.add(current);
                    } else if (genres.contains(list.get(1).name) && genres.contains(list.get(2).name)) {
                        bc.add(current);
                    } else {
                        a_b_c.add(current);
                    }
                }
        }
        Collections.sort(abc, new SuggestionNameCompare());
        Collections.sort(ab, new SuggestionNameCompare());
        Collections.sort(ac, new SuggestionNameCompare());
        Collections.sort(bc, new SuggestionNameCompare());
        Collections.sort(a_b_c, new SuggestionNameCompare());
        List<AnimeObject> finalList = new ArrayList<>();
        finalList.add(new AnimeObject(getSuggestionGenres(list.get(0).name, list.get(1).name, list.get(2).name), true, new ArrayList<AnimeObject>()));
        finalList.addAll(abc);
        finalList.add(new AnimeObject(getSuggestionGenres(list.get(0).name, list.get(1).name), true, new ArrayList<AnimeObject>()));
        finalList.addAll(ab);
        finalList.add(new AnimeObject(getSuggestionGenres(list.get(0).name, list.get(2).name), true, new ArrayList<AnimeObject>()));
        finalList.addAll(ac);
        finalList.add(new AnimeObject(getSuggestionGenres(list.get(1).name, list.get(2).name), true, new ArrayList<AnimeObject>()));
        finalList.addAll(bc);
        finalList.add(new AnimeObject(getSuggestionGenresFinals(list.get(0).name, list.get(1).name, list.get(2).name), true, new ArrayList<AnimeObject>()));
        finalList.addAll(a_b_c);
        return finalList;
    }

    private static boolean isExcluded(List<String> blacklist, String genres) {
        if (blacklist.size() > 0) {
            for (String genre : genres.split(",")) {
                if (blacklist.contains(genre.trim()))
                    return true;
            }
            return false;
        } else {
            return false;
        }
    }

    private static String getSuggestionGenres(String... genres) {
        StringBuilder builder = new StringBuilder("");
        for (String genre : genres) {
            builder.append(genre)
                    .append(", ");
        }
        String g = builder.toString().trim();
        return g.endsWith(",") ? g.substring(0, g.lastIndexOf(",")) : g;
    }

    private static String getSuggestionGenresFinals(String... genres) {
        StringBuilder builder = new StringBuilder("");
        for (String genre : genres) {
            builder.append(genre)
                    .append(" || ");
        }
        String g = builder.toString().trim();
        return g.endsWith(" ||") ? g.substring(0, g.lastIndexOf(" ")) : g;
    }

    public static void clear(Context context) {
        SuggestionDB.get(context).reset();
    }

    private static List<SuggestionDB.Suggestion> getGenresCount(Context context) {
        return SuggestionDB.get(context).getSuggestions(getExcluded(context));
    }

    public static List<String> getExcluded(Context context) {
        String[] excludedList = PreferenceManager.getDefaultSharedPreferences(context).getString("suggestion_blacklist", "").split(";");
        return Arrays.asList(excludedList);
    }

    public static void saveExcluded(Context context, String excluded) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("suggestion_blacklist", excluded).apply();
    }

    private static void registerPoints(Context context, String genre, int value, boolean isOld) {
        Log.e("Suggestions Algoritm", "Add " + value + " to " + genre + " isOld: " + isOld);
        SuggestionDB.get(context).register(genre, value, isOld);
    }

    public interface SuggestionCreate {
        void onListCreated(List<AnimeObject> aids, List<SuggestionDB.Suggestion> suggestions);

        void onError(int code);
    }
}
