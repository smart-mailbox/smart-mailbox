package clases;

public class PaqueteEnBuzon {
    private String descripcion ;
    private String peso;

    public PaqueteEnBuzon(String descripcion, String peso) {
        this.descripcion = descripcion;
        this.peso = peso;
    }
    public PaqueteEnBuzon(){

    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getPeso() {
        return peso;
    }

}
