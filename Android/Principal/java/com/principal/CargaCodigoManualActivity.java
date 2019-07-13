package com.principal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CargaCodigoManualActivity extends AppCompatActivity {
    String strTipoTemp = "N";
    EditText campoCodPaquete;
    EditText campoDescripcionPaquete;
    Button button7;
    ProgressDialog progreso;
    private static final String TAG = "CargaCodigoManualActivity";
    private static final String DESCRIPCION = "descripcion";
    private static final String TIPOTEMPERATURA = "tipo_temperatura";

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carga_codigo_manual);
        RadioButton rbNormal = (RadioButton)findViewById(R.id.rbTempNormal);
        rbNormal.setChecked(true);
        campoDescripcionPaquete=(EditText) findViewById(R.id.campoDescripcionPaquete);
        campoCodPaquete=(EditText) findViewById(R.id.campoCodPaquete);
        button7=(Button)findViewById(R.id.button7);
        button7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inserta_paquete(campoCodPaquete.getText().toString().trim(),campoDescripcionPaquete.getText().toString().trim(),strTipoTemp.trim());
                campoCodPaquete.setText("");
                campoDescripcionPaquete.setText("");
            }
        });
        campoCodPaquete.addTextChangedListener(cargaTextWatcher);
        campoDescripcionPaquete.addTextChangedListener(cargaTextWatcher);
    }
    //no te habilita el boton de cargar paquete hasta que no hayas completado los campos
    private TextWatcher cargaTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String cod_paquete_esperado_input=campoCodPaquete.getText().toString().trim();
            String descripcion_paquete_esperado_input=campoDescripcionPaquete.getText().toString().trim();

            button7.setEnabled(!cod_paquete_esperado_input.isEmpty()&&!descripcion_paquete_esperado_input.isEmpty());
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    //asigna la letra (N, A o B) dependiendo de si el paquete es sensible a la temperatura o no
    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        switch(view.getId()) {
            case R.id.rbTempNormal:
                if(checked)
                    strTipoTemp = "N";
                break;
            case R.id.rbTempAlta:
                if(checked)
                    strTipoTemp = "A";
                break;
            case R.id.rbTempBaja:
                if(checked)
                    strTipoTemp = "B";
                break;
        }

    }

    //metodo asociado al boton de ingresar paquete cuando se pusieron los datos
    private void inserta_paquete(String codigo_paquete, String descripcion, String tipo_temperatura) {
        progreso = new ProgressDialog(this);
        progreso.setMessage("Insertando...");
        progreso.show();
        DocumentReference bd_ = db.document("PaqueteEsperado/"+codigo_paquete);

        Map<String, Object> paquete_esperado = new HashMap<>();
        paquete_esperado.put(DESCRIPCION, descripcion);
        paquete_esperado.put(TIPOTEMPERATURA,tipo_temperatura);

        bd_.set(paquete_esperado)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progreso.hide();

                        Toast.makeText(CargaCodigoManualActivity.this, "Paquete esperado ingresado", Toast.LENGTH_LONG).show();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @SuppressLint("LongLogTag")
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progreso.hide();

                        Toast.makeText(CargaCodigoManualActivity.this, "ERROR! No se insertó", Toast.LENGTH_LONG).show();
                        Log.d(TAG, e.toString());

                    }
                });
    }
}
