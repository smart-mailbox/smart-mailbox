/* Inclusión de bibliotecas */
#include <Servo.h>
#include <DHT.h> //Humedad
#include <HX711_ADC.h> //Balanza
#define DHTPIN 13           // Definimos el pin digital donde se conecta el sensor de humedad
#define DHTTYPE DHT11       // Dependiendo del tipo de sensor

/* Variables Globales */
long millisBalanza;
float humedad, temp, pesoBal, pesoPaq = 0;
boolean flagPuerta   = false;
boolean flagOK       = false;
boolean flagShake    = false;
int flagOptico       = 1;
int medicionBalanza  = 0;
int intensidad       = 0;
String inputString   = "" ;
String stringAMandar = "" ;

/* Ubicación de Pines */
int sensorOptico     = 7;
int redLight         = 6;
int greenLight       = 5;
int blueLiight       = 3;
const int Trigger    = 2;    //Pin digital 2 para el Trigger del sensor distancia
const int Echo       = 9;    //Pin digital 3 para el echo del sensor distancia
const int pinRele    = 4;


HX711_ADC Balanza(A0, A1);  //(dout pin, sck pin) pines que utilza la balanza
Servo servoMotor;           // crea el objeto para el servo
DHT dht(DHTPIN, DHTTYPE);   // Inicializamos el sensor DHT11

void setup()
{
  /*Configuración de comunicación */
  Serial.begin(9600);  //iniciailzamos la comunicación
  Serial1.begin(9600); //Bluetooth
  Serial2.begin(9600); //Lectora

  /*Configuración de sensores */
  Serial1.println("MODULO CONECTADO x BLUETOOTH");
  pinMode(pinRele,  OUTPUT) ;
  pinMode(Trigger, OUTPUT);   //pin como salida
  pinMode(Echo, INPUT);       //pin como entrada
  digitalWrite(Trigger, LOW); //Inicializamos el pin con 0
  servoMotor.attach(10);      //inicializo Servo
  pinMode(redLight, OUTPUT);
  pinMode(greenLight, OUTPUT);
  pinMode(blueLiight, OUTPUT);
  pinMode(sensorOptico, INPUT);

  dht.begin();      // Comenzamos el sensor DHT Humedad y Temp
  inicia_balanza(); //Inicializa la calibración de la balanza
  lee_temperatura();
}

void loop()
{
  /* Variables para la lectura de la distancia */
  long disTiempo;                   //tiempo que demora en llegar el eco
  long disMedida;                   //distancia en centimetros

  digitalWrite(Trigger, HIGH);      //trigger sensor distancia
  delayMicroseconds(10);            //Enviamos un pulso de 10us
  digitalWrite(Trigger, LOW);

  leeSensorOptico();                // Leemos el valor del sensor optico
  disTiempo = pulseIn(Echo, HIGH);  // Obtenemos el ancho del pulso
  disMedida = disTiempo / 59;       // Escalamos el tiempo a una distancia en cm

  lecturaProx(disMedida);           // Leemos el sensor de proximidad
  readBluetooh();
}

/////////////////////////////////////////////////////////FUNCIONES///////////////////////////////////////////////////////////////////////
void leeSensorOptico()
{
  String Cadena;
  flagOptico = digitalRead(sensorOptico);            // 1 cerrado - 0 abierto

  if ( !flagPuerta && flagOptico  ==  0 )           // Se abrió la puerta
    flagPuerta  = true;
  else
  {
    if ( flagOptico == 1 && flagOK  && flagPuerta )
    {
      flagPuerta  = false;
      flagOK      = false;
      servoMotor.write(90);       // Deshabilita la apertura de puerta
      RGB_color(0, 0, 0);         // Se apaga el Led
      pesoBal = lee_balanza();    // Lectura del peso que hay en el paquete
      pesoPaq = pesoBal - pesoPaq;// Peso del paquete ingresado o retirado
      /* Si el peso en negativo entonces se retiro uno o mas paquetes */
      if ( pesoPaq < 0 )
      {
        stringAMandar = "O|";
        pesoPaq = pesoPaq * -1;
        lee_temperatura();          // Humedad y temperatura del Buzon
        mandarDatos();
      }
      else
        /* Si el peso es positivo entonces se ingreso un paquete */
        if ( pesoPaq > 0 )
        {
          stringAMandar = "I|";
          lee_temperatura();          // Humedad y temperatura del Buzon
          mandarDatos();
        }
      pesoPaq = pesoBal;
    }
  }
}


void lecturaProx(int distancia) {
  /* Si la distancia es menor a 30cm se habilta el lector de codigos de barra */
  if ( distancia <= 30 )
  {
    digitalWrite(pinRele, HIGH);  // el relé habilta el lector de codigos de barra
    leerCodigoBarras();           // Lectuar del codigo de barras.
  }
  else {
    if (distancia > 30 )
    {
      digitalWrite(pinRele, LOW);   // Codigo de barras deshabilitado
      //RGB_color(0, 255, 0);         // Led Verde: Buzón habilitado
    }
  }
}

void leerCodigoBarras()
{
  char codigo;
  if ( Serial2.available() > 0 )
  {
    Serial2.flush();
    codigo = (char)Serial2.read();
    if (codigo == '\r')
    {
      Serial.println("C|" + inputString); //MONITOR SERIE codigo de barras
      Serial1.println("C|" + inputString); //Bluetooth codigo de barras
      Serial1.flush();
      inputString = "";
    }
    else
      inputString += codigo;
  }
}


void RGB_color(int red_light_value, int green_light_value, int blue_light_value)
{
  analogWrite(redLight, red_light_value);
  analogWrite(greenLight, green_light_value);
  analogWrite(blueLiight, blue_light_value);
}
void lee_temperatura()
{
  humedad  = dht.readHumidity();  // Leemos la humedad relativa
  temp = dht.readTemperature();   // Leemos la temperatura en grados centígrados (por defecto)

  /* Comprobamos si ha habido algún error en la lectura */
  if (isnan(humedad) || isnan(temp))
  {
    Serial.println("Error obteniendo los datos del sensor DHT11");
    return;
  }

  Serial.print("Humedad: ");
  Serial.print(humedad);
  Serial.print(" %\t");
  Serial.print("Temperatura: ");
  Serial.print(temp);
  Serial.println(" *C ");
}

void inicia_balanza()
{
  float factor_de_calibracion = -227.83;          //Constante obtenida en la calibracion de la balanza

  Serial.println("Comenzando...");
  Balanza.begin();
  long tiempoParaTarar = 4000;                    //tiempo para luego Tarar la balanza
  Balanza.start(tiempoParaTarar);
  if (Balanza.getTareTimeoutFlag())
    Serial.println("timeout para Tarar, verificar cableado");
  else
  {
    Balanza.setCalFactor(factor_de_calibracion);  //Se setea en la balanza el valor obtenido en la calibracion
    Serial.println("Iniciacion + tara completada");
  }
}

float lee_balanza()
{
  int medicion = 0; //numero de medicion
  float peso = 0;
  while (medicion < 20) //obtengo 20 mediciones
  {
    if (millis() > millisBalanza + 250) //mido cada 250ms
    {
      Balanza.update();
      peso = Balanza.getData();

      if (peso < 50) //si el valor obtenido es < 50 gr, asumo que esta vacio el buzon
        peso = 0;

      millisBalanza = millis();
      medicion++;
    }

  }
  return peso;
}

void mandarDatos()
{

  String Cadena = stringAMandar  + humedad + "|" + temp + "|" + pesoPaq + "|" + pesoBal;
  Serial1.println(Cadena);  // Se envia la cadena por Bluetooth
  Serial.println(Cadena);   // Se muestra la cadena en el monitor serial
}

void readBluetooh()
{
  if ( Serial1.available() > 0 )
  {
    Serial.println("Entro a la lectura del Bluetooh");
    char respuesta = Serial1.read();
    Serial.println(respuesta);
    Serial1.flush();
    /* Si el codigo coincide se habilita la apertura de la puerta */
    if ( respuesta == '1' ) // se cambia la sentencia por lectura del bluetooh
    {
      flagOK = true;
      RGB_color(0, 255, 0); // Preden la luz verde
      servoMotor.write(0);  // Habilita la apertura de puerta
    }
    if (respuesta == '0')
      RGB_color(0, 0, 255); // Codigo incorrecto: Led Rojo
    /* Respuesta del acelerometro */
    if (respuesta == '2')
    {
      flagOK = true;
      RGB_color(255, 255, 255); // Preden la luz cyan
      servoMotor.write(0);  // Habilita la apertura de puerta
    }
    /* Respuesta del sensor de proximidad */
    if (respuesta == '3')
    {
      RGB_color(255, 0, 0);
      stringAMandar = "H|";
      lee_temperatura();          
      mandarDatos();
    }
  }
}
