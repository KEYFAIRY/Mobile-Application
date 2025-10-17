import matplotlib.pyplot as plt
import matplotlib.dates as mdates
import pandas as pd
from datetime import datetime
import io
import base64


def _convertir_a_lista_python(datos):
    if hasattr(datos, 'toArray'):
        print(f"[PYTHON LOG {datetime.now().strftime('%H:%M:%S')}] Detectado ArrayList Java, convirtiendo a lista Python...")
        datos = list(datos.toArray())
    return datos


def _mostrar_y_guardar(nombre, fig):
    buffer = io.BytesIO()
    fig.savefig(buffer, format="png")
    buffer.seek(0)
    img_str = base64.b64encode(buffer.getvalue()).decode("utf-8")
    plt.close(fig)
    print(f"[PYTHON LOG {datetime.now().strftime('%H:%M:%S')}] ✅ Gráfico '{nombre}' generado correctamente")
    return img_str

def _grafico_sin_datos(nombre, mensaje="Sin datos disponibles"):
    fig, ax = plt.subplots(figsize=(4, 3))
    ax.text(0.5, 0.5, mensaje, ha="center", va="center",
            fontsize=12, color="gray", fontweight="bold")
    ax.axis("off")
    return _mostrar_y_guardar(nombre, fig)

def top_escalas_graph(datos):
    try:
        print(f"[PYTHON LOG {datetime.now().strftime('%H:%M:%S')}] 🔸 Llamada a top_escalas_graph")

        datos = _convertir_a_lista_python(datos)

        if not datos:
            print("[PYTHON LOG] ⚠️ Lista vacía recibida en top_escalas_graph.")
            return _grafico_sin_datos("errores_posturales")

        datos = [dict(
            escala=str(item.getEscala()),
            vecesPracticada=int(item.getVecesPracticada())
        ) for item in datos]

        df = pd.DataFrame(datos)
        df = df.sort_values(by="vecesPracticada", ascending=False).reset_index(drop=True)

        if len(df) >= 3:
            orden_podio = [1, 0, 2]
            df = df.iloc[orden_podio]

        colores_podio = ["#a7b9e6", "#7b001c", "#b9824d"]
        colores = (colores_podio * (len(df)//3 + 1))[:len(df)]

        fig, ax = plt.subplots(figsize=(4.5, 3.5))
        bars = ax.bar(df["escala"], df["vecesPracticada"], color=colores, edgecolor="black")

        ax.set_ylabel("")
        ax.set_xlabel("")
        ax.spines["top"].set_visible(False)
        ax.spines["right"].set_visible(False)
        ax.spines["left"].set_visible(False)
        ax.spines["bottom"].set_visible(False)
        ax.tick_params(left=False, bottom=False)
        ax.set_yticks([])

        posiciones = [2, 1, 3]
        for i, bar in enumerate(bars):
            ax.text(
                bar.get_x() + bar.get_width()/2,
                0.05,
                f"{posiciones[i]}",
                ha='center', va='bottom',
                color='black', fontsize=11, fontweight='bold',
                bbox=dict(facecolor='white', edgecolor='black', boxstyle='circle')
            )

        plt.tight_layout(pad=1)

        return _mostrar_y_guardar("top_escalas", fig)

    except Exception as e:
        print(f"[PYTHON LOG] ❌ Error en top_escalas_graph: {e}")


# ==========================================================
# 🔹 2. Errores Posturales
# ==========================================================
def errores_posturales_graph(datos):

    try:
        print(f"[PYTHON LOG] 🔸 Llamada a errores_posturales_graph")
        print(f"[PYTHON LOG] Datos recibidos: {datos}")

        datos = _convertir_a_lista_python(datos)

        if not datos:
            print("[PYTHON LOG] ⚠️ Lista vacía recibida en errores_posturales_graph.")
            return _grafico_sin_datos("errores_posturales")

        datos_procesados = [dict(
            dia=str(item.getDia()),
            totalErroresPosturales=int(item.getTotalErroresPosturales()),
            escala=str(item.getEscala())
        ) for item in datos]

        df = pd.DataFrame(datos_procesados)
        print(f"[PYTHON LOG] DataFrame creado:\n{df}")

        df["dia_dt"] = pd.to_datetime(df["dia"])

        df = df.sort_values(by='dia_dt')

        fig, ax = plt.subplots(figsize=(7, 4))

        LINE_COLOR = '#8AB6F9'

        ax.plot(
            df["dia_dt"],
            df["totalErroresPosturales"],
            marker='o',
            linestyle='-',
            color=LINE_COLOR,
            linewidth=2,
            markersize=5
        )

        ax.set_xlabel("Fecha", fontsize=10, labelpad=15)
        ax.set_ylabel("Cantidad de errores", fontsize=10, labelpad=15)

        ax.xaxis.set_major_formatter(mdates.DateFormatter('%d %b'))

        ax.xaxis.set_major_locator(mdates.DayLocator(interval=1))

        ax.set_xlim(df["dia_dt"].min(), df["dia_dt"].max())

        max_y = df["totalErroresPosturales"].max()
        ax.set_ylim(0, max_y * 1.1)

        from matplotlib.ticker import MaxNLocator
        ax.yaxis.set_major_locator(MaxNLocator(integer=True))

        ax.spines['right'].set_visible(False)
        ax.spines['top'].set_visible(False)

        ax.grid(axis='y', linestyle='-', alpha=0.4, color='gray')
        ax.grid(axis='x', visible=False) # Quitar la cuadrícula vertical si existe

        return _mostrar_y_guardar("errores_posturales", fig)

    except Exception as e:
        print(f"[PYTHON LOG] ❌ Error en errores_posturales_graph: {e}")

# ==========================================================
# 🔹 3. Errores Musicales
# ==========================================================
def errores_musicales_graph(datos):

    try:
        print(f"[PYTHON LOG] 🔸 Llamada a errores_musicales_graph")
        print(f"[PYTHON LOG] Datos recibidos: {datos}")

        datos = _convertir_a_lista_python(datos)

        if not datos:
            print("[PYTHON LOG] ⚠️ Lista vacía recibida en errores_musicales_graph.")
            return _grafico_sin_datos("errores_musicales")

        datos_procesados = [dict(
            escala=str(item.getEscala()),
            totalErroresMusicales=int(item.getTotalErroresMusicales()),
            dia=str(item.getDia())
        ) for item in datos]

        df = pd.DataFrame(datos_procesados)
        print(f"[PYTHON LOG] DataFrame creado:\n{df}")

        df["dia_dt"] = pd.to_datetime(df["dia"])

        df = df.sort_values(by='dia_dt')

        fig, ax = plt.subplots(figsize=(7, 4))

        LINE_COLOR = '#8AB6F9'

        ax.plot(
            df["dia_dt"],
            df["totalErroresMusicales"],
            marker='o',
            linestyle='-',
            color=LINE_COLOR,
            linewidth=2,
            markersize=5
        )

        ax.set_xlabel("Fecha", fontsize=10, labelpad=15)
        ax.set_ylabel("Cantidad de errores", fontsize=10, labelpad=15)

        ax.xaxis.set_major_formatter(mdates.DateFormatter('%d %b'))
        ax.xaxis.set_major_locator(mdates.DayLocator(interval=1))
        ax.set_xlim(df["dia_dt"].min(), df["dia_dt"].max())

        max_y = df["totalErroresMusicales"].max()
        ax.set_ylim(0, max_y * 1.1)

        ax.spines['right'].set_visible(False)
        ax.spines['top'].set_visible(False)

        ax.grid(axis='y', linestyle='-', alpha=0.4, color='gray')
        ax.grid(axis='x', visible=False)

        return _mostrar_y_guardar("errores_musicales", fig)

    except Exception as e:
        print(f"[PYTHON LOG] ❌ Error en errores_musicales_graph: {e}")

# ==========================================================
# 🔹 4. Posturas (Tiempos)
# ==========================================================
def posturas_graph(datos):

    try:
        print(f"[PYTHON LOG] 🔸 Llamada a posturas_graph")
        print(f"[PYTHON LOG] Datos recibidos: {datos}")

        COLOR_MALA_POSTURA = '#E9C4CD'
        COLOR_BUENA_POSTURA = '#8D1E3A'

        datos = _convertir_a_lista_python(datos)

        if not datos:
            print("[PYTHON LOG] ⚠️ Lista vacía recibida en posturas_graph.")
            return _grafico_sin_datos("posturas")


        datos_procesados = [dict(
            escala=str(item.getEscala()),
            tiempoMalaPosturaMinutos=float(item.getTiempoMalaPosturaSegundos()) / 60.0,
            tiempoBuenaPosturaMinutos=float(item.getTiempoBuenaPosturaSegundos()) / 60.0
        ) for item in datos]

        df = pd.DataFrame(datos_procesados)
        print(f"[PYTHON LOG] DataFrame de tiempos en minutos:\n{df}")

        df = df.iloc[::-1]

        fig, ax = plt.subplots(figsize=(6, 4)) # Ajuste de tamaño

        ax.barh(
            df["escala"],
            df["tiempoBuenaPosturaMinutos"],
            color=COLOR_BUENA_POSTURA,
            label="Tiempo en buena postura (min)"
        )

        ax.barh(
            df["escala"],
            df["tiempoMalaPosturaMinutos"],
            left=df["tiempoBuenaPosturaMinutos"],  # Apilamiento horizontal
            color=COLOR_MALA_POSTURA,
            label="Tiempo en mala postura (min)"
        )

        ax.set_xlabel("Tiempo (min)", fontsize=10)
        ax.set_ylabel("", fontsize=10) # Se puede quitar el label del eje Y o dejar vacío

        handles, labels = ax.get_legend_handles_labels()
        ax.legend(
            handles[::-1],
            labels[::-1],
            loc='upper center',
            bbox_to_anchor=(0.5, 1.15),
            ncol=1,
            frameon=False,
            fontsize=9
        )

        ax.spines['right'].set_visible(False)
        ax.spines['top'].set_visible(False)

        return _mostrar_y_guardar("posturas", fig)

    except Exception as e:
        print(f"[PYTHON LOG] ❌ Error en posturas_graph: {e}")


# ==========================================================
# 🔹 5. Notas (Correctas vs Incorrectas)
# ==========================================================
def notas_graph(datos):

    try:
        print(f"[PYTHON LOG] 🔸 Llamada a notas_graph")
        print(f"[PYTHON LOG] Datos recibidos: {datos}")

        COLOR_NOTAS_CORRECTAS = '#E9C4CD'
        COLOR_NOTAS_INCORRECTAS = '#8D1E3A'

        datos = _convertir_a_lista_python(datos)

        if not datos:
            print("[PYTHON LOG] ⚠️ Lista vacía recibida en notas_graph.")
            return _grafico_sin_datos("notas")

        datos_procesados = [dict(
            escala=str(item.getEscala()),
            notasCorrectas=int(item.getNotasCorrectas()),
            notasIncorrectas=int(item.getNotasIncorrectas())
        ) for item in datos]

        df = pd.DataFrame(datos_procesados)
        print(f"[PYTHON LOG] DataFrame creado:\n{df}")

        df = df.iloc[::-1]

        fig, ax = plt.subplots(figsize=(7, 5)) # Ajuste de tamaño

        ax.barh(
            df["escala"],
            df["notasCorrectas"],
            color=COLOR_NOTAS_CORRECTAS,
            label="Notas correctas"
        )

        ax.barh(
            df["escala"],
            df["notasIncorrectas"],
            left=df["notasCorrectas"],  # Apilamiento horizontal
            color=COLOR_NOTAS_INCORRECTAS,
            label="Notas incorrectas"
        )

        ax.set_xlabel("Cantidad de notas", fontsize=10)
        ax.set_ylabel("Escala", fontsize=10)

        handles, labels = ax.get_legend_handles_labels()
        ax.legend(
            handles,
            labels,
            loc='upper center',
            bbox_to_anchor=(0.5, 1.15),
            ncol=2,
            frameon=False,
            fontsize=9
        )

        ax.spines['right'].set_visible(False)
        ax.spines['top'].set_visible(False)

        return _mostrar_y_guardar("notas", fig)

    except Exception as e:
        print(f"[PYTHON LOG] ❌ Error en notas_graph: {e}")