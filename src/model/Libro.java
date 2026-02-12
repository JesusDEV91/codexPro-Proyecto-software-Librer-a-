package model;

import java.time.LocalDate;


public class Libro {
    private int id;
    private String titulo;
    private String autor;
    private String categoria;
    private int stock;
    private LocalDate fechaRegistro; 

    public Libro(int id, String titulo, String autor, String categoria, int stock, LocalDate fechaRegistro) {
        this.id = id;
        this.titulo = titulo;
        this.autor = autor;
        this.categoria = categoria;
        this.stock = stock;
        this.fechaRegistro = fechaRegistro;
    }

    
    public int getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getAutor() { return autor; }
    public String getCategoria() { return categoria; }
    public int getStock() { return stock; }
    public LocalDate getFechaRegistro() { return fechaRegistro; }
}