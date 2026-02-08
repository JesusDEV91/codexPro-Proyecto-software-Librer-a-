package model;

import java.time.LocalDate;


public class Prestamo {
    private int id;
    private int idLibro;
    private int idSocio;
    private String tituloLibro;
    private String nombreSocio; 
    private LocalDate fechaSalida;
    private LocalDate fechaDevolucion;

    public Prestamo(int id, int idLibro, int idSocio, String tituloLibro, String nombreSocio, LocalDate fechaSalida, LocalDate fechaDevolucion) {
        this.id = id;
        this.idLibro = idLibro;
        this.idSocio = idSocio;
        this.tituloLibro = tituloLibro;
        this.nombreSocio = nombreSocio;
        this.fechaSalida = fechaSalida;
        this.fechaDevolucion = fechaDevolucion;
    }

    
    public int getId() { return id; }
    public String getTituloLibro() { return tituloLibro; }
    public String getNombreSocio() { return nombreSocio; }
    public LocalDate getFechaSalida() { return fechaSalida; }
    public LocalDate getFechaDevolucion() { return fechaDevolucion; }
    
  
    public int getIdLibro() { return idLibro; }
    public int getIdSocio() { return idSocio; }
}