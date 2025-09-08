import cv2
import numpy as np
import json
import os

RESIZE_WIDTH = 608
# Sirve para delimitar dos bordes a cada lado de la imagen, 10 pixeles izquierda, Ancho - 10 en la der
PIANO_AREA_XSECTION_OFFSET = int(RESIZE_WIDTH * 0.03)
PIANO_AREA_YSECTION_OFFSET = int(RESIZE_WIDTH * 0.025)
RIGHT_SIDE_LIMIT = RESIZE_WIDTH - PIANO_AREA_YSECTION_OFFSET
# Distancia aproximada que realiza el usuario cada vez que hace una correccion
MOVEMENT_CORRECTION_DISTANCE = int(RESIZE_WIDTH * 0.025)


def is_calibrated(byte_array_image, piano_area_percentage, heightToWidthRatio, context):

    def instruction_command(corners, image_height):
        # Rango de tolerancia que determina si el piano esta recto o no.
        straight_inferior_corners_tolerance = int(image_height * 0.2)
        bottom_side_limit = image_height - PIANO_AREA_YSECTION_OFFSET

        print(f"BOTTOM SIDE LIMIT: {bottom_side_limit}")

        corner_xy_tuples = []
        for corner in corners:
            x, y = corner.ravel()
            corner_xy_tuples.append((x,y))

        # Order by x coordinates
        corner_xy_tuples.sort(key = lambda point: point[0])
        left_side_upper_corner = ()
        left_side_lower_corner = ()
        right_side_upper_corner = ()
        right_side_lower_corner = ()
        if corner_xy_tuples[0][1] < corner_xy_tuples[1][1]:
            left_side_upper_corner = corner_xy_tuples[0]
            left_side_lower_corner = corner_xy_tuples[1]
        else:
            left_side_upper_corner = corner_xy_tuples[1]
            left_side_lower_corner = corner_xy_tuples[0]
        if corner_xy_tuples[2][1] < corner_xy_tuples[3][1]:
            right_side_upper_corner = corner_xy_tuples[2]
            right_side_lower_corner = corner_xy_tuples[3]
        else:
            right_side_upper_corner = corner_xy_tuples[3]
            right_side_lower_corner = corner_xy_tuples[2]

        # Revision de rectitud del piano (Las dos esquinas inferiores deben estar a +- la misma altura Y)
        print(f"TOLERANCIA: {straight_inferior_corners_tolerance}")
        print(f"DIFFY: {left_side_lower_corner[1] - right_side_lower_corner[1]}")
        print(f"LU: {left_side_upper_corner}")
        print(f"LL: {left_side_lower_corner}")
        print(f"RU: {right_side_upper_corner}")
        print(f"RL: {right_side_lower_corner}")





        if left_side_upper_corner[0] <= PIANO_AREA_XSECTION_OFFSET or left_side_lower_corner[0] <= PIANO_AREA_XSECTION_OFFSET:
            # Si al mover hacia la izquierda, los demas puntos se salen, subir dispositivo
            if right_side_upper_corner[0] + MOVEMENT_CORRECTION_DISTANCE >= RIGHT_SIDE_LIMIT or right_side_lower_corner[0] + MOVEMENT_CORRECTION_DISTANCE >= RIGHT_SIDE_LIMIT:
                return "arriba"
            else:
                return "izquierda"
        if right_side_upper_corner[0] >= RIGHT_SIDE_LIMIT or right_side_lower_corner[0] >= RIGHT_SIDE_LIMIT:
            if left_side_upper_corner[0] - MOVEMENT_CORRECTION_DISTANCE <= PIANO_AREA_XSECTION_OFFSET or left_side_lower_corner[0] - MOVEMENT_CORRECTION_DISTANCE <= PIANO_AREA_XSECTION_OFFSET:
                return "arriba"
            else:
                return "derecha"

        if np.absolute(left_side_lower_corner[1] - right_side_lower_corner[1]) > straight_inferior_corners_tolerance:
            # Si esquina inferior izq esta mas abajo que la derecha
            if left_side_lower_corner[1] > right_side_lower_corner[1]:
                return "r_izquierda"
            else:
                return "r_derecha"

        if left_side_upper_corner[1] <= PIANO_AREA_YSECTION_OFFSET or right_side_upper_corner[1] <= PIANO_AREA_YSECTION_OFFSET:
            return "adelante"
        if left_side_lower_corner[1] >= bottom_side_limit or right_side_lower_corner[1] >= bottom_side_limit:
            return "atras"


        return "calibrado"

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
        # print (f"L ANGLE: {np.degrees(np.arccos(cos_theta))}")

        BA = np.array(corner_xy_tuples[4]) - np.array(corner_xy_tuples[3])
        BC = np.array(corner_xy_tuples[5]) - np.array(corner_xy_tuples[3])

        cos_theta = np.dot(BA, BC) / (np.linalg.norm(BA) * np.linalg.norm(BC))
        cos_theta = np.clip(cos_theta, -1, 1)

        right_side_piano_angle = np.degrees(np.arccos(cos_theta))
        # print (f"R ANGLE: {np.degrees(np.arccos(cos_theta))}")

        # np.isnan(left_side_piano_angle) when is 90 degrees returns nan
        if ((np.isnan(left_side_piano_angle) or 85 <= left_side_piano_angle < 90) and (np.isnan(right_side_piano_angle) or 85 <= right_side_piano_angle < 90)):
            return True
        else:
            return False

    nparr = np.frombuffer(byte_array_image, np.uint8)

    # cv2.imdecode leer la imagen del Numpy array
    raw_frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)



    # files_dir = str(context.getFilesDir())
    #
    # # Define image file path
    # img_path = os.path.join(files_dir, "my_image.jpg")
    #
    # # Save image to the path
    # cv2.imwrite(img_path, raw_frame)
    #
    # return

# Get image dimensions
    original_height, original_width = raw_frame.shape[:2]
    taken_image_height = int(original_width * heightToWidthRatio)

    # Calculate the number of pixels to keep
    keep_height = int(taken_image_height * piano_area_percentage)
    # Crop from top (remove piano_area_percentage% from top)
    cropped_frame = raw_frame[:keep_height, :]

    original_height, original_width = cropped_frame.shape[:2]
    print(f"ANXCHOOOOOO {original_width}")

    aspect_ratio = RESIZE_WIDTH / original_width
    new_height = int(original_height * aspect_ratio)

    img = cv2.resize(cropped_frame, (RESIZE_WIDTH, new_height))

    resized_height, resized_width = img.shape[:2]
    print(f"new height: {new_height}")
    print(f"reized height: {resized_height}")


    # if new_height < RESIZE_WIDTH:
    #     pad_height = RESIZE_WIDTH - new_height
    #     img = cv2.copyMakeBorder(
    #         img,
    #         top=0,
    #         bottom=pad_height,
    #         left=0,
    #         right=0,
    #         borderType=cv2.BORDER_CONSTANT,
    #         value=0  # Black padding
    #     )


    # Aplicar Gaussian Blur para reducir ruido de la imagen
    # blur = cv2.bilateralFilter(img,9,50,100)
    #
    # # Modifica contraste y brillo
    # alpha = 1.3  # Increase contrast (make whites whiter, blacks blacker)
    # beta = 1.2    # Increase brightness (shift all pixels up)
    # bright_contrast_image = cv2.convertScaleAbs(blur, alpha=alpha, beta=beta)
    #
    # # Equilibra el brillo de la foto
    # # clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8,8))
    # clahe = cv2.createCLAHE(clipLimit=1.0, tileGridSize=(4,4))
    # clahe_img = clahe.apply(bright_contrast_image)
    # Threshold permite indicar un colo minimo de pixel que se convertira a blanco, el reston negro
    _, thresh = cv2.threshold(img, 176, 255, cv2.THRESH_TOZERO)


    #
    # Aplicar algoritmo de deteccion de bordes canny
    edges = cv2.Canny(thresh, threshold1=140, threshold2=350)
    #
    # Dilatacion para hacer mas gruesos los bordes de Canny porque son muy delgados
    kernel = np.ones((3,3), np.uint8)
    dilated_edges = cv2.dilate(edges, kernel, iterations=1)
    #
    # Hallamos contorno de la imagen, notese que no es lo mismo que la deteccion de bordes
    contours, hierarchy = cv2.findContours(dilated_edges, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    c = max(contours, key=cv2.contourArea)
    #
    # Con el contorno aproximado de la imagen, aplicamos un cascaron que ignore las irregularidades
    # y obtener de esta manera un rectangulo
    # Calcular Convex Hull
    hull = cv2.convexHull(c)
    #
    # Se crea un lienzo negro
    drawing = np.zeros((new_height, RESIZE_WIDTH), np.uint8)
    #
    # Definimos colores para dibujar sobre el lienzo
    color_hull = (255, 255, 255) # blanco
    #
    # Se dibuja el cascaron (Hull)
    cv2.drawContours(drawing, [hull], -1, color_hull, 1, 8)
    # files_dir = str(context.getFilesDir())
    #
    # # Define image file path
    # img_path = os.path.join(files_dir, "my_image.jpg")
    #
    # # Save image to the path
    # cv2.imwrite(img_path, drawing)
    #
    # return



    # drawing = ypkd.get_piano_keys_from_yolo_model(context, img)


    # return
    # Se aplica el algoritmo de deteccion de esquinas Shi-Tomasi
    corners_st = cv2.goodFeaturesToTrack(
        drawing,
        maxCorners=4,
        qualityLevel=0.01,
        minDistance=30,
        useHarrisDetector=False
    )

    drawing_height, drawing_width = drawing.shape[:2]
    print(f"drawing height: {drawing_height}")

    cv2.line(drawing, (0, 129), (drawing.shape[1], 129), (255, 255, 255), 2)

    # files_dir = str(context.getFilesDir())
    #
    # # Define image file path
    # img_path = os.path.join(files_dir, "my_image.jpg")
    #
    # # Save image to the path
    # cv2.imwrite(img_path, drawing)
    #
    # return

    if corners_st is not None:


        compressed_dimensions_corners = []
        for corner in corners_st:
            x, y = corner.ravel()
            compressed_dimensions_corners.append((int(x), int(y)))

        voice_command = instruction_command(corners_st, new_height)
        if is_piano_straight(corners_st) and is_piano_inside_area(corners_st):
            return json.dumps({
                'command': voice_command,
                'corners': compressed_dimensions_corners
            })
        else:
            return json.dumps({
                'command': voice_command,
                'corners': compressed_dimensions_corners
            })
    else:
        return json.dumps({
            'command': "notCalibrated",
            'corners': None
        })




