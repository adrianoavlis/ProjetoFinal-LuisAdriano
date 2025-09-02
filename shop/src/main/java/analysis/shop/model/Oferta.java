package analysis.shop.model;

public class Oferta extends Produto {
	
	private String dataValidade = "N/A";

	public Oferta() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Oferta(String id, String nome, String descricao, String categoria, double preco, String sku,
			int quantidadeEmEstoque, String marca, double peso, String cor, String tamanho, String imagemUrl,
			String dataValidade) {
		super(id, nome, descricao, categoria, preco, sku, quantidadeEmEstoque, marca, peso, cor, tamanho, imagemUrl);
		this.dataValidade = dataValidade;
	}

	public Oferta(String id, String nome, String descricao, String categoria, double preco, String sku,
			int quantidadeEmEstoque, String marca, double peso, String cor, String tamanho, String imagemUrl) {
		super(id, nome, descricao, categoria, preco, sku, quantidadeEmEstoque, marca, peso, cor, tamanho, imagemUrl);
		// TODO Auto-generated constructor stub
	}

	public Oferta(String validade) {
		super.setNome(validade);

		this.dataValidade = validade;

	}

	public String getDataValidade() {
		return dataValidade;
	}

	public void setDataValidade(String dataValidade) {
		this.dataValidade = dataValidade;
	}

}
