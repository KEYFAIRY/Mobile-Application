import matplotlib.pyplot as plt
import io
import base64
import pandas as pd


# --------------------------
# 🔹 Función utilitaria
# --------------------------
def fig_to_base64(fig):
    buf = io.BytesIO()
    fig.savefig(buf, format='png', bbox_inches='tight')
    buf.seek(0)
    img_base64 = base64.b64encode(buf.read()).decode('utf-8')
    plt.close(fig)
    return img_base64


# ======================================================
# 🔸 1. Top Escalas Semanales
# ======================================================
def top_escalas_graph(data):
    df = pd.DataFrame(data)
    df = df.sort_values('vecesPracticada', ascending=False)

    fig, ax = plt.subplots(figsize=(4, 3))
    bars = ax.barh(df['escala'], df['vecesPracticada'], color='#8B0000')
    ax.set_xlabel('Veces practicada')
    ax.set_ylabel('Escala')
    ax.set_title('Escalas más practicadas', fontsize=12, color='maroon')

    for bar in bars:
        ax.text(bar.get_width() + 0.1, bar.get_y() + bar.get_height()/2,
                f'{int(bar.get_width())}', va='center')

    plt.tight_layout()
    return fig_to_base64(fig)


# ======================================================
# 🔸 2. Tiempo en buena y mala postura
# ======================================================
def posturas_graph(data):
    df = pd.DataFrame(data)
    df = df.sort_values('tiempoTotalSegundos', ascending=False)

    fig, ax = plt.subplots(figsize=(5, 3))

    ax.barh(df['escala'], df['tiempoMalaPosturaSegundos']/60,
            label='Tiempo en mala postura (min)', color='#d19a9a')
    ax.barh(df['escala'], df['tiempoBuenaPosturaSegundos']/60,
            label='Tiempo en buena postura (min)', color='#580F0F', left=df['tiempoMalaPosturaSegundos']/60)

    ax.set_xlabel('Tiempo (min)')
    ax.set_ylabel('Escala')
    ax.set_title('Tiempo en buena y mala postura', fontsize=11)
    ax.legend(fontsize=8)

    plt.tight_layout()
    return fig_to_base64(fig)


# ======================================================
# 🔸 3. Notas correctas e incorrectas
# ======================================================
def notas_graph(data):
    df = pd.DataFrame(data)

    fig, ax = plt.subplots(figsize=(5, 3))
    ax.barh(df['escala'], df['notasIncorrectas'], color='#ffbaba', label='Notas incorrectas')
    ax.barh(df['escala'], df['notasCorrectas'], color='#8B0000', label='Notas correctas', left=df['notasIncorrectas'])

    ax.set_xlabel('Cantidad')
    ax.set_ylabel('Escala')
    ax.set_title('Cantidad de notas correctas e incorrectas', fontsize=11)
    ax.legend(fontsize=8)

    plt.tight_layout()
    return fig_to_base64(fig)


# ======================================================
# 🔸 4. Errores Posturales Semanales
# ======================================================
def errores_posturales_graph(data):
    df = pd.DataFrame(data)
    df = df.sort_values('dia')

    fig, ax = plt.subplots(figsize=(5, 3))
    ax.plot(df['dia'], df['totalErroresPosturales'], marker='o', color='#577BC1', linewidth=2)

    escala = df['escala'].iloc[0] if not df.empty else ''
    ax.set_title(f"Progreso en errores posturales: {escala}", fontsize=11)
    ax.set_xlabel("Fecha")
    ax.set_ylabel("Cantidad de errores")

    plt.tight_layout()
    return fig_to_base64(fig)


# ======================================================
# 🔸 5. Errores Musicales Semanales
# ======================================================
def errores_musicales_graph(data):
    df = pd.DataFrame(data)
    df = df.sort_values('dia')

    fig, ax = plt.subplots(figsize=(5, 3))
    ax.plot(df['dia'], df['totalErroresMusicales'], marker='o', color='#9E77F4', linewidth=2)

    escala = df['escala'].iloc[0] if not df.empty else ''
    ax.set_title(f"Progreso en errores musicales: {escala}", fontsize=11)
    ax.set_xlabel("Fecha")
    ax.set_ylabel("Cantidad de errores")

    plt.tight_layout()
    return fig_to_base64(fig)
