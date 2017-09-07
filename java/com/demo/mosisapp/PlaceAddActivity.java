package com.demo.mosisapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

public class PlaceAddActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener
{
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
    private Button btn_ok;
    private Button btn_cancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_add);

        Bundle extra = getIntent().getExtras();
        if (extra == null || !extra.containsKey("place_lon") || !extra.containsKey("place_lat")) finish();
        lat = extra.getDouble("place_lat");
        lon = extra.getDouble("place_lon");

        spin_type = (Spinner)findViewById(R.id.add_type_spinner);
        ArrayAdapter<CharSequence> spin_adapter_type = ArrayAdapter.createFromResource(this, R.array.add_type_spinner_array, android.R.layout.simple_spinner_item);
        spin_adapter_type.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin_type.setAdapter(spin_adapter_type);
        spin_type.setOnItemSelectedListener(this); //onItemSelected, onNothingSelected

        spin_attr = (Spinner)findViewById(R.id.add_attribute_spinner);
        ArrayAdapter<CharSequence> spin_adapter_attr = ArrayAdapter.createFromResource(this,R.array.add_attr_spinner_array, android.R.layout.simple_spinner_item);
        spin_adapter_attr.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin_attr.setAdapter(spin_adapter_attr);
        spin_attr.setOnItemSelectedListener(this); //onItemSelected, onNothingSelected

        descr = (EditText)findViewById(R.id.add_description_edit);
        rate = (RatingBar) findViewById(R.id.add_layout_ratingBar);
        label_key = (TextView)findViewById(R.id.read_label_key);
        label_author = (TextView)findViewById(R.id.read_label_author);
        label_date = (TextView)findViewById(R.id.read_label_date);
        label_time = (TextView)findViewById(R.id.read_label_time);
        label_points = (TextView)findViewById(R.id.read_label_points);
        btn_ok = (Button)findViewById(R.id.add_layout_button_ok);
        btn_cancel = (Button)findViewById(R.id.add_layout_button_cancel);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback

    }
}
