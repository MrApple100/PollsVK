package com.mrapple100.pollsvk3;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;
import android.widget.Toast;

import com.mrapple100.pollsvk3.databinding.ActivityMainBinding;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.util.VKUtil;

public class MainActivity extends AppCompatActivity {

    private String[] scope = {VKScope.WALL,VKScope.FRIENDS,VKScope.GROUPS};
    VKViewModel vkViewModel;
    VkRepository vkRepository;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // setContentView(R.layout.activity_main);

        vkViewModel = ViewModelProviders.of(this).get(VKViewModel.class);

        vkRepository = new VkRepository(this);

        vkViewModel.textScreenName.setValue(vkRepository.loadValue("textScreenName"));
        vkViewModel.textIdPoll.setValue(vkRepository.loadValue("textIdPoll"));
        vkViewModel.textListUsers.setValue(vkRepository.loadValue("textListUsers"));


        ActivityMainBinding binding = DataBindingUtil.setContentView(this,R.layout.activity_main);
        VKSdk.login(this,scope);
        binding.setVm(vkViewModel);
        binding.setLifecycleOwner(this);

      //  String[]  fingerprint = VKUtil.getCertificateFingerprint(this,this.getPackageName());
      //  System.out.println(fingerprint[0]);
    }

    @Override
    protected void onPause() {
        super.onPause();
//        vkRepository.saveValue("textScreenName",vkViewModel.textScreenName.getValue());
//        vkRepository.saveValue("textIdPoll",vkViewModel.textIdPoll.getValue());
//        vkRepository.saveValue("textListUsers",vkViewModel.textListUsers.getValue());
//
//
//        Toast.makeText(this, "SAVE NICE!", Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

//        vkRepository.saveValue("textScreenName",vkViewModel.textScreenName.getValue());
//        vkRepository.saveValue("textIdPoll",vkViewModel.textIdPoll.getValue());
//        vkRepository.saveValue("textListUsers",vkViewModel.textListUsers.getValue());
//
//
//        Toast.makeText(this, "SAVE NICE!", Toast.LENGTH_SHORT).show();

    }
}