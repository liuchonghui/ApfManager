package tools.android.apfmanager.bean;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Ex extends HashMap<String, String> implements Serializable {
    private static final long serialVersionUID = 1L;

    private static Ex EMPTY;

    public static Ex EMPTY() {
        if (EMPTY == null) {
            EMPTY = new Ex();
            EMPTY.setLowDefList(new ArrayList<String>());
            EMPTY.setNormalDefList(new ArrayList<String>());
            EMPTY.setHightDefList(new ArrayList<String>());
            EMPTY.setSuperDefList(new ArrayList<String>());
            EMPTY.setDefinitionList(new ArrayList<String>());
        }
        return EMPTY;
    }

    public void setDefinition(String definition) {
        put("definition", definition);
    }

    public String getDefinition() {
        return get("definition");
    }

    public void setLowDefList(List<String> low_def_list) {
        put("low_def_list", new Gson().toJson(low_def_list));
    }

    public ArrayList<String> getLowDefList() {
        String str = get("low_def_list");
        try {
            if (str != null && str.length() > 0) {
                return new Gson().fromJson(str, new TypeToken<ArrayList<String>>() {
                }.getType());
            }
        } catch (Exception e) {
        }
        return new ArrayList<String>();
    }

    public void setSuperDefList(List<String> super_def_list) {
        put("super_def_list", new Gson().toJson(super_def_list));
    }

    public ArrayList<String> getSuperDefList() {
        String str = get("super_def_list");
        try {
            if (str != null && str.length() > 0) {
                return new Gson().fromJson(str, new TypeToken<ArrayList<String>>() {
                }.getType());
            }
        } catch (Exception e) {
        }
        return new ArrayList<String>();
    }

    public void setHightDefList(List<String> high_def_list) {
        put("high_def_list", new Gson().toJson(high_def_list));
    }

    public ArrayList<String> getHighDefList() {
        String str = get("high_def_list");
        try {
            if (str != null && str.length() > 0) {
                return new Gson().fromJson(str, new TypeToken<ArrayList<String>>() {
                }.getType());
            }
        } catch (Exception e) {
        }
        return new ArrayList<String>();
    }

    public void setNormalDefList(List<String> normal_def_list) {
        put("normal_def_list", new Gson().toJson(normal_def_list));
    }

    public ArrayList<String> getNormalDefList() {
        String str = get("normal_def_list");
        try {
            if (str != null && str.length() > 0) {
                return new Gson().fromJson(str, new TypeToken<ArrayList<String>>() {
                }.getType());
            }
        } catch (Exception e) {
        }
        return new ArrayList<String>();
    }

    public void setDefinitionList(List<String> definitionList) {
        put("definition_list", new Gson().toJson(definitionList));
    }

    public ArrayList<String> getDefinitionList() {
        String str = get("definition_list");
        try {
            if (str != null && str.length() > 0) {
                return new Gson().fromJson(str, new TypeToken<ArrayList<String>>() {
                }.getType());
            }
        } catch (Exception e) {
        }
        return new ArrayList<String>();
    }

    public void setAbility(int code) {
        put("ability", String.valueOf(code));
    }

    public int getAbility() {
        int ret = 0;
        String str = get("ability");
        try {
            ret = Integer.valueOf(str);
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public void setVer(String ver) {
        put("ver", ver);
    }

    public String getVer() {
        return get("ver");
    }
}
