from music21 import scale

solfege_note_dict = {
    "Do": "C",
    "Do#": "C#",
    "Re": "D",
    "Re#": "D#",
    "Mi": "E",
    "Fa": "F",
    "Fa#": "F#",
    "Sol": "G",
    "Sol#": "G#",
    "La": "A",
    "La#": "A#",
    "Si": "B"
}

def get_num_notes(scale_name, type_scale, octaves):

    scale_name = solfege_note_dict.get(scale_name, scale_name)

    if type_scale == "Mayor":
        s = scale.MajorScale(scale_name)
    elif type_scale == "Menor":
        s = scale.MinorScale(scale_name)
    else:
        raise ValueError("Unknown scale type")

    # Calculate without building the full list
    notes = s.getPitches(f"{scale_name}4", f"{scale_name}5")
    notes_per_octave = len(notes) - 1  # Exclude last note to avoid duplication

    # Formula: (notes_per_octave * octaves) + 1 (peak) + (notes_per_octave * octaves) - 1 (descending without peak)
    total_notes = (notes_per_octave * octaves * 2) + 1

    return total_notes

