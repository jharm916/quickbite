package com.cmsc436.quickbite.slidingtab.ListElements;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.cmsc436.quickbite.Bite;
import com.cmsc436.quickbite.R;
import com.cmsc436.quickbite.TimerActivity;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.yelp.clientlib.connection.YelpAPI;
import com.yelp.clientlib.connection.YelpAPIFactory;
import com.yelp.clientlib.entities.Business;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import retrofit.Call;
import retrofit.Response;

public class RestaurantProfile extends AppCompatActivity {

    String consumerKey = "0YxBV-Axpu7Z0XD2pp91jg";
    String consumerSecret = "6GRuyklo0qJcODC9vuUjYo2uvVg";
    String token = "QLWLjZ0_7ltM0TuumUk8U2m6rcsNYXQy";
    String tokenSecret = "gE6sgS-8x5C3K1ttW8LsQTvbdQ0";
    YelpAPIFactory apiFactory = new YelpAPIFactory(consumerKey, consumerSecret, token, tokenSecret);
    YelpAPI yelpAPI = apiFactory.createAPI();
    Response<Business> response;
    private String restaurantID;
    private String restaurantName;
    ArrayList<Bite> biteData;
    int serviceRating = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_profile);

        Bundle bundle = getIntent().getExtras();
        restaurantID = bundle.getString(LocationList.restaurantIDKey);
        restaurantName = bundle.getString(LocationList.restaurantNameKey);

        getSupportActionBar().setTitle(restaurantName);
        TextView name = (TextView)findViewById(R.id.name);
        name.setText(restaurantName);


        /*Get wait time from intent here. Hardcoding for now.*/
        //given as seconds. divide by 60.
        int waitTime = 30;
        TextView txt = (TextView)findViewById(R.id.waitTime);
        if(waitTime == -1){
            txt.setText(getString(R.string.noTime));
        }else if(waitTime <= 10){
            txt.setText(getString(R.string.s));
        }else if(waitTime <= 30){
            txt.setText(getString(R.string.ok));
        }else if(waitTime > 30){
            txt.setText(getString(R.string.l));
        }

        RecyclerView rv = (RecyclerView)findViewById(R.id.rv);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rv.setLayoutManager(layoutManager);


        biteData = new ArrayList<Bite>();

        // Get a database reference to our posts
        Firebase ref = new Firebase("https://quick-bite.firebaseio.com/").child(restaurantID).child("bites");
        //attach a listener
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                biteData.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Bite b = postSnapshot.getValue(Bite.class);
                    serviceRating += b.getRating();
                    biteData.add(0, b);
                }

                /*Initialize service rating*/
                TextView service = (TextView) findViewById(R.id.service);
                if(biteData.size() != 0) {

                    serviceRating = serviceRating / biteData.size();
                    if (serviceRating == 1) {
                        service.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.qb_gray_very_dissatisfied), null, null, null);
                        service.setText(getString(R.string.one));
                    } else if (serviceRating == 2) {
                        service.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.qb_gray_dissatisfied), null, null, null);
                        service.setText(getString(R.string.two));
                    } else if (serviceRating == 3) {
                        service.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.qb_gray_neutral), null, null, null);
                        service.setText(getString(R.string.three));
                    } else if (serviceRating == 4) {
                        service.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.qb_gray_satisfied), null, null, null);
                        service.setText(getString(R.string.four));
                    } else if (serviceRating == 5) {
                        service.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.qb_gray_satisfied), null, null, null);
                        service.setText(getString(R.string.five));
                    } else {
                        service.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.qb_gray_neutral), null, null, null);
                        service.setText(getString(R.string.other));
                    }
                }else{
                    service.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.qb_gray_neutral), null, null, null);
                    service.setText(getString(R.string.other));
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });


        RVAdapter adapter = new RVAdapter(biteData);
        rv.setAdapter(adapter);

        //needs to be in an asycn task to avoid NetworkOnMainThreadException
        new GetBusinessData().execute(restaurantID);

    }

    private class GetBusinessData extends AsyncTask<String, Void, Response<Business>> {

        @Override
        protected Response<Business> doInBackground(String... params) {
            Call<Business> call = yelpAPI.getBusiness(params[0]);
            try {
                response = call.execute();
                return response;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
        protected void onPostExecute(Response<Business> result) {
            TextView address = (TextView)findViewById(R.id.address);
            List a = result.body().location().displayAddress();
            address.setText((String)a.get(0));
            //address.append((String) a.get(1));
            TextView phone = (TextView)findViewById(R.id.phone);
            phone.setText(result.body().displayPhone());
            String imageURL = result.body().imageUrl();
            //changing image from "ms.jpg" to "l.jpg" so that we get a higher resolution image & dont have to crop/zoom in as much.
            String largeimageURL = imageURL.substring(0, imageURL.lastIndexOf("ms.jpg")) + "l.jpg";
            new getImage().execute(largeimageURL);
        }
    }


    private class getImage extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... urls) {
            Bitmap map = null;
            for (String url : urls) {
                map = downloadImage(url);
            }
            return map;
        }

        // Sets the Bitmap returned by doInBackground
        @Override
        protected void onPostExecute(Bitmap result) {
            ImageView imageView = (ImageView)findViewById(R.id.image);
            imageView.setImageBitmap(result);
        }

        // Creates Bitmap from InputStream and returns it
        private Bitmap downloadImage(String url) {
            Bitmap bitmap = null;
            InputStream stream = null;
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inSampleSize = 1;

            try {
                stream = getHttpConnection(url);
                bitmap = BitmapFactory.
                        decodeStream(stream, null, bmOptions);
                stream.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return bitmap;
        }

        // Makes HttpURLConnection and returns InputStream
        private InputStream getHttpConnection(String urlString)
                throws IOException {
            InputStream stream = null;
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();

            try {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;
                httpConnection.setRequestMethod("GET");
                httpConnection.connect();

                if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    stream = httpConnection.getInputStream();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return stream;
        }
    }

    public void checkIn(View view) {
        Intent checkInIntent = new Intent(this, TimerActivity.class);
        checkInIntent.putExtra(LocationList.restaurantIDKey, restaurantID);
        checkInIntent.putExtra(LocationList.restaurantNameKey, restaurantName);
        startActivity(checkInIntent);
    }
}
