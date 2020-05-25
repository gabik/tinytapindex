package net.kazav.tinytapindex;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ContentActivity extends AppCompatActivity implements OnCompleteListener {
    private String TAG = "TinyTapIndex - GET_CONTENT";
    private int language, age, category;
    private ArrayList<String> need_to_download;
    private dataAdapter adapter;
    private View data_view;
    private FirebaseFirestore db;
    private String username;
    private Map<String, String> excludelist;
    private boolean loaded;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loaded = false;
        setContentView(R.layout.activity_content_loading);
        Bundle data = getIntent().getExtras();
        language = data.getInt("language");
        category = data.getInt("category");
        age = data.getInt("age");
        username = data.getString("username");
        db = FirebaseFirestore.getInstance();
        db.collection("exclude").document(username).get().addOnCompleteListener(this);
        final LocalDateTime now = LocalDateTime.now();
        db.collection("search").document(username).set(new HashMap<String, String>(){{
            put(now.toString(), "lang=" + language + ", age=" + age + ", cat=" + category);
        }}, SetOptions.merge());
        excludelist = new HashMap<>();
    }

    private void stop_loading(String data) {
        LayoutInflater inflater = getLayoutInflater();
        data_view = inflater.inflate(R.layout.activity_content_data, null);
        need_to_download = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(data);
            ArrayList<JSONObject> my_data = new ArrayList<>();
            for (int i=0 ; i<jsonArray.length(); i++) {
                try {
                    JSONObject cur_json = ((JSONObject) jsonArray.get(i));
                    if (excludelist.containsKey((Integer.toString(cur_json.getInt("id"))))) {
                        Log.w(TAG, "Excluding: " + cur_json.toString());
                        continue;
                    }
                    String img = ((JSONObject) jsonArray.get(i)).getString("cover");
                    img = img.split("/")[3];
                    cur_json.put("img", img);
                    File cur_pic = new File(getFilesDir(),  img + ".png");
                    //Log.i(TAG, "Checking: " + img);
                    if (!cur_pic.exists()) {
                        need_to_download.add(img);
                        //Log.i(TAG, "Not exists");
                    } else {
                        need_to_download.add("");
                    }
                    my_data.add(cur_json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            adapter = new dataAdapter(my_data, this);
        } catch (JSONException e) {
            Log.e(TAG, "Cannot parse data");
            e.printStackTrace();
        }
        new get_imgs((TextView) findViewById(R.id.txtprogress)).execute();
        show_data_list();
    }

    void show_data_list() {
        RecyclerView rview = data_view.findViewById(R.id.data_list_view);
        rview.setLayoutManager(new GridLayoutManager(this, 3));
        rview.setAdapter(adapter);
        setContentView(data_view);
        loaded = true;
    }

    void download_img(String img) {
        if (img.equals("")) return;
        try {
            String img_url = "https://content.tinytap.it/" + img + "/shareImage.png";
            File cur_pic = new File(getFilesDir(), img + ".png");
            Log.i(TAG, "Downloading: " + img_url);
            URL url = new URL(img_url);
            InputStream input = url.openStream();
            OutputStream output = new FileOutputStream(cur_pic);
            byte[] buffer = new byte[512];
            int bytesRead = 0;
            while ((bytesRead = input.read(buffer, 0, buffer.length)) >= 0) {
                output.write(buffer, 0, bytesRead);
            }
            output.close();
            input.close();
        }catch (IOException e){
            Log.e(TAG, "Cannot download");
            e.printStackTrace();
        }
    }

    @Override
    public void onComplete(@NonNull Task task) {
        if (task.isSuccessful()) {
            DocumentSnapshot document = (DocumentSnapshot) task.getResult();
            if (document.getData() == null) {
                Map<String, String> userfield = new HashMap<>();
                userfield.put("init", "start");
                db.collection("exclude").document(username).set(userfield);
            } else {
                for (String key : document.getData().keySet()) {
                    excludelist.put(key, key);
                }
            }
        } else {
            Log.d(TAG, "get failed with ", task.getException());
        }
        File listfile = new File(getFilesDir(), "l"+language+"a"+age+"c"+category+".json");
        if (listfile.exists()) {
            Log.i(TAG, "Found json file on disk cache!");
            StringBuilder raw_data = new StringBuilder();
            try {
                BufferedReader br = new BufferedReader(new FileReader(listfile));
                String line;
                while ((line = br.readLine()) != null) {
                    raw_data.append(line);
                }
                br.close();
                loaded = true;
                stop_loading(raw_data.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        new get_content().execute();
    }

    class dataAdapter extends RecyclerView.Adapter<dataAdapter.dataHolder> {

        private ArrayList<JSONObject> my_data = new ArrayList<>();
        private Context context;

        dataAdapter(ArrayList<JSONObject> data, Context c){
            context = c;
            my_data = data;
        }

        @NonNull
        @Override
        public dataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
            dataHolder holder = new dataHolder(v);
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull dataHolder holder, int position) {
            try {
                holder.itemView.setTag(Integer.toString(my_data.get(position).getInt("id")));
                holder.txtName.setText(my_data.get(position).getString("name"));
                holder.txtDesc.setText(my_data.get(position).getString("des"));
                File cur_img = new File(getFilesDir(), my_data.get(position).getString("img") + ".png");
                if (cur_img.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(cur_img.getAbsolutePath());
                    holder.imgCover.setImageBitmap(bitmap);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return my_data.size();
        }

        class dataHolder extends RecyclerView.ViewHolder  implements View.OnLongClickListener {
            ImageView imgCover;
            TextView txtName, txtDesc;

            dataHolder(View itemView) {
                super(itemView);
                imgCover = itemView.findViewById(R.id.imgCover);
                txtName = itemView.findViewById(R.id.txtName);
                txtDesc = itemView.findViewById(R.id.txtDesc);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final LocalDateTime now = LocalDateTime.now();
                        final String vtag = (String)view.getTag();
                        db.collection("play").document(username).set(new HashMap<String, String>(){{
                            put(now.toString(), vtag);
                        }}, SetOptions.merge());
                        Toast.makeText(view.getContext(), vtag, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        String uri = "it.tinytap://album/" + vtag;
                        Log.i(TAG, "Openning: " + uri);
                        intent.setData(Uri.parse(uri));
                        startActivity(intent);
                    }
                });
                itemView.setOnLongClickListener(this);
            }

            @Override
            public boolean onLongClick(View v) {
                excludelist.put((String) v.getTag(), (String) v.getTag());
                db.collection("exclude").document(username).set(excludelist);
                for (int i=0 ; i<my_data.size(); i++) {
                    try {
                        if (Integer.toString(my_data.get(i).getInt("id")).equals(v.getTag())) {
                            my_data.remove(i);
                            adapter.notifyItemRemoved(i);
                            adapter.notifyDataSetChanged();
                            break;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }
        }
    }

    class get_content extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            File listfile = new File(getFilesDir(), "l"+language+"a"+age+"c"+category+".json");
            try {
                if (!listfile.createNewFile()) Log.e(TAG, "Cannot Create file");
                FileOutputStream fOut = new FileOutputStream(listfile);
                OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                myOutWriter.append(s);
                myOutWriter.close();
                fOut.flush();
                fOut.close();
                Log.i(TAG, "Written new json file to disk!");
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!loaded) stop_loading(s);
        }

        @Override
        protected String doInBackground(Void... voids) {
            StringBuilder ret_val = new StringBuilder();
            Map<String, String> params = new LinkedHashMap<>();
            try {
                URL url = new URL("https://local-pointer-786.appspot.com/get_content");
                HttpURLConnection client = (HttpURLConnection) url.openConnection();
                client.setRequestMethod("POST");
                client.setDoOutput(true);
                client.setDoInput(true);
                client.connect();
                OutputStream os = client.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write("cat=" + category + "&age=" + age + "&lan=" + language);
                writer.flush();
                writer.close();
                os.close();

                int response = client.getResponseCode();
                Log.i(TAG, "Status:" + Integer.toString(response));
                if (response >= 200 && response <= 399) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String i;
                    while ((i = r.readLine()) != null) {
                        ret_val.append(i);
                    }
                } else {
                    Log.e(TAG, "Got Error response: " + Integer.toString(response));
                }
            } catch (IOException e) {
                Log.e(TAG, "Error in POST");
                e.printStackTrace();
            }
            return ret_val.toString();
        }
    }

    class get_imgs extends AsyncTask<Void, Integer, Void> {
        TextView progress;

        get_imgs(TextView txt) {
            progress = txt;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            //progress.setText("Downloading " + values[0] + "/" + need_to_download.size());
            //Log.i(TAG, "Downloaded " + values[0] + "/" + need_to_download.size());
        }

        @Override
        protected Void doInBackground(Void... voids) {
            for (int i=0 ; i<need_to_download.size(); i++) {
                download_img(need_to_download.get(i));
                publishProgress(i);
            }
            return null;
        }
    }
}
