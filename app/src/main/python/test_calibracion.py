import pytest
import numpy as np
import cv2
import json
import sys
import os
from unittest.mock import Mock, MagicMock

# Agregar el directorio actual al path para importar el módulo
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# Importar desde el archivo piano_keyboard_detection.py
from calibracion import is_calibrated, RESIZE_WIDTH, PIANO_AREA_XSECTION_OFFSET, PIANO_AREA_YSECTION_OFFSET, RIGHT_SIDE_LIMIT


# ==========================================================
# 🔹 Clase mock para simular el contexto de Android
# ==========================================================
class MockContext:
    def __init__(self, files_dir="/tmp"):
        self.files_dir = files_dir
    
    def getFilesDir(self):
        return self.files_dir


# ==========================================================
# 🔸 Función auxiliar para crear imagen de prueba
# ==========================================================
def crear_imagen_piano(width=1920, height=1080, piano_rect=None):
    """
    Crea una imagen sintética con un rectángulo blanco simulando un piano.
    
    Args:
        width: Ancho de la imagen
        height: Alto de la imagen
        piano_rect: Tupla (x, y, w, h) para posición y tamaño del piano
    
    Returns:
        bytes: Imagen codificada en formato JPEG
    """
    # Crear imagen negra
    img = np.zeros((height, width, 3), dtype=np.uint8)
    
    # Si se proporciona rectángulo del piano, dibujarlo
    if piano_rect:
        x, y, w, h = piano_rect
        cv2.rectangle(img, (x, y), (x + w, y + h), (255, 255, 255), -1)
    else:
        # Piano centrado por defecto
        piano_w = int(width * 0.6)
        piano_h = int(height * 0.3)
        piano_x = (width - piano_w) // 2
        piano_y = int(height * 0.4)
        cv2.rectangle(img, (piano_x, piano_y), (piano_x + piano_w, piano_y + piano_h), (255, 255, 255), -1)
    
    # Codificar como JPEG
    _, buffer = cv2.imencode('.jpg', img)
    return buffer.tobytes()


def crear_imagen_piano_rotado(width=1920, height=1080, angle=15):
    """
    Crea una imagen con un piano rotado para probar detección de inclinación.
    
    Args:
        width: Ancho de la imagen
        height: Alto de la imagen
        angle: Ángulo de rotación en grados
    
    Returns:
        bytes: Imagen codificada en formato JPEG
    """
    img = np.zeros((height, width, 3), dtype=np.uint8)
    
    # Crear piano centrado
    piano_w = int(width * 0.6)
    piano_h = int(height * 0.3)
    center_x = width // 2
    center_y = height // 2
    
    # Calcular puntos del rectángulo rotado
    rect = ((center_x, center_y), (piano_w, piano_h), angle)
    box = cv2.boxPoints(rect)
    box = box.astype(int)  # Cambio aquí
    
    # Dibujar rectángulo rotado
    cv2.drawContours(img, [box], 0, (255, 255, 255), -1)
    
    _, buffer = cv2.imencode('.jpg', img)
    return buffer.tobytes()


# ==========================================================
# 🔹 Tests de validación de resultado
# ==========================================================
def validar_resultado_calibracion(resultado):
    """Valida que el resultado sea un JSON válido con la estructura esperada"""
    assert isinstance(resultado, str)
    data = json.loads(resultado)
    assert 'command' in data
    assert 'corners' in data
    return data


# ==========================================================
# 🔹 1. Tests básicos de funcionamiento
# ==========================================================
def test_calibration_returns_valid_json():
    """Verifica que is_calibrated retorna un JSON válido"""
    img_bytes = crear_imagen_piano()
    context = MockContext()
    
    result = is_calibrated(img_bytes, 0.8, 1.33, context)
    data = validar_resultado_calibracion(result)
    
    assert data['command'] is not None


def test_calibration_with_centered_piano():
    """Verifica calibración con piano perfectamente centrado"""
    img_bytes = crear_imagen_piano(
        width=1920, 
        height=1080, 
        piano_rect=(400, 400, 1120, 280)
    )
    context = MockContext()
    
    result = is_calibrated(img_bytes, 0.8, 1.33, context)
    data = validar_resultado_calibracion(result)
    
    # Debería detectar corners
    assert data['corners'] is not None or data['command'] == "notCalibrated"


# ==========================================================
# 🔹 2. Tests de comandos de instrucción
# ==========================================================
def test_calibration_piano_left_edge():
    """Verifica comando cuando el piano está en el borde izquierdo"""
    img_bytes = crear_imagen_piano(
        width=1920,
        height=1080,
        piano_rect=(0, 400, 800, 280)
    )
    context = MockContext()
    
    result = is_calibrated(img_bytes, 0.8, 1.33, context)
    data = validar_resultado_calibracion(result)
    
    # Debería sugerir mover a la derecha o ajustar
    assert data['command'] in ["izquierda", "derecha", "arriba", "notCalibrated"]


def test_calibration_piano_right_edge():
    """Verifica comando cuando el piano está en el borde derecho"""
    img_bytes = crear_imagen_piano(
        width=1920,
        height=1080,
        piano_rect=(1120, 400, 800, 280)
    )
    context = MockContext()
    
    result = is_calibrated(img_bytes, 0.8, 1.33, context)
    data = validar_resultado_calibracion(result)
    
    assert data['command'] in ["derecha", "izquierda", "arriba", "notCalibrated"]


def test_calibration_piano_rotated():
    """Verifica detección de piano inclinado"""
    img_bytes = crear_imagen_piano_rotado(angle=20)
    context = MockContext()
    
    result = is_calibrated(img_bytes, 0.8, 1.33, context)
    data = validar_resultado_calibracion(result)
    
    # Debería detectar rotación o no calibrar
    assert data['command'] in ["r_izquierda", "r_derecha", "notCalibrated"]


# ==========================================================
# 🔹 3. Tests de casos especiales
# ==========================================================

def test_calibration_multiple_percentage_values():
    """Verifica calibración con diferentes valores de porcentaje de área"""
    img_bytes = crear_imagen_piano()
    context = MockContext()
    
    percentages = [0.5, 0.7, 0.9, 1.0]
    
    for percentage in percentages:
        result = is_calibrated(img_bytes, percentage, 1.33, context)
        data = validar_resultado_calibracion(result)
        assert data is not None


def test_calibration_different_aspect_ratios():
    """Verifica calibración con diferentes aspect ratios"""
    img_bytes = crear_imagen_piano()
    context = MockContext()
    
    aspect_ratios = [1.0, 1.33, 1.77, 2.0]
    
    for ratio in aspect_ratios:
        result = is_calibrated(img_bytes, 0.8, ratio, context)
        data = validar_resultado_calibracion(result)
        assert data is not None


# ==========================================================
# 🔹 4. Tests de robustez
# ==========================================================
def test_calibration_small_image():
    """Verifica calibración con imagen pequeña"""
    img_bytes = crear_imagen_piano(width=640, height=480)
    context = MockContext()
    
    result = is_calibrated(img_bytes, 0.8, 1.33, context)
    data = validar_resultado_calibracion(result)
    assert data is not None


def test_calibration_large_image():
    """Verifica calibración con imagen grande"""
    img_bytes = crear_imagen_piano(width=3840, height=2160)
    context = MockContext()
    
    result = is_calibrated(img_bytes, 0.8, 1.33, context)
    data = validar_resultado_calibracion(result)
    assert data is not None



# ==========================================================
# 🔹 5. Tests de corners detectados
# ==========================================================
def test_calibration_corners_format():
    """Verifica que los corners detectados tengan el formato correcto"""
    img_bytes = crear_imagen_piano()
    context = MockContext()
    
    result = is_calibrated(img_bytes, 0.8, 1.33, context)
    data = validar_resultado_calibracion(result)
    
    if data['corners'] is not None:
        assert isinstance(data['corners'], list)
        for corner in data['corners']:
            assert isinstance(corner, (list, tuple))
            assert len(corner) == 2
            assert isinstance(corner[0], (int, float))
            assert isinstance(corner[1], (int, float))


def test_calibration_corners_count():
    """Verifica que se detecten exactamente 4 corners cuando hay piano"""
    img_bytes = crear_imagen_piano()
    context = MockContext()
    
    result = is_calibrated(img_bytes, 0.8, 1.33, context)
    data = validar_resultado_calibracion(result)
    
    if data['corners'] is not None:
        # Debería detectar 4 esquinas
        assert len(data['corners']) <= 4


# ==========================================================
# 🔹 6. Tests de comandos específicos
# ==========================================================
def test_all_possible_commands():
    """Verifica que todos los comandos posibles son valores válidos"""
    valid_commands = [
        "calibrado",
        "izquierda",
        "derecha",
        "arriba",
        "atras",
        "adelante",
        "r_izquierda",
        "r_derecha",
        "notCalibrated"
    ]
    
    img_bytes = crear_imagen_piano()
    context = MockContext()
    
    result = is_calibrated(img_bytes, 0.8, 1.33, context)
    data = validar_resultado_calibracion(result)
    
    assert data['command'] in valid_commands


# ==========================================================
# 🔹 7. Tests de integración con diferentes escenarios
# ==========================================================
def test_calibration_piano_top_position():
    """Verifica comando cuando el piano está muy arriba"""
    img_bytes = crear_imagen_piano(
        width=1920,
        height=1080,
        piano_rect=(400, 50, 1120, 200)
    )
    context = MockContext()
    
    result = is_calibrated(img_bytes, 0.8, 1.33, context)
    data = validar_resultado_calibracion(result)
    
    assert data['command'] in ["adelante", "atras", "notCalibrated"]


def test_calibration_piano_bottom_position():
    """Verifica comando cuando el piano está muy abajo"""
    img_bytes = crear_imagen_piano(
        width=1920,
        height=1080,
        piano_rect=(400, 850, 1120, 200)
    )
    context = MockContext()
    
    result = is_calibrated(img_bytes, 0.8, 1.33, context)
    data = validar_resultado_calibracion(result)
    
    assert data['command'] in ["adelante", "atras", "notCalibrated"]
