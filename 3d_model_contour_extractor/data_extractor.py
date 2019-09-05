import sys
import os
import json
# import numpy as np

# import cv2
from pathlib import Path

import gc;

import model_to_vector

tempdir = ''
SQUARE_SIZE=8


def get_img_silhouette_points(model_path, model_features):
    model_to_vector.create_scene_n_save_screenshots(model_path, tempdir)
    for filename in os.listdir(tempdir):
        full_name = tempdir + "/" + filename
        curr_img = model_to_vector.get_silhouette_to_vector(full_name)/255  ## normalize
        if 'y' in filename:
            model_features['silhouette_y'] = curr_img.tolist()
            os.remove(full_name)
        elif 'x' in filename:
            model_features['silhouette_x'] = curr_img.tolist()
            os.remove(full_name)
        elif 'z' in filename:
            model_features['silhouette_z'] = curr_img.tolist()
            os.remove(full_name)

    return model_features


def get_img_fourier_points(model_path, model_features):
    model_to_vector.create_scene_n_save_screenshots(model_path, tempdir)
    for filename in os.listdir(tempdir):
        full_name = tempdir + "/" + filename
        curr_img_fourier = model_to_vector.get_fourier_transform_coefficients(full_name, SQUARE_SIZE).ravel()  ## normalize
        if 'y' in filename:
            model_features['fourier_y'] = curr_img_fourier.tolist()
            os.remove(full_name)
        elif 'x' in filename:
            model_features['fourier_x'] = curr_img_fourier.tolist()
            os.remove(full_name)
        elif 'z' in filename:
            model_features['fourier_z'] = curr_img_fourier.tolist()
            os.remove(full_name)

    return model_features


# def get_model_tags(model_path):
#     pass


def get_model_relative_path(model_path, data_folder, model_features):
    p = Path(model_path)
    model_features["src_path"] = model_path
    model_features["name"] = str(p.name)[:-4]

    return model_features


# def model_to_data_doc(model_path, temp_dir):
#     pass


def write_doc_txt(model_doc, target_path):
    model_file = target_path + "/" + model_doc["name"] + ".json"
    with open(model_file, 'w') as m_file:
        json.dump(model_doc, m_file)


def travers_models(data_folder, target_data_loc):
    for root, dirs, files in os.walk(data_folder):
        for file in files:
            if file.endswith('.off'):
                fullpath = os.path.join(root, file)
                model_doc = {}
                # model_doc = get_img_silhouette_points(fullpath, model_doc)
                model_doc = get_img_fourier_points(fullpath, model_doc)
                model_doc = get_model_relative_path(fullpath, data_folder, model_doc)
                write_doc_txt(model_doc, target_data_loc)
                gc.collect()


def main():
    global tempdir
    target_data_loc, data_folder = sys.argv[1:]
    tempdir = os.getcwd() + "/temp_dir"
    try:
        os.mkdir(tempdir)
    except FileExistsError:
        pass
    travers_models(data_folder, target_data_loc)
    os.remove(tempdir)


if __name__ == '__main__':
    main()