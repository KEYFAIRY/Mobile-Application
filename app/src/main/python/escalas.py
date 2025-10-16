from music21 import scale

def obtener_escalas():
    traducciones_escalas = {
        'MajorScale': 'Mayor',
        'HarmonicMinorScale': 'Menor armónica'
    }

    notas_latinas = {
        "C": "Do",  "C#": "Do#",  "D-": "Reb",
        "D": "Re",  "D#": "Re#",  "E-": "Mib",
        "E": "Mi",  "F": "Fa",    "F#": "Fa#", "G-": "Solb",
        "G": "Sol", "G#": "Sol#", "A-": "Lab",
        "A": "La",  "A#": "La#",  "B-": "Sib",
        "B": "Si"
    }

    nombres_clases = list(traducciones_escalas.keys())
    notas_base = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"]

    resultado = []
    for nota in notas_base:
        nota_latina = notas_latinas[nota]
        for clase in nombres_clases:
            clase_escala = getattr(scale, clase)
            obj_escala = clase_escala(nota)
            notas = obj_escala.getPitches(f'{nota}4', f'{nota}5')
            notas_texto = []
            for n in notas:
                clave = n.name
                nombre = notas_latinas.get(clave, clave)
                notas_texto.append(f"{nombre}{n.octave}")
            nombre_escala = f"{nota_latina} {traducciones_escalas[clase]}"
            resultado.append(f"{nombre_escala}: {', '.join(notas_texto)}")

    return resultado
