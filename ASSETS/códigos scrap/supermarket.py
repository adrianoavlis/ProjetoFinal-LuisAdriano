from urllib.request import urlopen
from bs4 import BeautifulSoup
import pandas as pd

# URL do site
url = 'https://redesupermarket.com.br/ofertas/'
response = urlopen(url)
html = response.read()
soup = BeautifulSoup(html, 'html.parser')

# Declarando variável cards
cards = []
anuncios = soup.find_all('article', class_='card-product')

for anuncio in anuncios:
    card = {}
    
    # Selos
    seal_tag = anuncio.find('span', class_='card-product__seal-inner')
    if seal_tag:
        card['seal'] = seal_tag.get_text(strip=True)
    else:
        card['seal'] = 'N/A'

    # Imagem
    image_tag = anuncio.find('figure', class_='card-product__image').find('img')
    if image_tag:
        card['image'] = image_tag['src']
    else:
        card['image'] = 'N/A'
    
    # Título do Produto
    title_tag = anuncio.find('h3', class_='card-product__title')
    if title_tag:
        card['name'] = title_tag.get_text(strip=True)
    else:
        card['name'] = 'N/A'
    
    # Preço
    price_tag = anuncio.find('span', class_='card-product__price')
    if price_tag:
        price_text = price_tag.get_text(strip=True)
        card['price'] = price_text.replace('\n', '').replace(' ', '')
    else:
        card['price'] = 'N/A'
    
    # Adicionando resultado à lista cards
    cards.append(card)

# Criando um DataFrame com os resultados
dataset = pd.DataFrame(cards)

# Salvando como CSV no caminho especificado
# dataset.to_csv(r'C:\Users\PC\Projetos\Projeto-Final---Luis-Adriano-\ASSETS\data\WebData\supermarket\datasetSupermarket.csv', sep=';', index=False, encoding='utf-8-sig')

# Salvando como XLS no caminho especificado
# dataset.to_excel(r'C:\Users\PC\Projetos\Projeto-Final---Luis-Adriano-\ASSETS\data\WebData\supermarket\datasetSupermarket.xlsx', index=False)

print(dataset)
