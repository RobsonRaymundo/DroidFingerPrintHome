package ray.droid.com.fingerprinthome;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class DroidMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        Intent startIntent = new Intent(DroidMainActivity.this, DroidService.class);
        startService(startIntent);
        finish();
    }

}
