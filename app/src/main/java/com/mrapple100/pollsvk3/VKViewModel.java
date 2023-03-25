package com.mrapple100.pollsvk3;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.vk.sdk.VKAccessToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.stream.Collectors;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class VKViewModel extends ViewModel {
    public MutableLiveData<String> textScreenName = new MutableLiveData<>();
    public MutableLiveData<String> textIdPoll = new MutableLiveData<>();
    public MutableLiveData<String> textListUsers = new MutableLiveData<>();
    public MutableLiveData<String> textResult = new MutableLiveData<>();

    public void VKClick(){
        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                subscriber.onNext(getIdOwner(textScreenName.getValue()));
                subscriber.onCompleted();
            }
        }).onErrorResumeNext(new Func1<Throwable, Observable<? extends String>>() {
            @Override
            public Observable<? extends String> call(Throwable throwable) {
                return Observable.just("0");
            }
        }).map(new Func1<String, String>() {
            @Override
            public String call(String jsonOwner) {
                String idOwner = null;
                try {
                    idOwner = new JSONObject(jsonOwner).getJSONArray("response").getJSONObject(0).getString("id");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return idOwner;
            }
        }).map(new Func1<String, String[]>() {
            @Override
            public String[] call(String idOwner) {
                String response = getPollResponse(textIdPoll.getValue(),idOwner);
               // System.out.println(response);

                JSONArray answers = null;

                try {
                    answers = new JSONObject(response).getJSONObject("response").getJSONArray("answers");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String[] ans_ids = new String[answers.length()+1];
                for(int i=0;i<answers.length();i++){
                    try {
                        ans_ids[i] = answers.getJSONObject(i).getString("id");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                ans_ids[ans_ids.length-1] = idOwner;
                return ans_ids;
            }
        }).map(new Func1<String[], String[]>() {
                    @Override
                    public String[] call(String[] answers_id) {
                        String[] responses = new String[answers_id.length-1];
                        for(int i=0;i<responses.length;i++){
                            responses[i] = getVotes(textIdPoll.getValue(), answers_id[answers_id.length-1],answers_id[i]);
                        }
                        System.out.println(responses[0]);
                        return responses;
                    }
                }).map(new Func1<String[], TreeMap<Long,Integer>>() {
                    @Override
                    public TreeMap<Long, Integer> call(String[] responses) {
                        TreeMap<Long,Integer> votes = new TreeMap<>();
                        for(int i=0;i<responses.length;i++){
                            String[] arrayIds = null;

                            try {
                                JSONObject users = new JSONObject(responses[i]).getJSONArray("response") .getJSONObject(0).getJSONObject("users");
                                arrayIds = users.getString("items").substring(1,users.getString("items").length()-1).split(",");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            for(int j=0;j<arrayIds.length;j++){
                                votes.put(Long.parseLong(arrayIds[j]),i);
                            }
                        }
                        return votes;
                    }
                }).map(new Func1<TreeMap<Long, Integer>, ArrayList<Integer>>() {
                    @Override
                    public ArrayList<Integer> call(TreeMap<Long, Integer> votes) {
                        String[] listsneed = textListUsers.getValue().split("vk.com");
                        System.out.println("UNITL "+Arrays.toString(listsneed));
                        ArrayList<String> listsneed2 =(ArrayList<String>) Arrays.stream(listsneed).map(i -> i.split("https://")[0]).collect(Collectors.toList());
                        System.out.println("AFTER "+listsneed2);
                        ArrayList<String> needsbody = new ArrayList<String>(listsneed2);
                        needsbody =(ArrayList<String>) needsbody.stream().map(i -> {
                            String[] str = i.split("/");
                            return str[str.length-1].trim();
                        }).collect(Collectors.toList());

                        System.out.println("NEEDBody "+needsbody);
                        ArrayList<Integer> votesdigit = new ArrayList<>();

                        for(int i=0;i<needsbody.size();i++){
                            long idvote = 0L;

                            try {
                                idvote = new JSONObject(getIdOwner(needsbody.get(i))).getJSONArray("response").getJSONObject(0).getLong("id");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if(votes.containsKey(idvote)){
                                int dig = votes.get(idvote);
                                while(votesdigit.size()-1<dig){
                                    votesdigit.add(0);
                                }
                                votesdigit.set(dig,votesdigit.get(dig)+1);
                            }
                            if(i%3==0){
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        return votesdigit;
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ArrayList<Integer>>() {
                    @Override
                    public void call(ArrayList<Integer> votesdigit) {
                        textResult.setValue(votesdigit.toString());
                    }
                });
    }


    private String getBase(String url){
        URL obj =null;
        try {
            obj = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HttpURLConnection connection=null;
        try {
            connection =(HttpURLConnection) obj.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            connection.setRequestMethod("GET");
        } catch (ProtocolException e) {
            e.printStackTrace();
        }
        BufferedReader in =null;
        try {
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String inputLine = null;
        StringBuffer response = new StringBuffer();
        while(true){
            try {
                if((inputLine = in.readLine()) == null) break;
            } catch (IOException e) {
                e.printStackTrace();
            }
            response.append(inputLine);
        }

        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response.toString();
    }

    private String getIdOwner(String screenName){
        String url = "https://api.vk.com/method/users.get?user_ids="+screenName+"&access_token="+ VKAccessToken.currentToken().accessToken+"&v=5.131";
        return getBase(url);
    }
    private String getPollResponse(String idPoll,String idOwner){
        String url = "https://api.vk.com/method/polls.getById?poll_id="+idPoll+"&owner_id="+idOwner+"&access_token="+VKAccessToken.currentToken().accessToken+"&v=5.131";
        return getBase(url);
    }
    private String getVotes(String idPoll,String idOwner,String idAnswer){
        String url = "https://api.vk.com/method/polls.getVoters?poll_id="+idPoll+"&owner_id="+idOwner+"&answer_ids="+idAnswer+"&access_token="+VKAccessToken.currentToken().accessToken+"&v=5.131";
        return getBase(url);
    }




}
