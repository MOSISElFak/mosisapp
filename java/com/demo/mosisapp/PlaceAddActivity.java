package com.demo.mosisapp;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.lang.reflect.Field;
import java.util.Date;

public class PlaceAddActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, View.OnClickListener
{
    private final String TAG = "PlaceAddActivity";
    private Double lat;
    private Double lon;
    private boolean ftype,fattr,fdesc,frate; //flags
    // Layout items
    private Spinner spin_type;
    private Spinner spin_attr;
    private EditText descr;     //add_description_edit
    private RatingBar rate;     //add_layout_ratingBar
    private TextView label_key; //read_label_key
    private TextView label_author;      //read_label_author
    private TextView label_date;        //read_label_date
    private TextView label_time;        //read_label_time
    private TextView label_points;      //read_label_points
    private Button btn_ok;      //add_layout_button_cancel
    private Button btn_cancel;  //add_layout_button_ok

    private FirebaseDatabase database;
    private DatabaseReference refPlaces;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_add);

        Bundle extra = getIntent().getExtras();
        if (extra == null || !extra.containsKey("place_lon") || !extra.containsKey("place_lat"))
            finish();
        lat = extra.getDouble("place_lat");
        lon = extra.getDouble("place_lon");

        // Connect to layout
        spin_type = (Spinner) findViewById(R.id.add_type_spinner);
        ArrayAdapter<CharSequence> spin_adapter_type = ArrayAdapter.createFromResource(this, R.array.add_type_spinner_array, android.R.layout.simple_spinner_item);
        spin_adapter_type.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin_type.setAdapter(spin_adapter_type);
        spin_type.setOnItemSelectedListener(this); //onItemSelected, onNothingSelected

        spin_attr = (Spinner) findViewById(R.id.add_attribute_spinner);
        spin_attr.setOnItemSelectedListener(this); //onItemSelected, onNothingSelected

        descr = (EditText) findViewById(R.id.add_description_edit);
        rate = (RatingBar) findViewById(R.id.add_layout_ratingBar);
        label_key = (TextView) findViewById(R.id.read_label_key);
        label_author = (TextView) findViewById(R.id.read_label_author);
        label_date = (TextView) findViewById(R.id.read_label_date);
        label_time = (TextView) findViewById(R.id.read_label_time);
        label_points = (TextView) findViewById(R.id.read_label_points);
        btn_ok = (Button) findViewById(R.id.add_layout_button_ok);
        btn_cancel = (Button) findViewById(R.id.add_layout_button_cancel);

        btn_ok.setOnClickListener(this);
        btn_cancel.setOnClickListener(this);

        String message = lat.toString().concat("..").concat(lon.toString());
        label_key.setText(message);

        LayerDrawable stars = (LayerDrawable) rate.getProgressDrawable();
        stars.getDrawable(2).setColorFilter(ResourcesCompat.getColor(getResources(), R.color.Green_A700, null), PorterDuff.Mode.SRC_ATOP); // stackoverflow.com/a/21872331

        // Connect to database
        database = FirebaseDatabase.getInstance();
        refPlaces = database.getReference().child(Constants.ADDPLACE);
    }

    // This will dynamically change list of items on attribute spinner depending on type spinner
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(position);
        if (parent.getId() == R.id.add_type_spinner) {
            try {
                String source = "add_attr_".concat(Integer.toString(position+1));
                Class res = R.array.class;
                Field field = res.getField(source);
                int zeId = field.getInt(null);
                ArrayAdapter<CharSequence> spin_adapter_attr = ArrayAdapter.createFromResource(this, zeId, android.R.layout.simple_spinner_item);
                spin_adapter_attr.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spin_attr.setAdapter(spin_adapter_attr);
            }
            catch (Exception e) {
                Log.e("Spinner selection", "Failed to get spinner ID.", e);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
        Log.d(TAG, "onNothingSelected");
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case (R.id.add_layout_button_cancel):
                finish();
                break;
            case (R.id.add_layout_button_ok):
                createPlace();
                finish();
                break;
        }
    }

    private void createPlace() {
        String type = spin_type.getSelectedItem().toString();
        String attr = spin_attr.getSelectedItem().toString();

        PlaceBean thisOne = new PlaceBean(lat,lon,type,attr);

        refPlaces.push().setValue(thisOne).addOnCompleteListener(new OnCompleteListener<Void>()
        {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful())
                    Toast.makeText(getApplicationContext(), "Place added!", Toast.LENGTH_SHORT).show();
                else {
                    Toast.makeText(getApplicationContext(), "Adding place failed", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, task.getException().getMessage());
                }
            }
        });
    }
}