package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Time;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

public class MainActivity extends AppCompatActivity {

    private static final String MACDISPOSITIVO = "00:18:E4:34:F5:53"; //MAC del celular a conectar

    private static final String TAG = "MainActivity";
    private static final String CODIGO = "codigo";
    private static final String PESO = "peso";
    private static final String TIPOTEMPERATURA = "tipo_temperatura";
    private static final String DESCRIPCION = "descripcion";
    private static final String PESOTOTAL = "peso_total";
    private static final String CANTPAQUETES = "cant_paquetes";
    private static final String TEMPERATURA = "temperatura";
    private static final String HUMEDAD = "humedad";
    private static final String ESTADOPUERTA = "estado_puerta";
    private static final String CODIGO_INGRESADO = "C";
    private static final String INGRESO_PAQUETE = "I";
    private static final String SALIDA_PAQUETE = "O";
    private static final String ACTUALIZA_TEMP_Y_HUM = "H";



    private TextView textViewData;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentReference sensores_acc = db.document("Sensores/1");

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //VARIABLES GLOBALES DE PAQUETES ESPERADOS
    String descripcion      = "";
    String tipo_temperatura = "";
    //VARIABLES GLOBALES DE PAQUETES EN BUZON
    String codigo_paquete   = "";
    String peso_paquete     = "";

    //VARIABLES GLOBALES DE BUZON
    String temperatura;
    String humedad;
    Integer cantidad_paquetes = 0;
    String peso_total;

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    Handler bluetoothIn;
    private ConnectedThread MyConexionBT;
    final int handlerState = 0;
    private StringBuilder DataStringIN = new StringBuilder();


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            btSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint({"InvalidWakeLockTag", "HandlerLeak"})

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //Esto es para que la pantalla quede encendida

        setContentView(R.layout.activity_main);

        textViewData = findViewById(R.id.text_view_data);

        btAdapter = BluetoothAdapter.getDefaultAdapter(); // get Bluetooth adapter
        BluetoothDevice device = btAdapter.getRemoteDevice(MACDISPOSITIVO);

        try {
            btSocket = createBluetoothSocket(device);//Crea el socket bluetooth

        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "ERROR", Toast.LENGTH_LONG).show();
            textViewData.setText("ERROR: La creación del Socket falló");
        }

        // Establece la conexión con el socket Bluetooth.
        try {
            btSocket.connect();
            Toast.makeText(getBaseContext(), "Dispositivo conectado", Toast.LENGTH_LONG).show();
            textViewData.setText("Dispositivo conectado");
        } catch (IOException e) {
            try {
                Toast.makeText(getBaseContext(), "ERROR", Toast.LENGTH_LONG).show();
                textViewData.setText("ERROR: No se pudo conectar el dispositivo");
                btSocket.close();
            } catch (IOException e2) {
            }
        }

        MyConexionBT = new ConnectedThread(btSocket);
        MyConexionBT.start();
        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    DataStringIN.append(readMessage);
                    int endOfLineIndex = DataStringIN.indexOf("\r");

                    if (endOfLineIndex > 0) {
                        String dataInPrint = DataStringIN.substring(0, endOfLineIndex);
                        dataInPrint.trim();
                        dataInPrint = dataInPrint.replace("\n", "");
                        textViewData.setText(textViewData.getText() + "\n" + "BLUETOOTH TIENE: " + dataInPrint);
                        String codigo = dataInPrint.substring(0, 1);
                        if (codigo.equals(CODIGO_INGRESADO)) {
                            codigo_paquete = dataInPrint.substring(2);
                            consultaCodigoPaqueteLeido();
                        } else if (codigo.equals(INGRESO_PAQUETE)) {
                            textViewData.setText(textViewData.getText() + "\n" + "LLEGO AL IF");
                            desencadenar(dataInPrint);
                            insertPaqueteEnBuzon();
                            deletePaqueteEsperado();
                            UpdateBuzon(INGRESO_PAQUETE);
                            Notificar("1");
                            textViewData.setText(textViewData.getText() + "\n" + "Humedad: " + humedad + " Temperatura: " + temperatura + " PesoPaq: " + peso_paquete + " PesoTotal: " + peso_total);
                        } else if (codigo.equals(SALIDA_PAQUETE)) {
                            desencadenar(dataInPrint);
                            UpdateBuzon(SALIDA_PAQUETE);
                            Notificar("2");
                            textViewData.setText(textViewData.getText() + "\n" + "Humedad: " + humedad + " Temperatura: " + temperatura + " PesoPaq: " + peso_paquete + " PesoTotal: " + peso_total);
                        } else if (codigo.equals(ACTUALIZA_TEMP_Y_HUM)) {
                            desencadenar(dataInPrint);
                            UpdateBuzon(ACTUALIZA_TEMP_Y_HUM);
                            textViewData.setText(textViewData.getText() + "\n" + "Humedad: " + humedad + " Temperatura: " + temperatura + " PesoPaq: " + peso_paquete + " PesoTotal: " + peso_total);
                        }
                        DataStringIN.delete(0, DataStringIN.length());
                    }
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        sensores_acc.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Toast.makeText(MainActivity.this, "ERROR AL CARGAR", Toast.LENGTH_LONG).show();
                    Log.d(TAG, e.toString());
                    return;
                }

                if (documentSnapshot.exists()) {
                    String _estado_puerta = documentSnapshot.getString(ESTADOPUERTA);
                    if (_estado_puerta.equals("2")) {
                        MyConexionBT.write("2");
                        textViewData.setText(textViewData.getText() + "\n" + "Se abrió la puerta manualmente");
                        update_estado_puerta_Sensor_acelerometro();
                    } else if (_estado_puerta.equals("3")) {
                        MyConexionBT.write("3");
                        textViewData.setText(textViewData.getText() + "\n" + "Cambio de color de LED para detectar buzón");
                        update_estado_puerta_Sensor_acelerometro();
                    }
                }
            }
        });
    }

    // Se separa la cadena recibida por bluetooh
    private void desencadenar(String dataInPrint ){
        int indice_2do_pipe;
        int indice_3er_pipe;
        int indice_4er_pipe;
        indice_2do_pipe = dataInPrint.indexOf('|', 2);//indice del 2do pipe
        humedad = dataInPrint.substring(2, indice_2do_pipe);
        indice_3er_pipe = dataInPrint.indexOf('|', indice_2do_pipe + 1);//indice del 3er pipe
        temperatura = dataInPrint.substring(indice_2do_pipe + 1, indice_3er_pipe);
        indice_4er_pipe = dataInPrint.indexOf('|', indice_3er_pipe + 1);//indice del 4er pipe
        peso_paquete = dataInPrint.substring(indice_3er_pipe + 1, indice_4er_pipe);
        peso_total = dataInPrint.substring(indice_4er_pipe + 1);//Peso total, no neceisto buscar un 4to PIPE
    }

    // Se busca el codigo leido por la lectora de codigo de barras en la base de datos.
    private void consultaCodigoPaqueteLeido() {
        DocumentReference paquete_esperado = db.document("PaqueteEsperado/" + codigo_paquete);

        paquete_esperado.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            descripcion = documentSnapshot.getString("descripcion");
                            tipo_temperatura = documentSnapshot.getString("tipo_temperatura");
                            textViewData.setText(textViewData.getText() + "\n" + "Paquete encontrado: " + descripcion);
                            Toast.makeText(MainActivity.this, "Paquete encontrado", Toast.LENGTH_LONG).show();
                            MyConexionBT.write("1");
                        } else {
                            Toast.makeText(MainActivity.this, "ERROR", Toast.LENGTH_LONG).show();
                            textViewData.setText(textViewData.getText() + "\n" + "ERROR: No se encontró el paquete");
                            MyConexionBT.write("0");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "ERROR", Toast.LENGTH_LONG).show();
                        textViewData.setText(textViewData.getText() + "\n" + "ERROR: No se pudo consultar los paquetes esperados");
                        Log.d(TAG, e.toString());
                    }
                });
    }

    // Se ingresa los datos del paquete leido en la base de datos
    private void insertPaqueteEnBuzon() {
        Time today=new Time(Time.getCurrentTimezone());
        today.setToNow();
        int dia=today.monthDay;
        int mes=today.month;
        int ano=today.year;
        mes=mes+1;

        String fecha = dia+"/"+mes+"/"+ano;
        String hora = new Date().toString();

        Toast.makeText(MainActivity.this, fecha + "" + hora, Toast.LENGTH_LONG).show();

        DocumentReference bd_ = db.document("PaqueteEnBuzon/" + codigo_paquete);
        Map<String, Object> paquete_en_buzon = new HashMap<>();
        paquete_en_buzon.put(CODIGO, codigo_paquete);
        paquete_en_buzon.put(PESO, peso_paquete);
        paquete_en_buzon.put(DESCRIPCION, descripcion);
        paquete_en_buzon.put(TIPOTEMPERATURA, tipo_temperatura);

        bd_.set(paquete_en_buzon)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MainActivity.this, "Paquete " + codigo_paquete + " ingresado en buzón", Toast.LENGTH_LONG).show();
                        textViewData.setText(textViewData.getText() + "\n" + "Paquete " + codigo_paquete + " ingresado en buzón");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "ERROR", Toast.LENGTH_LONG).show();
                        textViewData.setText(textViewData.getText() + "\n" + "ERROR: No se insertó el paquete " + codigo_paquete + " en buzón");
                        Log.d(TAG, e.toString());
                    }
                });
    }

    // Se elimina el paquete ingresado de la tabla de paquetes esperados
    private void deletePaqueteEsperado() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("PaqueteEsperado").document(codigo_paquete)
                .delete().addOnCompleteListener(new OnCompleteListener<Void>() {

            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Paquete esperado borrado " + codigo_paquete, Toast.LENGTH_LONG).show();
                    textViewData.setText(textViewData.getText() + "\n" + "Paquete esperado borrado " + codigo_paquete);
                } else {
                    Toast.makeText(MainActivity.this, "ERROR", Toast.LENGTH_LONG).show();
                    textViewData.setText(textViewData.getText() + "\n" + "ERROR: No se pudo borrar el paquete esperado " + codigo_paquete);
                }
            }
        });
    }

    // Se elimina el paquete egrasado de la tabla de paquetes en buzon
    private void deletePaqueteEnBuzon() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("PaqueteEnBuzon").document("1")
                .delete().addOnCompleteListener(new OnCompleteListener<Void>() {

            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Paquetes en buzón borrados", Toast.LENGTH_LONG).show();
                    textViewData.setText(textViewData.getText() + "\n" + "Paquetes en buzón borrados");
                } else {
                    Toast.makeText(MainActivity.this, "ERROR", Toast.LENGTH_LONG).show();
                    textViewData.setText(textViewData.getText() + "\n" + "ERROR: No se pudieron borrar los paquetes en buzón");
                }
            }
        });
    }

    // Se actualizan lops datos de la tabla Buzon
    private void UpdateBuzon(String valor) {
        final String var = valor;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference noteRf = db.document("Buzon/1");

        noteRf.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            String cantpq = documentSnapshot.getString("cant_paquetes");
                            Integer aux = Integer.parseInt(cantpq);
                            switch (var) {
                                case "I":
                                    aux++;
                                    break;
                                case "O":
                                    codigo_paquete = documentSnapshot.getString("codigo");
                                    break;
                                case "H":
                                    peso_total = documentSnapshot.getString("peso_total");
                                    break;
                            }
                            Map<String, Object> Buzon = new HashMap<>();
                            Buzon.put("peso_total", peso_total);
                            Buzon.put("temperatura", temperatura);
                            Buzon.put("humedad", humedad);
                            Buzon.put("cant_paquetes", aux.toString());
                            Buzon.put("codigo", codigo_paquete);
                            noteRf.set(Buzon);
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

    }

    // Ingrasa el valor en la tabla notificacion
    private void Notificar(String valor) {
        DocumentReference bd_ = db.document("Notificacion/1");
        Map<String, Object> obj = new HashMap<>();
        obj.put("notificacion", valor);

        bd_.set(obj)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MainActivity.this, "Notificación enviada", Toast.LENGTH_LONG).show();
                        textViewData.setText(textViewData.getText() + "\n" + "Notificación enviada");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "ERROR", Toast.LENGTH_LONG).show();
                        textViewData.setText(textViewData.getText() + "\n" + "ERROR: Falló al enviar la notificación");
                        Log.d(TAG, e.toString());
                    }
                });
    }

    //crea un conexion de salida segura para el dispositivo usando el servicio UUID
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    // Setea el valor de la tabla de Sensores despues de un evento
    private void update_estado_puerta_Sensor_acelerometro() {
        DocumentReference bd_ = db.document("Sensores/1");

        Map<String, Object> sensores = new HashMap<>();
        sensores.put(ESTADOPUERTA, "45");

        bd_.set(sensores)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MainActivity.this, "Se puso un 45 en el estado_puerta", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "ERROR", Toast.LENGTH_LONG).show();
                        textViewData.setText(textViewData.getText() + "\n" + "ERROR: No se puso un 45 en el estado_puerta");
                        Log.d(TAG, e.toString());
                    }
                });
    }


    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;

        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Se mantiene en modo escucha para determinar el ingreso de datos
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    // Envia los datos obtenidos hacia el evento via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        //Envio de trama
        public void write(String input) {
            try {
                mmOutStream.write(input.getBytes());
            } catch (IOException e) {
                //si no es posible enviar datos se cierra la conexión
                Toast.makeText(getBaseContext(), "La Conexión falló", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}