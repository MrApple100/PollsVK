package com.example.comparatorforvkpolls;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.INotificationSideChannel;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;
import com.vk.sdk.util.VKUtil;

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
import java.util.HashMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private String[] scope = new String[]{VKScope.FRIENDS,VKScope.GROUPS,VKScope.WALL};
    private Button result;
    private TextView textresult;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Элементы интерфейса
        EditText idpoll = findViewById(R.id.idpoll);
        EditText screennameowner = findViewById(R.id.idowner);
        EditText screennamesneed = findViewById(R.id.listsneed);
        result = findViewById(R.id.result);
        textresult = findViewById(R.id.resulttext);
        //Логинимся
        VKSdk.login(this, scope);
        //Init clients
        //проверка приложения, что оно зарегистрировано по имени
        String[] fingerprints = VKUtil.getCertificateFingerprint(this, this.getPackageName());
        System.out.println(fingerprints[0]);

        System.out.println("ku "+5.131);
        //обработка на нажатие
        result.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                //создание последовательности обработки запроса
                Subscription subscription = Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> subscriber) {
                        //получаем текст из поля и отправляем запрос на получение id владельца опроса
                        subscriber.onNext( getidowner(screennameowner.getText().toString()));
                        //сообщает обсервеблу что отправка данных закончена
                        subscriber.onCompleted();
                    }
                }).onErrorResumeNext(new Func1<Throwable, Observable<? extends String>>() {
                    @Override
                    public Observable<? extends String> call(Throwable throwable) {
                        //если мы что то не так написали в айди пользователя, то чтобы не выдавать ошибку сделаем заглушку, 0 это мы
                        return Observable.just("0");
                    }
                }).map(new Func1<String, String>() {
                    @Override
                    public String call(String jsonowner) {//получение айди владельца
                        String idowner = null;
                        try {
                            //достаем из json айдишник
                            idowner = new JSONObject(jsonowner).getJSONArray("response").getJSONObject(0).getString("id");
                        } catch (JSONException jsonException) {
                            jsonException.printStackTrace();
                        }
                        return idowner;
                    }
                }).map(new Func1<String, String[]>() {
                    @Override
                    public String[] call(String idowner) {//отправка запроса на получени опроса
                        String responce = getpollresponce(idpoll.getText().toString(),idowner);

                        System.out.println(idowner);//вывод в консоль владельца
                        JSONObject responsebody = null;
                        String question = null;
                        JSONArray answers = null;
                        try {
                            System.out.println(responce);//вывод в консоль ответа об опросе
                            responsebody = new JSONObject(responce).getJSONObject("response");
                            question = responsebody.getString("question");//получение вопроса
                            answers = responsebody.getJSONArray("answers");//получение массива ответов
                        } catch (JSONException jsonException) {
                            jsonException.printStackTrace();
                        }
                        //создаем массив айдишников ответов
                        String[] ans_ids= new String[answers.length()+1];
                        //////////////////////// это нужно для дебага
                        StringBuilder text = new StringBuilder();//почему тут я использую стрингбилдер
                        text.append(question).append("\n\n");
                        for(int i=0;i<answers.length();i++){
                            try {
                                //заполняем массив айдишниками ответов
                                ans_ids[i] = answers.getJSONObject(i).getString("id");
                                ////////////////////////////////
                                text.append(answers.getJSONObject(i).getString("text"));
                                text.append(answers.getJSONObject(i).getString("votes")).append("\n");
                                ////////////////////////////////
                            } catch (JSONException jsonException) {
                                jsonException.printStackTrace();
                            }
                        }
                        ///////////////////////
                        ans_ids[ans_ids.length-1] =idowner;//добавим в конец айдишник владельца
                        //textresult.setText(text);
                        return ans_ids;
                    }
                }).map(new Func1<String[], String[]>() {
                    @Override
                    public String[] call(String[] answers_id) {//получаем массив голосов в виде пользователей на каждый ответ
                        String[] responses = new String[answers_id.length-1];
                        for(int i=0;i<answers_id.length-1;i++){
                           responses[i] = getvotes(idpoll.getText().toString(),answers_id[answers_id.length-1],answers_id[i]);
                        }
                        System.out.println(responses[0]);

                        return responses;//массив массива голосов
                    }
                }).map(new Func1<String[], TreeMap<Long,Integer>>() {
                    @Override
                    public TreeMap<Long, Integer> call(String[] responses) {//каждый список ответов мы будем хранить в дереве, так как в дальнейшем
                        // нам надо будет выборочно искать и удалять тех кто не нужон
                        TreeMap<Long,Integer> votes = new TreeMap<>();
                        for(int i=0;i<responses.length;i++){
                            String[] arrayids = null;
                            try {
                                JSONArray responsebody = new JSONObject(responses[i]).getJSONArray("response");
                                System.out.println(responsebody.getString(0));
                                JSONObject users =responsebody.getJSONObject(0).getJSONObject("users");
                                System.out.println(users);
                                //достаем массив всех голосов и добавляем в один массив
                                arrayids = users.getString("items").substring(1,users.getString("items").length()-1).split(",");
                            } catch (JSONException jsonException) {
                                jsonException.printStackTrace();
                            }
                            System.out.println(Arrays.asList(arrayids));
                            //в ключ мы заносим айдишник а в значение -номер за что проголосовал человек
                            for(int j=0;j<arrayids.length;j++){
                                    votes.put(Long.parseLong(arrayids[j]),i);

                            }
                        }
                        System.out.println(votes.toString()+"ku");
                        return votes;
                    }
                }).map(new Func1<TreeMap<Long, Integer>, ArrayList<Integer>>() {
                    @Override
                    public ArrayList<Integer> call(TreeMap<Long, Integer> votes) {
                        //Создаем массив имен которые нужно чтобы были
                        String[] listsneed = screennamesneed.getText().toString().split("\n");
                        System.out.println(Arrays.asList(listsneed));

                        ArrayList<String> needsbody = new ArrayList<String>(Arrays.asList(listsneed));
                        //делим каждую строку на строки через слэш и убераем пробелы по краям через трим
                        //тем самым получаем только айдишники которые нужно найти
                        needsbody = (ArrayList<String>) needsbody.stream().map(i ->{
                            String[] str = i.split("/");
                            return str[str.length-1].trim();
                        }).collect(Collectors.toList());
                        System.out.println(needsbody);

                        ArrayList<Integer> votesdigit = new ArrayList<>();

                        for(int i=0;i<needsbody.size();i++){
                            long idvote = 0L;
                            try {
                                //превращаем каждый наш айдишник в списке в правильный айдишник в виде чисел
                                idvote = new JSONObject(getidowner(needsbody.get(i))).getJSONArray("response").getJSONObject(0).getLong("id");
                            } catch (JSONException jsonException) {
                                jsonException.printStackTrace();
                            }
                            //проверяем есть ли в нашей структуре этот айдишник
                            if(votes.containsKey(idvote)){
                                int dig = votes.get(idvote);
                                while (votesdigit.size()-1<dig){
                                    votesdigit.add(0);
                                }
                                votesdigit.set(dig,votesdigit.get(dig)+1);
                            }
                            // то нужно так как вк может дать побашке за частый спам запросами
                            if(i%3==0) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        return votesdigit;
                    }
                }).subscribeOn(Schedulers.newThread())//говорим что все операции выполнять в другом потоке
                        .observeOn(AndroidSchedulers.mainThread())//а после выполнения вернуться в главный поток
                        .subscribe(new Action1<ArrayList<Integer>>() {
                            @Override
                            public void call(ArrayList<Integer> votesdigit) {
                                textresult.setText(votesdigit.toString());//выводим результат
                            }
                        });
                /*
                Handler handler = new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(@NonNull Message msg) {
                        String response = (String) msg.obj;
                        textresult.setText(response);
                        return true;
                    }
                });
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String jsonowner =  getidowner(screennameowner.getText().toString());
                        String idowner = null;
                        try {
                            idowner = new JSONObject(idowner).getString("id");
                        } catch (JSONException jsonException) {
                            jsonException.printStackTrace();
                        }
                        String response = getpollresponce(idpoll.getText().toString(),idowner);
                        System.out.println(response.toString());
                        Message message = new Message();
                        message.obj = response.toString();
                        handler.sendMessage(message);
                    }
                }).start();

                 */

// выведет json-ответ запроса


            /*


                TransportClient transportClient = new HttpTransportClient();
                VkApiClient vkClient = new VkApiClient(transportClient);
                VkStreamingApiClient streamingClient = new VkStreamingApiClient(transportClient);

                Integer appId = 7749640;
                UserActor actor = new UserActor(VKAccessToken.currentToken().userId.hashCode(), VKAccessToken.currentToken().accessToken);
               // https://api.vk.com/method/users.get?user_ids=210700286&fields=bdate&access_token=533bacf01e11f55b536a565b57531ac114461ae8736d6506a3&v=5.131
                //VKRequest request= new VKRequest("polls.getById",VKParameters.from("v","5.81","poll_id","672189780","fields","id"));

                try {
                    System.out.println(request.getPreparedRequest().getQuery());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                try {
                    GetByIdResponse getResponse = vkClient.polls().getById(actor,Integer.parseInt(idpoll.getText().toString())).fields("id").execute();
                    textresult.setText(getResponse.toString());

                } catch (ApiException e) {
                    e.printStackTrace();
                } catch (ClientException e) {
                    e.printStackTrace();
                }

                /*
                request.executeWithListener(new VKRequest.VKRequestListener() {
                    @Override
                    public void onComplete(VKResponse response) {
                        super.onComplete(response);
                        System.out.println(response);
                        VKApiPoll poll=(VKApiPoll) response.parsedModel;
                        System.out.println(poll.toString());
                        textresult.setText(poll.toString());

                    }

                    @Override
                    public void onError(VKError error) {
                        super.onError(error);
                        System.out.println(error);
                        textresult.setText(error.toString());

                    }
                });

                 */


            }
        });


    }


//обработка перехода из одной активити в другой
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                Toast.makeText(getApplicationContext(),"hi_result",Toast.LENGTH_LONG).show();

            }

            @Override
            public void onError(VKError error) {
                Toast.makeText(getApplicationContext(),"hi_Error",Toast.LENGTH_LONG).show();
            }
        }))
            super.onActivityResult(requestCode, resultCode, data);

    }
    //запрос на получение опроса
    public String getpollresponce(String idpoll, String idowner){
        // формируют url запроса
        String url = "https://api.vk.com/method/polls.getById?poll_id="+idpoll+"&owner_id="+idowner+"&access_token="+VKAccessToken.currentToken().accessToken+"&v=5.131";
        System.out.println(url);
        //создаем юрл
        URL obj = null;
        try {
            obj = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        //устанавливаем соединение по http
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) obj.openConnection();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
// из документации: параметры могут передаваться как методом GET, так и POST. Если вы будете передавать большие данные (больше 2 килобайт), следует использовать POST.
        try {
            connection.setRequestMethod("GET");
        } catch (ProtocolException e) {
            e.printStackTrace();
        }
// посылаем запрос и сохраняем ответ
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        String inputLine = null;
        StringBuffer response = new StringBuffer();
        //накапливаем ответ
        while (true) {
            try {
                if (!((inputLine = in.readLine()) != null)) break;
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            response.append(inputLine);
        }
        //закрываем связь
        try {
            in.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return response.toString();
    }
    public String getvotes(String idpoll, String idowner,String idanswer){
        // формируют url запроса
        String url = "https://api.vk.com/method/polls.getVoters?poll_id="+idpoll+"&owner_id="+idowner+"&answer_ids="+idanswer+"&access_token="+VKAccessToken.currentToken().accessToken+"&v=5.131";
        System.out.println(url);
        URL obj = null;
        try {
            obj = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) obj.openConnection();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
// из документации: параметры могут передаваться как методом GET, так и POST. Если вы будете передавать большие данные (больше 2 килобайт), следует использовать POST.
        try {
            connection.setRequestMethod("GET");
        } catch (ProtocolException e) {
            e.printStackTrace();
        }
// посылаем запрос и сохраняем ответ
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        String inputLine = null;
        StringBuffer response = new StringBuffer();
        while (true) {
            try {
                if (!((inputLine = in.readLine()) != null)) break;
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            response.append(inputLine);
        }
        try {
            in.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return response.toString();
    }
    private String getidowner(String screenname) {
        // формируют url запроса
        String url = "https://api.vk.com/method/users.get?user_ids="+screenname+"&access_token="+VKAccessToken.currentToken().accessToken+"&v=5.131";
        System.out.println(url);
        URL obj = null;
        try {
            obj = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) obj.openConnection();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
// из документации: параметры могут передаваться как методом GET, так и POST. Если вы будете передавать большие данные (больше 2 килобайт), следует использовать POST.
        try {
            connection.setRequestMethod("GET");
        } catch (ProtocolException e) {
            e.printStackTrace();
        }
// посылаем запрос и сохраняем ответ
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        String inputLine = null;
        StringBuffer response = new StringBuffer();
        while (true) {
            try {
                if (!((inputLine = in.readLine()) != null)) break;
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            response.append(inputLine);
        }
        try {
            in.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return response.toString();
    }
}