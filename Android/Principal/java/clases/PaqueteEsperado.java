package clases;

public class PaqueteEsperado {

    private String descripcion ;
    private String tipo_temperatura;

    public PaqueteEsperado(){
        //constructor sin nada de argumento
    }
    public PaqueteEsperado(String descripcion, String tipo_temperatura){
        this.descripcion = descripcion;
        this.tipo_temperatura = tipo_temperatura;
    }

    public String getTipo_temperatura() {
        return tipo_temperatura;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
