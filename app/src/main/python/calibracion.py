import cv2
import numpy as np
import json

RESIZE_WIDTH = 450
# Sirve para delimitar dos bordes a cada lado de la imagen, 10 pixeles izquierda, Ancho - 10 en la der
PIANO_AREA_XSECTION_OFFSET = 10
# Define el porcentaje de la imagen original que corresponde al piano para su recorte
PIANO_AREA_YSECTION_PERCENTAGE = 0.4

def is_calibrated(byte_array_image):

    def is_piano_inside_area(corners):
        for corner in corners:
            x, y = corner.ravel()
            if not (PIANO_AREA_XSECTION_OFFSET <= x <= (RESIZE_WIDTH - PIANO_AREA_XSECTION_OFFSET)):
                return False
        return True

    def is_piano_straight(corners):
        corner_xy_tuples = []
        for corner in corners:
            x, y = corner.ravel()
            corner_xy_tuples.append((x,y))

        corner_xy_tuples.sort(key = lambda point: point[0])

        L_third_point = (corner_xy_tuples[1][0], corner_xy_tuples[0][1])
        R_third_point = (corner_xy_tuples[-1][0], corner_xy_tuples[-2][1])
        corner_xy_tuples.insert(2, L_third_point)
        corner_xy_tuples.append(R_third_point)

        BA = np.array(corner_xy_tuples[1]) - np.array(corner_xy_tuples[0])
        BC = np.array(corner_xy_tuples[2]) - np.array(corner_xy_tuples[0])

        cos_theta = np.dot(BA, BC) / (np.linalg.norm(BA) * np.linalg.norm(BC))
        cos_theta = np.clip(cos_theta, -1, 1)

        left_side_piano_angle = np.degrees(np.arccos(cos_theta))
        print (f"L ANGLE: {np.degrees(np.arccos(cos_theta))}")

        BA = np.array(corner_xy_tuples[4]) - np.array(corner_xy_tuples[3])
        BC = np.array(corner_xy_tuples[5]) - np.array(corner_xy_tuples[3])

        cos_theta = np.dot(BA, BC) / (np.linalg.norm(BA) * np.linalg.norm(BC))
        cos_theta = np.clip(cos_theta, -1, 1)

        right_side_piano_angle = np.degrees(np.arccos(cos_theta))
        print (f"R ANGLE: {np.degrees(np.arccos(cos_theta))}")

        # np.isnan(left_side_piano_angle) when is 90 degrees returns nan
        if ((np.isnan(left_side_piano_angle) or 85 <= left_side_piano_angle < 90) and (np.isnan(right_side_piano_angle) or 85 <= right_side_piano_angle < 90)):
            return True
        else:
            return False


    nparr = np.frombuffer(byte_array_image, np.uint8)

    # cv2.imdecode leer la imagen del Numpy array
    raw_frame = cv2.imdecode(nparr, cv2.IMREAD_GRAYSCALE)
    # Get image dimensions
    original_height, original_width = raw_frame.shape
    # Calculate the number of pixels to keep
    keep_height = int(original_height * PIANO_AREA_YSECTION_PERCENTAGE)
    # Crop from top (remove 30% from top)
    cropped_frame = raw_frame[:keep_height, :]


    original_height, original_width = cropped_frame.shape[:2]
    aspect_ratio = RESIZE_WIDTH / original_width
    new_height = int(original_height * aspect_ratio)

    img = cv2.resize(cropped_frame, (RESIZE_WIDTH, new_height))

    # Aplicar Gaussian Blur para reducir ruido de la imagen
    blur = cv2.bilateralFilter(img,9,50,100)

    # Modifica contraste y brillo
    alpha = 1.3  # Increase contrast (make whites whiter, blacks blacker)
    beta = 1.2    # Increase brightness (shift all pixels up)
    bright_contrast_image = cv2.convertScaleAbs(blur, alpha=alpha, beta=beta)

    # Define mejor el contraste, permite identificar mejor los bordes
    equalized = cv2.equalizeHist(bright_contrast_image)

    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8,8))
    clahe_img = clahe.apply(bright_contrast_image)
    # Threshold permite indicar un colo minimo de pixel que se convertira a blanco, el reston negro
    _, thresh = cv2.threshold(clahe_img, 176, 255, cv2.THRESH_TOZERO)

    # Aplicar algoritmo de deteccion de bordes canny
    edges = cv2.Canny(thresh, threshold1=140, threshold2=350)

    # Dilatacion para hacer mas gruesos los bordes de Canny porque son muy delgados
    kernel = np.ones((3,3), np.uint8)
    dilated_edges = cv2.dilate(edges, kernel, iterations=1)

    # Hallamos contorno de la imagen, notese que no es lo mismo que la deteccion de bordes
    contours, hierarchy = cv2.findContours(dilated_edges, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    c = max(contours, key=cv2.contourArea)

    # Con el contorno aproximado de la imagen, aplicamos un cascaron que ignore las irregularidades
    # y obtener de esta manera un rectangulo
    # Calcular Convex Hull
    hull = cv2.convexHull(c)

    # Se crea un lienzo negro
    drawing = np.zeros((thresh.shape[0], thresh.shape[1]), np.uint8)

    # Definimos colores para dibujar sobre el lienzo
    color_hull = (255, 255, 255) # blanco

    # Se dibuja el cascaron (Hull)
    cv2.drawContours(drawing, [hull], -1, color_hull, 1, 8)

    # Se aplica el algoritmo de deteccion de esquinas Shi-Tomasi
    corners_st = cv2.goodFeaturesToTrack(
        drawing,
        maxCorners=4,
        qualityLevel=0.01,
        minDistance=30,
        useHarrisDetector=False
    )

    if corners_st is not None:
        scale_x = original_width / RESIZE_WIDTH
        scale_y = original_height / new_height

        resized_dimensions_corners = []
        for corner in corners_st:
            x, y = corner.ravel()
            resized_dimensions_corners.append((x, y))

        # FIX: Use lists, not tuples
        original_dimensions_corners = [[int(x * scale_x), int(y * scale_y)] for (x, y) in resized_dimensions_corners]

        if is_piano_straight(corners_st) and is_piano_inside_area(corners_st):
            return json.dumps({
                'success': True,
                'corners': original_dimensions_corners
            })
        else:
            return json.dumps({
                'success': False,
                'corners': original_dimensions_corners
            })
    else:
        return json.dumps({
            'success': False,
            'corners': None
        })




