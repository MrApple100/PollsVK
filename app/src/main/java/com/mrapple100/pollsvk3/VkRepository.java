package com.mrapple100.pollsvk3;

import android.content.Context;
import android.content.SharedPreferences;

public class VkRepository {

    private SharedPreferences preferences;

    private String CONST_PREF = "PREF" ;

    public VkRepository(Context context) {
        this.preferences = context.getSharedPreferences(CONST_PREF,Context.MODE_PRIVATE);
    }
    public void saveValue(String key,String value){
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key,value);
        editor.apply();//commit immediately
    }
    public String loadValue(String key){
        return preferences.getString(key,"");
    }

}
