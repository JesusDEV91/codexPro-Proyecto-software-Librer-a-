package model;



public class Libro {
    private  int id;
    private String titulo;
    private String autor;
    private String categoria;
    private int stock;

    public Libro(int id, String titulo, String autor, String categoria, int stock) {
        this.id = id;
        this.titulo = titulo;
        this.autor = autor;
        this.categoria = categoria;
        this.stock = stock;
    }


    public int getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getAutor() { return autor; }
    public String getCategoria() { return categoria; }
    public int getStock() { return stock; }

    @Override
    public String toString() {
        return "Libro{" + "id=" + id + ", titulo=" + titulo + ", autor=" + autor + ", categoria=" + categoria + ", stock=" + stock + '}';
    }
    
    
}