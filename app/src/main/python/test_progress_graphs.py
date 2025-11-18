import pytest
import base64

from progress_charts import (
    top_escalas_graph,
    errores_posturales_graph,
    errores_musicales_graph,
    posturas_graph,
    notas_graph
)

# 🔹 Clase mock para simular objetos Java con getters
class MockItem:
    def __init__(self, **kwargs):
        self.attrs = kwargs

    def __getattr__(self, name):
        def getter():
            return self.attrs.get(name[3:], None)  # soporta getEscala(), getDia(), etc.
        return getter


# ==========================================================
# 🔸 Función auxiliar para validar resultado gráfico
# ==========================================================
def validar_imagen_base64(resultado):
    assert isinstance(resultado, str)
    decoded = base64.b64decode(resultado)
    assert decoded.startswith(b"\x89PNG")  # PNG signature


# ==========================================================
# 🔹 1. top_escalas_graph
# ==========================================================
def test_top_escalas_graph_generates_image():
    datos = [
        MockItem(Escala="Do Mayor", VecesPracticada=3),
        MockItem(Escala="Re Menor", VecesPracticada=2),
        MockItem(Escala="Mi Mayor", VecesPracticada=4),
    ]
    result = top_escalas_graph(datos)
    validar_imagen_base64(result)


def test_top_escalas_graph_empty_data():
    result = top_escalas_graph([])
    validar_imagen_base64(result)


# ==========================================================
# 🔹 2. errores_posturales_graph
# ==========================================================
def test_errores_posturales_graph_generates_image():
    datos = [
        MockItem(Escala="Do Mayor", TotalErroresPosturales=5, Dia="2025-11-01"),
        MockItem(Escala="Re Menor", TotalErroresPosturales=3, Dia="2025-11-02"),
    ]
    result = errores_posturales_graph(datos)
    validar_imagen_base64(result)


def test_errores_posturales_graph_empty_data():
    result = errores_posturales_graph([])
    validar_imagen_base64(result)


# ==========================================================
# 🔹 3. errores_musicales_graph
# ==========================================================
def test_errores_musicales_graph_generates_image():
    datos = [
        MockItem(Escala="Do Mayor", TotalErroresMusicales=4, Dia="2025-11-01"),
        MockItem(Escala="Re Menor", TotalErroresMusicales=6, Dia="2025-11-02"),
    ]
    result = errores_musicales_graph(datos)
    validar_imagen_base64(result)


def test_errores_musicales_graph_empty_data():
    result = errores_musicales_graph([])
    validar_imagen_base64(result)


# ==========================================================
# 🔹 4. posturas_graph
# ==========================================================
def test_posturas_graph_generates_image():
    datos = [
        MockItem(Escala="Do Mayor", TiempoMalaPosturaSegundos=120, TiempoBuenaPosturaSegundos=300),
        MockItem(Escala="Re Menor", TiempoMalaPosturaSegundos=90, TiempoBuenaPosturaSegundos=180),
    ]
    result = posturas_graph(datos)
    validar_imagen_base64(result)


def test_posturas_graph_empty_data():
    result = posturas_graph([])
    validar_imagen_base64(result)


# ==========================================================
# 🔹 5. notas_graph
# ==========================================================
def test_notas_graph_generates_image():
    datos = [
        MockItem(Escala="Do Mayor", NotasCorrectas=80, NotasIncorrectas=20),
        MockItem(Escala="Re Menor", NotasCorrectas=60, NotasIncorrectas=10),
    ]
    result = notas_graph(datos)
    validar_imagen_base64(result)


def test_notas_graph_empty_data():
    result = notas_graph([])
    validar_imagen_base64(result)
