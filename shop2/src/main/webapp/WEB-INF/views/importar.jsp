<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" %>
<%@ taglib prefix="c"  uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8"/>
  <title>Importar Cesta Básica (DIEESE)</title>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"/>
  <style>
    .table-sm td, .table-sm th { padding: .5rem .75rem; }
  </style>
  <script>
    function toggleAll(source){
      document.querySelectorAll("input[name='sel[]']").forEach(c => c.checked = source.checked);
    }
    function montarHiddenSelecionados(){
      const form = document.getElementById("form-salvar");
      // limpa antigos
      document.querySelectorAll(".dyn").forEach(e=>e.remove());

      const sel = document.querySelectorAll("input[name='sel[]']:checked");
      if(sel.length===0){ alert("Selecione ao menos um item."); return false; }

      sel.forEach(ch => {
        const i = ch.value;
        const tr = document.querySelector(`tr[data-i='${i}']`);
        const municipio = tr.querySelector(".col-municipio").textContent.trim();
        const estado    = tr.querySelector(".col-estado").textContent.trim();
        const mesAno    = tr.querySelector(".col-mes").textContent.trim();
        const valor     = tr.querySelector(".col-valor").textContent.trim().replace(",", ".");

        [["municipio[]",municipio],["estado[]",estado],["mesAno[]",mesAno],["valor[]",valor],["sel[]",i]]
        .forEach(([name,value])=>{
          const h = document.createElement("input");
          h.type="hidden"; h.className="dyn"; h.name=name; h.value=value;
          form.appendChild(h);
        });
      });
      return true;
    }
  </script>
</head>
<body class="bg-light">
<div class="container py-4">
  <h1 class="h5 mb-3">Importar dados – DIEESE</h1>

  <!-- Formulário de busca -->
  <form method="post" action="${pageContext.request.contextPath}/cesta/buscar" class="row g-3 align-items-end">
    <div class="col-sm-3">
      <label for="mesIni" class="form-label">Mês/Ano inicial (mmaaaa)</label>
      <input type="text" id="mesIni" name="mesAnoInicial" maxlength="6" class="form-control" required value="${mesIni}">
    </div>
    <div class="col-sm-3">
      <label for="mesFim" class="form-label">Mês/Ano final (mmaaaa)</label>
      <input type="text" id="mesFim" name="mesAnoFinal" maxlength="6" class="form-control" required value="${mesFim}">
    </div>
    <div class="col-sm-3">
      <button type="submit" class="btn btn-primary">Buscar</button>
      <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/">Voltar</a>
    </div>
  </form>

  <!-- Preview -->
  <c:if test="${not empty preview}">
    <div class="d-flex justify-content-between align-items-center mt-4 mb-2">
      <h2 class="h6 mb-0">Pré-visualização</h2>
      <label class="small"><input type="checkbox" onclick="toggleAll(this)"> Selecionar todos</label>
    </div>

    <form id="form-salvar" method="post" action="${pageContext.request.contextPath}/cesta/salvar" onsubmit="return montarHiddenSelecionados()">
      <input type="hidden" name="mesIni" value="${mesIni}">
      <input type="hidden" name="mesFim" value="${mesFim}">

      <div class="table-responsive border bg-white rounded">
        <table class="table table-sm mb-0">
          <thead class="table-light">
          <tr>
            <th style="width:40px;">Sel.</th>
            <th>Município</th>
            <th>UF</th>
            <th>Mês/Ano</th>
            <th class="text-end">Valor</th>
          </tr>
          </thead>
          <tbody>
          <c:forEach items="${preview}" var="p" varStatus="st">
            <tr data-i="${st.index}">
              <td class="text-center"><input type="checkbox" name="sel[]" value="${st.index}"></td>
              <td class="col-municipio"><c:out value="${p.municipio}"/></td>
              <td class="col-estado"><c:out value="${empty p.estado ? '' : p.estado}"/></td>
              <td class="col-mes"><c:out value="${p.mesAno}"/></td>
              <td class="col-valor text-end"><c:out value="${p.valor}"/></td>
            </tr>
          </c:forEach>
          </tbody>
        </table>
      </div>

      <div class="mt-3">
        <button type="submit" class="btn btn-success">Confirmar & Salvar</button>
      </div>
    </form>
  </c:if>

  <!-- Mensagem pós-salvar -->
  <c:if test="${salvo ne null}">
    <div class="alert alert-info mt-3">
      Processados: <strong>${salvo.processados}</strong> — Mergeados (criadas/atualizadas): <strong>${salvo.mergeados}</strong>
    </div>
  </c:if>
</div>
</body>
</html>
