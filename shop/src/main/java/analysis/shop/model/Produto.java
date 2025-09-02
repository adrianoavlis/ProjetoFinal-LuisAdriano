package analysis.shop.model;

public class Produto {
    
    private String id;            
    private String nome;           
    private String descricao;      
    private String categoria;       
    private double preco;          
    private String sku;            
    private int quantidadeEmEstoque;
    

    private String marca;          
    private double peso;           
    private String cor;            
    private String tamanho;         

    
    private String imagemUrl;     

    
    private double precoDesconto; 
    private boolean ativo;         
    private double avaliacao;       
    private int numeroDeVendas;     

    
    public Produto(String id, String nome, String descricao, String categoria, double preco, String sku, 
                   int quantidadeEmEstoque, String marca, double peso, String cor, String tamanho, 
                   String imagemUrl) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.preco = preco;
        this.sku = sku;
        this.quantidadeEmEstoque = quantidadeEmEstoque;
        this.marca = marca;
        this.peso = peso;
        this.cor = cor;
        this.tamanho = tamanho;
        this.imagemUrl = imagemUrl;
        this.precoDesconto = 0.0; // Inicialmente sem desconto
        this.ativo = true; // Produto ativo por padrão
        this.avaliacao = 0.0; 
        this.numeroDeVendas = 0; 
    }
    
    public Produto() {
    	super();
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public double getPreco() {
        return preco;
    }

    public void setPreco(double preco) {
        this.preco = preco;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantidadeEmEstoque() {
        return quantidadeEmEstoque;
    }

    public void setQuantidadeEmEstoque(int quantidadeEmEstoque) {
        this.quantidadeEmEstoque = quantidadeEmEstoque;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public double getPeso() {
        return peso;
    }

    public void setPeso(double peso) {
        this.peso = peso;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public String getTamanho() {
        return tamanho;
    }

    public void setTamanho(String tamanho) {
        this.tamanho = tamanho;
    }

    public String getImagemUrl() {
        return imagemUrl;
    }

    public void setImagemUrl(String imagemUrl) {
        this.imagemUrl = imagemUrl;
    }

    public double getPrecoDesconto() {
        return precoDesconto;
    }

    public void setPrecoDesconto(double precoDesconto) {
        this.precoDesconto = precoDesconto;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public double getAvaliacao() {
        return avaliacao;
    }

    public void setAvaliacao(double avaliacao) {
        this.avaliacao = avaliacao;
    }

    public int getNumeroDeVendas() {
        return numeroDeVendas;
    }

    public void setNumeroDeVendas(int numeroDeVendas) {
        this.numeroDeVendas = numeroDeVendas;
    }
}
