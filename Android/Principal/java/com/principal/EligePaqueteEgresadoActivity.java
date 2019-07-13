package com.principal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.HashMap;
import java.util.Map;

import clases.PaqueteEnBuzon;
import clases.PaqueteEsperado;

public class EligePaqueteEgresadoActivity extends AppCompatActivity {
    Button escan;
    String codigo_paquete = "";
    private static final String TAG = "EligePaqueteEgresadoActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elige_paquete_egresado);
        escan = (Button) findViewById(R.id.button);
    }

    public void ir_main_activity(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    //metodo asociado al boton scan, que hace un intent para scanear codigo
    public void escanear_codigo(View view) {
        new IntentIntegrator(EligePaqueteEgresadoActivity.this).initiateScan();
    }

    //una vez que se scaneo el codigo con la camara devuelve el resultado a esta activity por result.getcontent()
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null)
            if (result.getContents() != null) {
                codigo_paquete = result.getContents().trim();
                deletePaqueteEnBuzon(codigo_paquete);
            } else {
                Toast.makeText(EligePaqueteEgresadoActivity.this, "Error al escanear el código de barras", Toast.LENGTH_SHORT).show();
            }
    }

    //Cada vez que escaneamos un paquete egresado, se almacena el dato del peso y se elimina de la tabla de paquetes en buzon
    private void deletePaqueteEnBuzon(String codigo_paquete) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("PaqueteEnBuzon").document(codigo_paquete)
                .delete().addOnCompleteListener(new OnCompleteListener<Void>() {

            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(EligePaqueteEgresadoActivity.this, "PAQUETE BORRADO", Toast.LENGTH_LONG).show();
                    actualizarBuzon();
                } else {
                    Toast.makeText(EligePaqueteEgresadoActivity.this, "NO SE BORRÓ EL PAQUETE ESPERADO", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // Actualiza la cantidad de paquetes cuando Egresa un paqute
    private void actualizarBuzon() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference noteRf = db.document("Buzon/1");
        noteRf.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            String pesotot = documentSnapshot.getString("peso_total");
                            String humedad = documentSnapshot.getString("humedad");
                            String temperatura = documentSnapshot.getString("temperatura");
                            String cantpq = documentSnapshot.getString("cant_paquetes");
                            String codigo = documentSnapshot.getString("codigo");

                            Integer aux = Integer.parseInt(cantpq);
                            if(aux > 0)
                                aux--;

                            Map<String, Object> Buzon = new HashMap<>();
                            Buzon.put("peso_total", pesotot);
                            Buzon.put("temperatura", temperatura);
                            Buzon.put("humedad", humedad);
                            Buzon.put("cant_paquetes", aux.toString());
                            Buzon.put("codigo", codigo);
                            noteRf.set(Buzon);
                        } else {
                            Toast.makeText(EligePaqueteEgresadoActivity.this, "Document does not exist", Toast.LENGTH_LONG).show();
                        }

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(EligePaqueteEgresadoActivity.this, "ERROR!", Toast.LENGTH_LONG).show();


                    }
                });

    }


}
