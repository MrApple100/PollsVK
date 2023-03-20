package com.example.comparatorforvkpolls;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKAccessTokenTracker;
import com.vk.sdk.VKSdk;



public class Application extends android.app.Application{
    @Override
    public void onCreate() {
        super.onCreate();
        VKSdk.initialize(this);
        //если мы уже вошли то входить больше не нужно
        tokenTracker.startTracking();
    }

    private VKAccessTokenTracker tokenTracker = new VKAccessTokenTracker() {
        @Override
        public void onVKAccessTokenChanged(@Nullable VKAccessToken oldToken, @Nullable VKAccessToken newToken) {
            if(newToken==null){
                Intent intent=new Intent(Application.this,MainActivity.class);
                //Если выполняются какие либо процессы, то они сместятся в фон, а это активити будет сверху всех остальных
                //Если мы и так на самом вверху по приоритету выполнения, то старые таски будут закрыты и данное активити откроется
                //в каком то старом как в новом
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        }
    };


}
