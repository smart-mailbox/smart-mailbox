package clases;

public class Buzon {

    private String cant_paquetes ;
    private String humedad ;
    private String peso_total;
    private String temperatura;
    private String codigo;

    public Buzon(){
        //constructor sin nada de argumento
    }

    public Buzon(String cant_paquetes, String codigo, String humedad, String peso_total, String temperatura) {
        this.cant_paquetes = cant_paquetes;
        this.codigo = codigo;
        this.humedad = humedad;
        this.peso_total = peso_total;
        this.temperatura = temperatura;
    }

    public String getCant_Paquetes() {
        return cant_paquetes;
    }

    public String getHumedad() {
        return humedad;
    }

    public String getPeso_total() {
        return peso_total;
    }

    public String getTemperatura() {
        return temperatura;
    }

    public String getgCodigo() {
        return codigo;
    }
}
