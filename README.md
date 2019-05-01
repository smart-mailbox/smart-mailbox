Materia: Sistemas Operativos Avanzados
Trabajo Práctico: Sistemas Embebidos, Android Y HPC.

Integrantes:

Celestino, Gustavo.
Fontán, Andres.
Gutiérrez, Ruben.
Montenegro, Diego.
Salmin, Silvio.

------------------------------------------------------------------------------------------------
BUZÓN INTELIGENTE


El Buzón Inteligente permite el ingreso y egreso de correspondencia de manera segura, gestionando los pedidos que utilicen tracking, evitando la necesidad de estar presente al momento de recibirlos. Mediante diferentes sensores, mantiene informado al usuario a través de la APP.

Sensores y Actuadores a utilizar:

1 Sensor de distancia
1 Sensor de temperatura
1 Balanza
2 LED
2 Servo motor
1 Lector de codigo de barras
1 Sensor óptico

Aplicaciones:

INGRESO DE CORRESPONDENCIA

Aproximar la Entrega al Lector de Codigo de Barras
El Sensor de Distancia activa el Lector cuando la Entrega está a una distancia menor a 10 cm.
El Lector lee el tracking y envía el dato al Arduino
Arduino verifica si el código recibido coincide con alguno de los tracking almacenados. 
Si coincide, envía una señal al Servo Motor de Ingreso para que rote 90° (el ingreso de correspondencia queda habilitado) y enciende el LED Verde.
Si no coincide, enciende el LED Rojo.
 Se ingresa el Paquete al buzón a través de la Puerta de Ingreso.
El Paquete cae sobre la Balanza.
La Balanza toma el peso y envía el dato al Arduino.
El Sensor de Temperatura realiza una lectura y envía el dato al Arduino
Transcurridos 30 segundos de la habilitación de ingreso, Arduino envía una señal al Servo Motor de Ingreso para que retorne a su estado inicial.
Si existe una diferencia de peso en la Balanza y el Servo Motor de Ingreso volvió a su estado inicial, Arduino registra el ingreso del Paquete.
Arduino guarda el peso junto con el tracking y lo envía junto con la fecha y hora a la APP.
Si la temperatura es mayor a 30°, Arduino envía una notificación a la APP informando que es un paquete caliente.



EGRESO DE CORRESPONDENCIA

Desde la APP se envía una señal al Arduino para habilitar la Puerta de Egreso.
Arduino envía una señal al Servo Motor de Egreso para que rote 90° ( el egreso de correspondencia queda habilitado ).
Se extrae uno , varios o la totalidad de los paquetes.
Al cerrarse la Puerta de Egreso, el Sensor Óptico cambia su estado.
Arduino envía una Señal al Servo Motor de Egreso para retornar a su estado inicial ( egreso de correspondencia deshabilitado ).
Si transcurren más de 30 segundos sin cerrar la puerta, Arduino envía una notificación a la APP informando que la Puerta de Egreso quedó abierta.
Arduino calcula la cantidad de correspondencia que queda en el Buzón Inteligente y envía la información a la APP. 


