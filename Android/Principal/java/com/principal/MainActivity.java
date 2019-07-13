package com.principal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import clases.Buzon;
import clases.PaqueteEnBuzon;
import clases.PaqueteEsperado;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, SensorEventListener, View.OnClickListener {
    TextView textView;
    Sensor sensor;
    SensorManager sensorManager;
    SensorEventListener sensorEventListener;
    int sonido = 0;
    String tipoTemperatura = "";
    String temperatura = "";
    String codigoPaquete = "";
    String mensaje = "";

    private TextView textViewData;
    private static final String TAG = "MainActivity";
    private static final String ESTADOPUERTA = "estado_puerta";
    private static final String CODIGO = "codigo";
    private static final String NOTIFICACION = "notificacion";
    private static final String TEMPERATURA = "20";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    ArrayList<String> stringArrayList = new ArrayList<String>();
    private String coleccion = "PaqueteEsperado";
    private static final int REQUEST_CODE = 1;
    private DocumentReference notificacion = db.document("Notificacion/1");
    private PendingIntent pendingIntent;
    private final static String CHANNEL_ID = "NOTIFICACION";
    private final static int NOTIFICACION_ID = 0;
    private final static String TEMP_IDEAL = "24";
    private final static  int ACELEROMETROMAX = 7;
    private final static  int ACELEROMETROMIN = -7;
    private final static  int SONIDO_ON = 1;
    private final static  int SONIDO_OFF = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FloatingActionButton fab = findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(this);
        textView = (TextView) findViewById(R.id.textView);
        textViewData = (TextView) findViewById(R.id.text_view_data);
        Spinner spinner = findViewById(R.id.spinner1);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.colecciones, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        start();
        verificarPermisos();
    }

    @Override
    protected void onStart() {
        super.onStart();
        notificacion.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Toast.makeText(MainActivity.this, "Error creando el listener", Toast.LENGTH_LONG).show();
                    Log.d(TAG, e.toString());
                    return;
                }

                if (documentSnapshot.exists()) {
                    String _notificacion = documentSnapshot.getString(NOTIFICACION);
                    // Notificaciòn si se Ingresa un paquete
                    if (_notificacion.equals("1")) {
                        crearMsj();
                        createNotificationChannel();
                        createNotification(mensaje);

                    } else {
                        // Notificaciòn si se Egresa un paquete
                        if (_notificacion.equals("2")) {
                            createNotificationChannel();
                            createNotification2();
                        }
                    }
                    actualizaCampoNotificacion();
                }

            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        parar_sensores();
        notificacion.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Toast.makeText(MainActivity.this, "Error creando el listener", Toast.LENGTH_LONG).show();
                    Log.d(TAG, e.toString());
                    return;
                }

                if (documentSnapshot.exists()) {
                    String _notificacion = documentSnapshot.getString(NOTIFICACION);
                    // Notificaciòn cuando se Ingresa un Paquete|
                    if (_notificacion.equals("1")) {
                        crearMsj();
                        createNotificationChannel();
                        createNotification(mensaje);

                    } else {
                        // Notificaciòn cuando se Egresa un Paquete
                        if (_notificacion.equals("2")) {
                            createNotificationChannel();
                            createNotification2();
                        }
                    }
                    actualizaCampoNotificacion();
                }

            }
        });
    }

    @Override
    protected void onResume() {
        start();
        super.onResume();
        inicia_sensores();
    }

    @Override
    protected void onPause() {
        stop();
        super.onPause();
        parar_sensores();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        inicia_sensores();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        parar_sensores();
    }


    // Crea el msj de davertencia cuando se ingresa un paquete.
    private String crearMsj() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference noteRf = db.document("Buzon/1");

        noteRf.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            String temperatura = documentSnapshot.getString("temperatura");
                            String codigo = documentSnapshot.getString("codigo");
                            String tipo_temp = buscarTipoTemp(codigo);
                            if ((temperatura.compareTo(TEMP_IDEAL) > 0 && tipo_temp.equals("B")) || (temperatura.compareTo(TEMP_IDEAL) < 0 && tipo_temp.equals("A")))
                                mensaje =  "ADVERTENCIA: Temperatura no ideal para el paquete esperado.";

                        } else {

                            Toast.makeText(MainActivity.this, "Document does not exist", Toast.LENGTH_LONG).show();
                        }

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@androidx.annotation.NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "ERROR!", Toast.LENGTH_LONG).show();


                    }
                });

        return mensaje;

    }

    // Busca el tipo de temperatura que tiene el paquete.
    private String buscarTipoTemp(final String codigo) {
        DocumentReference paquete_esperado = db.document("PaqueteEnBuzon/" + codigo);
        paquete_esperado.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            tipoTemperatura = documentSnapshot.getString("tipo_temperatura");
                        } else {
                            Toast.makeText(MainActivity.this, "ERROR : TIPO DE TEMPERATURA " + codigo, Toast.LENGTH_LONG).show();

                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "ERROR", Toast.LENGTH_LONG).show();
                        textViewData.setText(textViewData.getText() + "\n" + "ERROR: No se pudo consultar los paquetes en buzon");
                        Log.d(TAG, e.toString());
                    }
                });
        return tipoTemperatura;
    }

    //llama a la funcion verificarpermisos()
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        verificarPermisos();
    }

    //verifica los permisos de la camara para scannear el cod de paquete usado
    private void verificarPermisos() {
        Log.d(TAG, "verifyPermissions: asking user for permissions");
        String[] permissions = {Manifest.permission.CAMERA};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[0]) != PackageManager.PERMISSION_GRANTED

        ) {
            ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_CODE);
        }
    }

    //inicia los sensores de proximidad y acelerometro
    private void inicia_sensores() {
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL);
    }

    //metodos utilizados por los sensores
    private void start() {
        sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void stop() {
        sensorManager.unregisterListener(sensorEventListener);
    }

    private void parar_sensores() {
        sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY));

    }


    //metodos del spinner
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        String text = adapterView.getItemAtPosition(i).toString();
        CollectionReference notebookRef = db.collection(text);
        if (text.equals("PaqueteEsperado")) {
            notebookRef.addSnapshotListener(this, new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        return;
                    }
                    String data = "";

                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        PaqueteEsperado note = documentSnapshot.toObject(PaqueteEsperado.class);

                        String descripcion = note.getDescripcion();
                        String tipotemp = note.getTipo_temperatura();
                        stringArrayList.add(documentSnapshot.getId());

                        data += "Código: " + documentSnapshot.getId() + "\nDescripción: " + descripcion + "\nTipo temperatura: " + tipotemp + "\n\n";
                    }

                    textViewData.setText(data);
                }
            });
        }
        if (text.equals("Buzon")) {
            notebookRef.addSnapshotListener(this, new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        return;
                    }
                    String data = "";

                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Buzon note = documentSnapshot.toObject(Buzon.class);
                        String cant_paquetes = note.getCant_Paquetes();
                        String humedad = note.getHumedad();
                        String peso_total = note.getPeso_total();
                        String temperatura = note.getTemperatura();
                        String codigo = note.getgCodigo();

                        stringArrayList.add(documentSnapshot.getId());

                        data += "Cant. Paquetes: " + cant_paquetes + "\nHumedad: "
                                + humedad + "\nPeso total: " + peso_total + "\nTemperatura: " + temperatura + "\n\n";
                    }

                    textViewData.setText(data);
                }
            });
        }
        if (text.equals("PaqueteEnBuzon")) {
            notebookRef.addSnapshotListener(this, new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        return;
                    }
                    String data = "";

                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        PaqueteEnBuzon note = documentSnapshot.toObject(PaqueteEnBuzon.class);
                        String descripcion = note.getDescripcion();
                        String peso = note.getPeso();
                        stringArrayList.add(documentSnapshot.getId());
                        data += "Descripcion: " + descripcion + "\nPeso: " + peso + "\n\n";
                    }

                    textViewData.setText(data);
                }
            });
        }
        coleccion = text;
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    //switch para detectar el cambio en los sensores y actualizar la tabla de sensores
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        synchronized (this) {

            switch (sensorEvent.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    float x = sensorEvent.values[0];
                    if (x < ACELEROMETROMIN && sonido == 0) {
                        System.out.println("Valor giro " + x);
                        sonido++;
                    } else if (x > ACELEROMETROMAX && sonido == SONIDO_ON) {
                        sonido++;
                    }
                    if (sonido == SONIDO_OFF ) {
                        sonido = 0;
                        sound();
                        updateSensor_acelerometro();
                    }
                    break;
                case Sensor.TYPE_PROXIMITY:
                    if (sensorEvent.values[0] <= 1) {
                        updateSensor_proximidad();
                    }
                    break;
            }//switch
        }//synchronize
    }//onSensorChanged

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    //modifica en firebase el valor de la tabla para que la secundaria prenda el led y actualize la temperatura del Buzon
    private void updateSensor_proximidad() {
        DocumentReference bd_ = db.document("Sensores/1");

        Map<String, Object> sensores = new HashMap<>();
        sensores.put(ESTADOPUERTA, "3");


        bd_.set(sensores)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, e.toString());
                    }
                });
    }

    //modifica en firebase el valor de la tabla para que la secundaria abra la puerta
    private void updateSensor_acelerometro() {
        DocumentReference bd_ = db.document("Sensores/1");

        Map<String, Object> sensores = new HashMap<>();
        sensores.put(ESTADOPUERTA, "2");

        bd_.set(sensores);
    }

    //vuelve a poner en 0 el campo de notificaciones luego de recibir una
    private void actualizaCampoNotificacion() {
        DocumentReference bd_ = db.document("Notificacion/1");
        Map<String, Object> notificacion = new HashMap<>();
        notificacion.put(NOTIFICACION, "0");


        bd_.set(notificacion);
    }


    //metodo asociado al boton flotante mas para cargar codigo, el cual abre una activity nueva por intent
    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, CargaCodigoManualActivity.class);
        startActivity(intent);
    }

    //se crea una instancia de notificacion
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Noticacion";
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    //crea visualmente la notificacion de cuando llega un paquete nuevo al buzon
    private void createNotification(String msj) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
        builder.setSmallIcon(R.drawable.ic_sms_black_24dp);
        builder.setContentTitle("Ingresó un paquete");
        builder.setContentText(msj);
        builder.setColor(Color.BLUE);
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        builder.setDefaults(Notification.DEFAULT_SOUND);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
        notificationManagerCompat.notify(NOTIFICACION_ID, builder.build());
        Intent resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);
        notificationManagerCompat.notify(0, builder.build());

    }

    //crea visualmente la notificacion de cuando se sacan paquetes del buzom
    private void createNotification2() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
        builder.setSmallIcon(R.drawable.ic_sms_black_24dp);
        builder.setContentTitle("Sacaste un paquete");
        builder.setContentText("Escanea el Codigo");
        builder.setColor(Color.BLUE);
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        builder.setDefaults(Notification.DEFAULT_SOUND);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
        notificationManagerCompat.notify(NOTIFICACION_ID, builder.build());
        Intent resultIntent = new Intent(this, EligePaqueteEgresadoActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManagerCompat.notify(0, builder.build());

    }

    //hace sonar el audio cuando detecta el shake
    private void sound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.cierre_1);
        mediaPlayer.start();
    }
}

